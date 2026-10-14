package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.GitHubArtifact
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.ProjectBuildEntity
import com.example.data.model.WorkflowJob
import com.example.data.remote.BuildStepProgress
import com.example.data.remote.GitHubApiClient
import com.example.data.remote.GitHubRepository
import com.example.util.SampleProjectGenerator
import com.example.util.ZipAnalysisResult
import com.example.util.ZipProjectParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AuthUiState(
  val token: String = "",
  val isVerifying: Boolean = false,
  val user: GitHubUser? = null,
  val repos: List<GitHubRepo> = emptyList(),
  val selectedRepo: String = "",
  val isCreatingRepo: Boolean = false,
  val isInstallingWorkflow: Boolean = false,
  val infoMessage: String? = null,
  val errorMessage: String? = null
)

data class BuildUiState(
  val projectAnalysis: ZipAnalysisResult? = null,
  val projectName: String = "",
  val buildVariant: String = "debug", // "debug" or "release"
  val isAnalyzing: Boolean = false,
  val isBuilding: Boolean = false,
  val currentProgress: BuildStepProgress? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null
)

data class LogsDialogState(
  val isVisible: Boolean = false,
  val buildEntity: ProjectBuildEntity? = null,
  val jobs: List<WorkflowJob> = emptyList(),
  val artifacts: List<GitHubArtifact> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

class BuilderViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val buildDao = database.projectBuildDao()
  private val prefs = PreferencesManager(application)

  private val _authUiState = MutableStateFlow(AuthUiState())
  val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

  private val _buildUiState = MutableStateFlow(BuildUiState())
  val buildUiState: StateFlow<BuildUiState> = _buildUiState.asStateFlow()

  private val _logsDialogState = MutableStateFlow(LogsDialogState())
  val logsDialogState: StateFlow<LogsDialogState> = _logsDialogState.asStateFlow()

  val allBuilds: StateFlow<List<ProjectBuildEntity>> = buildDao.getAllBuilds()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private var gitHubRepository: GitHubRepository? = null
  private var pollingJob: Job? = null

  init {
    val savedToken = prefs.githubToken.value
    val savedRepo = prefs.defaultRepo.value
    val savedVariant = prefs.defaultVariant.value

    _authUiState.value = _authUiState.value.copy(
      token = savedToken,
      selectedRepo = savedRepo
    )
    _buildUiState.value = _buildUiState.value.copy(
      buildVariant = savedVariant
    )

    if (savedToken.isNotBlank()) {
      saveAndVerifyToken(savedToken)
    }

    startPollingActiveBuilds()
  }

  private fun initializeApi(token: String) {
    val service = GitHubApiClient.createService(token)
    gitHubRepository = GitHubRepository(service)
  }

  fun updateToken(token: String) {
    _authUiState.value = _authUiState.value.copy(token = token, errorMessage = null)
  }

  fun saveAndVerifyToken(token: String) {
    viewModelScope.launch {
      val cleanToken = token.trim()
      if (cleanToken.isBlank()) {
        _authUiState.value = _authUiState.value.copy(errorMessage = "Insira um token válido do GitHub.")
        return@launch
      }

      _authUiState.value = _authUiState.value.copy(isVerifying = true, errorMessage = null, infoMessage = null)
      initializeApi(cleanToken)

      val repo = gitHubRepository ?: return@launch
      val userResult = repo.validateToken()

      if (userResult.isSuccess) {
        val user = userResult.getOrNull()!!
        prefs.saveGitHubToken(cleanToken)
        prefs.saveGitHubUsername(user.login)

        _authUiState.value = _authUiState.value.copy(
          user = user,
          isVerifying = false,
          infoMessage = "Autenticado com sucesso como @${user.login}!"
        )

        fetchUserRepos()
      } else {
        _authUiState.value = _authUiState.value.copy(
          isVerifying = false,
          errorMessage = userResult.exceptionOrNull()?.localizedMessage ?: "Falha ao validar o token do GitHub."
        )
      }
    }
  }

  fun fetchUserRepos() {
    viewModelScope.launch {
      val repo = gitHubRepository ?: return@launch
      val result = repo.getUserRepos()
      if (result.isSuccess) {
        val list = result.getOrNull().orEmpty()
        val currentSelected = _authUiState.value.selectedRepo
        val defaultChoice = if (currentSelected.isNotBlank() && list.any { it.fullName == currentSelected }) {
          currentSelected
        } else {
          list.firstOrNull()?.fullName ?: ""
        }

        _authUiState.value = _authUiState.value.copy(
          repos = list,
          selectedRepo = defaultChoice
        )
        if (defaultChoice.isNotBlank()) {
          prefs.saveDefaultRepo(defaultChoice)
        }
      }
    }
  }

  fun setSelectedRepo(repoFullName: String) {
    _authUiState.value = _authUiState.value.copy(selectedRepo = repoFullName)
    prefs.saveDefaultRepo(repoFullName)
  }

  fun setBuildVariant(variant: String) {
    _buildUiState.value = _buildUiState.value.copy(buildVariant = variant)
    prefs.saveDefaultVariant(variant)
  }

  fun createCompilerRepo(repoName: String) {
    viewModelScope.launch {
      val repo = gitHubRepository ?: run {
        _authUiState.value = _authUiState.value.copy(errorMessage = "Configure o token do GitHub primeiro.")
        return@launch
      }

      _authUiState.value = _authUiState.value.copy(isCreatingRepo = true, errorMessage = null)
      val result = repo.createCompilerRepository(repoName.trim())

      if (result.isSuccess) {
        val created = result.getOrNull()!!
        _authUiState.value = _authUiState.value.copy(
          isCreatingRepo = false,
          selectedRepo = created.fullName,
          infoMessage = "Repositório ${created.fullName} criado com sucesso e configurado com o Workflow CI!"
        )
        prefs.saveDefaultRepo(created.fullName)
        fetchUserRepos()
      } else {
        _authUiState.value = _authUiState.value.copy(
          isCreatingRepo = false,
          errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Erro ao criar repositório."
        )
      }
    }
  }

  fun installWorkflowToSelectedRepo() {
    viewModelScope.launch {
      val repoFullName = _authUiState.value.selectedRepo
      if (repoFullName.isBlank()) {
        _authUiState.value = _authUiState.value.copy(errorMessage = "Selecione um repositório primeiro.")
        return@launch
      }

      val repo = gitHubRepository ?: return@launch
      _authUiState.value = _authUiState.value.copy(isInstallingWorkflow = true, errorMessage = null)

      val result = repo.installWorkflow(repoFullName)
      if (result.isSuccess) {
        _authUiState.value = _authUiState.value.copy(
          isInstallingWorkflow = false,
          infoMessage = "Workflow CI instalado/atualizado em $repoFullName (.github/workflows/android-compile.yml)!"
        )
      } else {
        _authUiState.value = _authUiState.value.copy(
          isInstallingWorkflow = false,
          errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Erro ao instalar workflow."
        )
      }
    }
  }

  fun analyzeZipUri(uri: Uri) {
    viewModelScope.launch {
      _buildUiState.value = _buildUiState.value.copy(isAnalyzing = true, errorMessage = null)
      val context = getApplication<Application>()
      val analysis = ZipProjectParser.analyzeAndExtractZip(context, uri)

      _buildUiState.value = _buildUiState.value.copy(
        isAnalyzing = false,
        projectAnalysis = analysis,
        projectName = analysis.detectedProjectName,
        errorMessage = analysis.error
      )
    }
  }

  fun loadSampleProject() {
    _buildUiState.value = _buildUiState.value.copy(isAnalyzing = true, errorMessage = null)
    val analysis = SampleProjectGenerator.generateSampleProject()
    _buildUiState.value = _buildUiState.value.copy(
      isAnalyzing = false,
      projectAnalysis = analysis,
      projectName = analysis.detectedProjectName,
      statusMessage = "Projeto de exemplo gerado com sucesso! Pronto para compilar."
    )
  }

  fun updateProjectName(name: String) {
    _buildUiState.value = _buildUiState.value.copy(projectName = name)
  }

  fun clearSelectedProject() {
    _buildUiState.value = _buildUiState.value.copy(
      projectAnalysis = null,
      projectName = "",
      statusMessage = null,
      errorMessage = null,
      currentProgress = null
    )
  }

  fun startBuild(onSuccessNavigateToHistory: () -> Unit) {
    val analysis = _buildUiState.value.projectAnalysis ?: run {
      _buildUiState.value = _buildUiState.value.copy(errorMessage = "Nenhum projeto ZIP selecionado.")
      return
    }

    val repoFullName = _authUiState.value.selectedRepo
    if (repoFullName.isBlank()) {
      _buildUiState.value = _buildUiState.value.copy(errorMessage = "Selecione o repositório nas configurações.")
      return
    }

    val gitRepo = gitHubRepository ?: run {
      _buildUiState.value = _buildUiState.value.copy(errorMessage = "Configure o token do GitHub primeiro.")
      return
    }

    viewModelScope.launch {
      _buildUiState.value = _buildUiState.value.copy(
        isBuilding = true,
        errorMessage = null,
        statusMessage = "Preparando arquivos para envio ao GitHub..."
      )

      val projectName = _buildUiState.value.projectName.ifBlank { "App" }
      val variant = _buildUiState.value.buildVariant
      val gradleTask = if (variant == "release") "assembleRelease" else "assembleDebug"

      // 1. Create DB record in "uploading" state
      val buildEntity = ProjectBuildEntity(
        projectName = projectName,
        repoFullName = repoFullName,
        branchName = "preparando...",
        buildVariant = variant,
        gradleTask = gradleTask,
        status = "uploading",
        totalFiles = analysis.totalFiles,
        zipSizeBytes = analysis.totalSizeBytes
      )
      val insertedId = buildDao.insertBuild(buildEntity)

      val result = gitRepo.pushProjectAndStartBuild(
        repoFullName = repoFullName,
        projectName = projectName,
        files = analysis.files,
        buildVariant = variant
      ) { progress ->
        _buildUiState.value = _buildUiState.value.copy(currentProgress = progress)
      }

      if (result.isSuccess) {
        val (branchName, runId, commitSha) = result.getOrNull()!!
        val updated = buildDao.getBuildById(insertedId)?.copy(
          branchName = branchName,
          runId = runId,
          commitSha = commitSha,
          status = if (runId != null) "in_progress" else "queued"
        )
        if (updated != null) {
          buildDao.updateBuild(updated)
        }

        _buildUiState.value = _buildUiState.value.copy(
          isBuilding = false,
          currentProgress = null,
          statusMessage = "Código enviado com sucesso para o branch $branchName! Compilação em andamento."
        )

        onSuccessNavigateToHistory()
      } else {
        val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Erro desconhecido ao enviar código."
        val failed = buildDao.getBuildById(insertedId)?.copy(
          status = "failed",
          conclusion = "failure",
          errorMessage = errorMsg,
          completedAt = System.currentTimeMillis()
        )
        if (failed != null) {
          buildDao.updateBuild(failed)
        }

        _buildUiState.value = _buildUiState.value.copy(
          isBuilding = false,
          currentProgress = null,
          errorMessage = errorMsg
        )
      }
    }
  }

  private fun startPollingActiveBuilds() {
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
      while (isActive) {
        try {
          val activeList = allBuilds.value.filter { it.status in listOf("uploading", "queued", "in_progress") }
          val repo = gitHubRepository

          if (repo != null && activeList.isNotEmpty()) {
            for (build in activeList) {
              if (build.runId != null) {
                // Poll GitHub run status
                val runResult = repo.getRunStatus(build.repoFullName, build.runId)
                if (runResult.isSuccess) {
                  val run = runResult.getOrNull()!!
                  val isFinished = run.status == "completed"

                  var artifactName: String? = null
                  var artifactUrl: String? = null

                  if (isFinished && run.conclusion == "success") {
                    val artifactsResult = repo.getArtifacts(build.repoFullName, build.runId)
                    if (artifactsResult.isSuccess) {
                      val list = artifactsResult.getOrNull().orEmpty()
                      val apkArtifact = list.firstOrNull { it.name.contains("apk") } ?: list.firstOrNull()
                      if (apkArtifact != null) {
                        artifactName = apkArtifact.name
                        artifactUrl = apkArtifact.archiveDownloadUrl
                      }
                    }
                  }

                  val updated = build.copy(
                    status = run.status ?: build.status,
                    conclusion = run.conclusion,
                    runUrl = run.htmlUrl ?: build.runUrl,
                    artifactName = artifactName ?: build.artifactName,
                    artifactDownloadUrl = artifactUrl ?: build.artifactDownloadUrl,
                    completedAt = if (isFinished) System.currentTimeMillis() else null
                  )
                  buildDao.updateBuild(updated)
                }
              }
            }
          }
        } catch (_: Exception) {
          // Keep polling alive gracefully
        }
        delay(6000)
      }
    }
  }

  fun openLogsForBuild(build: ProjectBuildEntity) {
    _logsDialogState.value = LogsDialogState(
      isVisible = true,
      buildEntity = build,
      isLoading = true
    )

    viewModelScope.launch {
      val repo = gitHubRepository
      if (repo == null || build.runId == null) {
        _logsDialogState.value = _logsDialogState.value.copy(
          isLoading = false,
          error = "Informações de execução do GitHub não disponíveis para este build."
        )
        return@launch
      }

      val jobsResult = repo.getRunJobs(build.repoFullName, build.runId)
      val artifactsResult = repo.getArtifacts(build.repoFullName, build.runId)

      _logsDialogState.value = _logsDialogState.value.copy(
        isLoading = false,
        jobs = jobsResult.getOrNull().orEmpty(),
        artifacts = artifactsResult.getOrNull().orEmpty(),
        error = jobsResult.exceptionOrNull()?.localizedMessage
      )
    }
  }

  fun dismissLogsDialog() {
    _logsDialogState.value = LogsDialogState(isVisible = false)
  }

  fun deleteBuild(id: Long) {
    viewModelScope.launch {
      buildDao.deleteById(id)
    }
  }

  fun clearAllBuilds() {
    viewModelScope.launch {
      buildDao.clearAll()
    }
  }

  override fun onCleared() {
    super.onCleared()
    pollingJob?.cancel()
  }
}
