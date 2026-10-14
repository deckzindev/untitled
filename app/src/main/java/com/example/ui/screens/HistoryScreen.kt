package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectBuildEntity
import com.example.ui.components.BuildStatusBadge
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AccentGreenBright
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BuilderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  viewModel: BuilderViewModel,
  onNavigateToBuild: () -> Unit
) {
  val builds by viewModel.allBuilds.collectAsState()
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Compilações Realizadas",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = TextPrimary
        )
        Text(
          text = "${builds.size} projetos compilados",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }

      if (builds.isNotEmpty()) {
        IconButton(
          onClick = { viewModel.clearAllBuilds() },
          modifier = Modifier.testTag("clear_history_button")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Limpar histórico", tint = TextMuted)
        }
      }
    }

    if (builds.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(32.dp))
          }
          Text(
            text = "Nenhuma compilação realizada ainda",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextPrimary
          )
          Text(
            text = "Envie um projeto ZIP para compilar seu primeiro APK.",
            fontSize = 13.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = onNavigateToBuild,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("goto_build_screen_button")
          ) {
            Text("Iniciar Nova Compilação", color = DarkSurface, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(builds, key = { it.id }) { build ->
          BuildCardItem(
            build = build,
            onViewLogs = { viewModel.openLogsForBuild(build) },
            onDelete = { viewModel.deleteBuild(build.id) }
          )
        }
        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
private fun BuildCardItem(
  build: ProjectBuildEntity,
  onViewLogs: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  val formattedDate = dateFormat.format(Date(build.createdAt))

  Card(
    colors = CardDefaults.cardColors(containerColor = DarkSurface),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top row: Project name and status badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = build.projectName,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = TextPrimary
          )
          Text(
            text = "$formattedDate • ${build.buildVariant.uppercase()}",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        BuildStatusBadge(status = build.status, conclusion = build.conclusion)
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Metadata card
      Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Repositório: ${build.repoFullName}",
            fontSize = 11.sp,
            color = TextSecondary
          )
          Text(
            text = "Branch: ${build.branchName}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = AccentBlueBright
          )
          if (build.runId != null) {
            Text(
              text = "Execução no Actions: #${build.runId}",
              fontSize = 11.sp,
              color = TextMuted
            )
          }
          if (build.errorMessage != null) {
            Text(
              text = "Erro: ${build.errorMessage}",
              fontSize = 11.sp,
              color = AccentRed
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Actions row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // If success and APK artifact available
        if (build.artifactDownloadUrl != null) {
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(build.artifactDownloadUrl))
              context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenBright),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("download_apk_${build.id}")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkSurface)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Baixar APK", color = DarkSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        OutlinedButton(
          onClick = onViewLogs,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("view_logs_${build.id}")
        ) {
          Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Passos & Logs", fontSize = 12.sp)
        }

        if (build.runUrl != null) {
          IconButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(build.runUrl))
              context.startActivity(intent)
            },
            modifier = Modifier.size(36.dp).testTag("open_github_link_${build.id}")
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir no GitHub", tint = TextSecondary)
          }
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(36.dp).testTag("delete_build_${build.id}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TextMuted)
        }
      }
    }
  }
}
