package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WorkflowStep
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LogsDialogState

@Composable
fun LogsViewerDialog(
  state: LogsDialogState,
  onDismiss: () -> Unit
) {
  if (!state.isVisible) return

  val context = LocalContext.current
  val build = state.buildEntity

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 600.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
      color = DarkSurface
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Passos da Compilação",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = TextPrimary
            )
            Text(
              text = build?.projectName ?: "Projeto",
              fontSize = 13.sp,
              color = TextSecondary
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_logs_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (build != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Branch: ${build.branchName}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = AccentBlueBright
              )
              if (build.runId != null) {
                Text(
                  text = "Run ID: #${build.runId} • Variante: ${build.buildVariant}",
                  fontSize = 12.sp,
                  color = TextMuted
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (state.isLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = AccentBlueBright)
              Spacer(modifier = Modifier.height(12.dp))
              Text("Carregando passos do GitHub Actions...", color = TextSecondary, fontSize = 13.sp)
            }
          }
        } else if (state.error != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Text(text = state.error, color = AccentRed, fontSize = 13.sp)
          }
        } else {
          val allSteps = state.jobs.flatMap { it.steps ?: emptyList() }

          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (allSteps.isEmpty()) {
              item {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Aguardando o runner do GitHub iniciar as tarefas...",
                    color = TextMuted,
                    fontSize = 13.sp
                  )
                }
              }
            } else {
              items(allSteps) { step ->
                StepItemRow(step = step)
              }
            }

            // Artifacts section
            if (state.artifacts.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Artefatos Gerados",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp,
                  color = AccentGreenBright
                )
              }

              items(state.artifacts) { artifact ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = CodeBackground),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = artifact.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = TextPrimary
                      )
                      val sizeMb = String.format("%.2f MB", artifact.sizeInBytes / (1024f * 1024f))
                      Text(
                        text = "Tamanho: $sizeMb",
                        fontSize = 11.sp,
                        color = TextMuted
                      )
                    }

                    if (artifact.archiveDownloadUrl != null) {
                      Button(
                        onClick = {
                          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artifact.archiveDownloadUrl))
                          context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("download_artifact_button")
                      ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Baixar", fontSize = 12.sp, color = DarkSurface)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          if (build?.runUrl != null) {
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(build.runUrl))
                context.startActivity(intent)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("open_github_run_button")
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Abrir no GitHub", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("dismiss_logs_btn")
          ) {
            Text("Fechar", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun StepItemRow(step: WorkflowStep) {
  val (statusColor, statusIcon) = when {
    step.conclusion == "success" -> AccentGreenBright to Icons.Default.CheckCircle
    step.conclusion == "failure" -> AccentRed to Icons.Default.Error
    step.status == "in_progress" -> AccentBlueBright to Icons.Default.HourglassBottom
    else -> TextMuted to Icons.Default.HourglassBottom
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(DarkSurfaceVariant.copy(alpha = 0.5f))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = statusIcon,
      contentDescription = step.status,
      tint = statusColor,
      modifier = Modifier.size(16.dp)
    )

    Spacer(modifier = Modifier.width(10.dp))

    Text(
      text = step.name,
      fontSize = 13.sp,
      color = TextPrimary,
      modifier = Modifier.weight(1f)
    )

    val conclusionLabel = when (step.conclusion) {
      "success" -> "Concluído"
      "failure" -> "Falhou"
      else -> if (step.status == "in_progress") "Executando" else "Aguardando"
    }

    Text(
      text = conclusionLabel,
      fontSize = 11.sp,
      color = statusColor,
      fontWeight = FontWeight.Medium
    )
  }
}
