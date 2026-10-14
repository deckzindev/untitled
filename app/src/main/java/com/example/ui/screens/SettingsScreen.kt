package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WorkflowViewerDialog
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BuilderViewModel) {
  val authState by viewModel.authUiState.collectAsState()
  val context = LocalContext.current

  var tokenInput by remember(authState.token) { mutableStateOf(authState.token) }
  var isTokenVisible by remember { mutableStateOf(false) }
  var showNewRepoDialog by remember { mutableStateOf(false) }
  var newRepoNameInput by remember { mutableStateOf("android-compiler-runner") }
  var isWorkflowViewerOpen by remember { mutableStateOf(false) }
  var isRepoDropdownExpanded by remember { mutableStateOf(false) }

  WorkflowViewerDialog(
    isVisible = isWorkflowViewerOpen,
    onDismiss = { isWorkflowViewerOpen = false }
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Configuração do GitHub & Compilador",
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = TextPrimary
    )

    // 1. GitHub PAT Authentication Section
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = AccentBlueBright)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Token de Acesso Pessoal (PAT)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
          )
        }

        Text(
          text = "O token do GitHub é necessário para criar branches, enviar arquivos e disparar workflows do GitHub Actions.",
          fontSize = 12.sp,
          color = TextSecondary,
          lineHeight = 16.sp
        )

        OutlinedTextField(
          value = tokenInput,
          onValueChange = { tokenInput = it },
          label = { Text("Token (ghp_...)") },
          singleLine = true,
          visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(
              onClick = { isTokenVisible = !isTokenVisible },
              modifier = Modifier.testTag("toggle_token_visibility")
            ) {
              Icon(
                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Alternar visibilidade",
                tint = TextSecondary
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlueBright,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("github_token_input")
        )

        Button(
          onClick = { viewModel.saveAndVerifyToken(tokenInput) },
          enabled = tokenInput.isNotBlank() && !authState.isVerifying,
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlueBright),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verify_token_button")
        ) {
          if (authState.isVerifying) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkSurface, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Validando credenciais...", color = DarkSurface)
          } else {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkSurface)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Validar e Conectar Conta", color = DarkSurface, fontWeight = FontWeight.Bold)
          }
        }

        // Validated Profile Card
        if (authState.user != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(AccentGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreenBright)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = authState.user?.name ?: authState.user?.login ?: "",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = TextPrimary
                )
                Text(
                  text = "@${authState.user?.login} • ${authState.user?.publicRepos} repositórios",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }
          }
        }

        // Link to generate token
        OutlinedButton(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/tokens/new?scopes=repo,workflow&description=GitBuilder%20CI%20Mobile"))
            context.startActivity(intent)
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_token_github_button")
        ) {
          Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Como gerar Token no GitHub (com permissões repo & workflow)", fontSize = 11.sp)
        }
      }
    }

    // 2. Compiler Repository Section
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = AccentPurple)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Repositório do Compilador",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = TextPrimary
            )
          }

          if (authState.token.isNotBlank()) {
            IconButton(
              onClick = { viewModel.fetchUserRepos() },
              modifier = Modifier.testTag("refresh_repos_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Recarregar repositórios", tint = TextSecondary)
            }
          }
        }

        Text(
          text = "Escolha em qual repositório do seu GitHub os códigos serão enviados para compilação pelo GitHub Actions.",
          fontSize = 12.sp,
          color = TextSecondary
        )

        // Dropdown for selecting user's repo
        if (authState.repos.isNotEmpty()) {
          ExposedDropdownMenuBox(
            expanded = isRepoDropdownExpanded,
            onExpandedChange = { isRepoDropdownExpanded = !isRepoDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = authState.selectedRepo.ifBlank { "Selecione um repositório..." },
              onValueChange = {},
              readOnly = true,
              label = { Text("Repositório Selecionado") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRepoDropdownExpanded) },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("select_repo_dropdown")
            )

            ExposedDropdownMenu(
              expanded = isRepoDropdownExpanded,
              onDismissRequest = { isRepoDropdownExpanded = false },
              modifier = Modifier.background(DarkSurfaceVariant)
            ) {
              authState.repos.forEach { repo ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(repo.fullName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                      if (repo.private) {
                        Text("Privado", color = TextMuted, fontSize = 11.sp)
                      }
                    }
                  },
                  onClick = {
                    viewModel.setSelectedRepo(repo.fullName)
                    isRepoDropdownExpanded = false
                  }
                )
              }
            }
          }
        }

        // Option to create a dedicated compiler repo automatically
        Button(
          onClick = { showNewRepoDialog = !showNewRepoDialog },
          colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_compiler_repo_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Criar Novo Repositório de Compilação", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        AnimatedVisibility(visible = showNewRepoDialog) {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Nome do novo repositório:", fontSize = 12.sp, color = TextSecondary)
              OutlinedTextField(
                value = newRepoNameInput,
                onValueChange = { newRepoNameInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = AccentPurple,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("new_repo_name_input")
              )

              Button(
                onClick = {
                  viewModel.createCompilerRepo(newRepoNameInput)
                  showNewRepoDialog = false
                },
                enabled = newRepoNameInput.isNotBlank() && !authState.isCreatingRepo,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                if (authState.isCreatingRepo) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Criando no GitHub...")
                } else {
                  Text("Confirmar Criação no GitHub")
                }
              }
            }
          }
        }

        // Install Workflow button
        if (authState.selectedRepo.isNotBlank()) {
          OutlinedButton(
            onClick = { viewModel.installWorkflowToSelectedRepo() },
            enabled = !authState.isInstallingWorkflow,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("install_workflow_button")
          ) {
            if (authState.isInstallingWorkflow) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentBlueBright)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Instalando workflow no repositório...", fontSize = 12.sp)
            } else {
              Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Instalar/Atualizar Workflow CI no Repositório", fontSize = 12.sp)
            }
          }
        }

        // Workflow inspection button
        OutlinedButton(
          onClick = { isWorkflowViewerOpen = true },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("view_workflow_yaml_btn")
        ) {
          Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Inspecionar Script GitHub Actions (.yml)", fontSize = 12.sp)
        }
      }
    }

    // Feedback messages
    if (authState.errorMessage != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = authState.errorMessage ?: "", color = AccentRed, fontSize = 12.sp)
        }
      }
    }

    if (authState.infoMessage != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, AccentGreenBright.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreenBright)
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = authState.infoMessage ?: "", color = AccentGreenBright, fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
