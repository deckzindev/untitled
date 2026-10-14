package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.BuildStepProgress
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BuilderViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildScreen(
  viewModel: BuilderViewModel,
  onNavigateToSettings: () -> Unit,
  onNavigateToHistory: () -> Unit
) {
  val authState by viewModel.authUiState.collectAsState()
  val buildState by viewModel.buildUiState.collectAsState()

  val zipPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.analyzeZipUri(uri)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    // 1. Repository Info Banner
    if (authState.token.isBlank() || authState.selectedRepo.isBlank()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .clickable { onNavigateToSettings() }
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (authState.token.isBlank()) "GitHub PAT não configurado" else "Nenhum repositório selecionado",
              fontWeight = FontWeight.SemiBold,
              color = AccentAmber,
              fontSize = 14.sp
            )
            Text(
              text = "Toque para configurar sua conta e repositório de compilação.",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
          Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = AccentAmber)
        }
      }
    } else {
      Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(AccentGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AccentGreenBright, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Repositório do GitHub CI",
              fontSize = 11.sp,
              color = TextMuted
            )
            Text(
              text = authState.selectedRepo,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              fontFamily = FontFamily.Monospace,
              color = AccentBlueBright
            )
          }
          OutlinedButton(
            onClick = onNavigateToSettings,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("change_repo_button")
          ) {
            Text("Alterar", fontSize = 11.sp)
          }
        }
      }
    }

    // 2. Select Project Section
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "1. Código-Fonte do Projeto",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
          )

          if (buildState.projectAnalysis != null) {
            IconButton(
              onClick = { viewModel.clearSelectedProject() },
              modifier = Modifier.size(28.dp).testTag("clear_project_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Remover projeto", tint = TextMuted)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (buildState.isAnalyzing) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentBlueBright)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Analisando estrutura do projeto ZIP...", fontSize = 13.sp, color = TextSecondary)
          }
        } else if (buildState.projectAnalysis == null) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                zipPickerLauncher.launch(
                  arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream",
                    "*/*"
                  )
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_zip_button"),
              colors = ButtonDefaults.buttonColors(containerColor = AccentBlueBright),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.FolderZip, contentDescription = null, tint = DarkSurface)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Selecionar Arquivo ZIP do Projeto", color = DarkSurface, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { viewModel.loadSampleProject() },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("load_sample_project_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Ou testar com Projeto Exemplo (Hello World)", fontSize = 13.sp)
            }

            Text(
              text = "O ZIP deve conter a estrutura de código-fonte Android (arquivos build.gradle, settings.gradle, src, etc). Pastas como .gradle e build serão filtradas automaticamente.",
              fontSize = 11.sp,
              color = TextMuted,
              lineHeight = 15.sp
            )
          }
        } else {
          val analysis = buildState.projectAnalysis!!

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = buildState.projectName,
              onValueChange = { viewModel.updateProjectName(it) },
              label = { Text("Nome do Projeto") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("project_name_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlueBright,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              shape = RoundedCornerShape(8.dp)
            )

            // Info stats
            Card(
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val sizeKb = analysis.totalSizeBytes / 1024
                Text(
                  text = "📦 ${analysis.totalFiles} arquivos prontos para envio (${sizeKb} KB)",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  color = TextPrimary
                )

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  StatusChip(label = "settings.gradle", active = analysis.hasSettingsGradle)
                  StatusChip(label = "módulo /app", active = analysis.hasAppModule)
                  StatusChip(label = "gradlew", active = analysis.hasGradleWrapper)
                }
              }
            }
          }
        }
      }
    }

    // 3. Compilation Variant Options
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "2. Opções de Compilação",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Variante de Compilação:", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          FilterChip(
            selected = buildState.buildVariant == "debug",
            onClick = { viewModel.setBuildVariant("debug") },
            label = { Text("Debug (assembleDebug)") },
            leadingIcon = if (buildState.buildVariant == "debug") {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentBlueBright.copy(alpha = 0.2f),
              selectedLabelColor = AccentBlueBright
            ),
            modifier = Modifier.testTag("variant_debug_chip")
          )

          FilterChip(
            selected = buildState.buildVariant == "release",
            onClick = { viewModel.setBuildVariant("release") },
            label = { Text("Release (assembleRelease)") },
            leadingIcon = if (buildState.buildVariant == "release") {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentBlueBright.copy(alpha = 0.2f),
              selectedLabelColor = AccentBlueBright
            ),
            modifier = Modifier.testTag("variant_release_chip")
          )
        }
      }
    }

    // 4. Progress and Error Indicators
    AnimatedVisibility(visible = buildState.isBuilding) {
      Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, AccentBlueBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentBlueBright, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Enviando código e iniciando GitHub Actions...",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              color = AccentBlueBright
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentBlueBright)

          val step = buildState.currentProgress
          if (step is BuildStepProgress.UploadingFiles) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Arquivo ${step.current}/${step.total}: ${step.currentFile}",
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              color = TextMuted,
              maxLines = 1
            )
          } else if (step != null) {
            val stepText = when (step) {
              is BuildStepProgress.CreatingTree -> "Criando árvore Git no GitHub..."
              is BuildStepProgress.CreatingCommit -> "Criando commit de compilação..."
              is BuildStepProgress.UpdatingBranch -> "Atualizando branch no GitHub..."
              is BuildStepProgress.WaitingForRunner -> "Aguardando o runner do GitHub iniciar a execução..."
              is BuildStepProgress.WorkflowStarted -> "Execução do workflow iniciada (ID #${step.runId})"
              else -> "Processando..."
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stepText, fontSize = 12.sp, color = TextSecondary)
          }
        }
      }
    }

    if (buildState.errorMessage != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = buildState.errorMessage ?: "", color = AccentRed, fontSize = 12.sp)
        }
      }
    }

    if (buildState.statusMessage != null && !buildState.isBuilding) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, AccentGreenBright.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreenBright)
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = buildState.statusMessage ?: "", color = AccentGreenBright, fontSize = 12.sp)
        }
      }
    }

    // 5. Main Submit Button
    val canSubmit = buildState.projectAnalysis != null &&
        authState.token.isNotBlank() &&
        authState.selectedRepo.isNotBlank() &&
        !buildState.isBuilding

    Button(
      onClick = { viewModel.startBuild { onNavigateToHistory() } },
      enabled = canSubmit,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("start_build_button"),
      colors = ButtonDefaults.buttonColors(
        containerColor = AccentGreenBright,
        disabledContainerColor = DarkSurfaceVariant
      ),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (canSubmit) DarkSurface else TextMuted)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Enviar e Compilar APK no GitHub Actions",
        color = if (canSubmit) DarkSurface else TextMuted,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun StatusChip(label: String, active: Boolean) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (active) AccentGreen.copy(alpha = 0.2f) else DarkBorder)
      .padding(horizontal = 8.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(if (active) AccentGreenBright else TextMuted)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      color = if (active) AccentGreenBright else TextMuted,
      fontFamily = FontFamily.Monospace
    )
  }
}
