package com.example.data.workflow

object WorkflowTemplate {
  const val WORKFLOW_PATH = ".github/workflows/android-compile.yml"
  const val WORKFLOW_FILE_NAME = "android-compile.yml"

  val WORKFLOW_YAML = """
name: Android APK Compiler CI

on:
  push:
    branches:
      - 'main'
      - 'build/**'
  workflow_dispatch:
    inputs:
      build_variant:
        description: 'Variante de compilação (debug ou release)'
        required: true
        default: 'debug'
        type: choice
        options:
          - debug
          - release
      gradle_task:
        description: 'Tarefa do Gradle a executar'
        required: true
        default: 'assembleDebug'

jobs:
  build:
    name: Build Android APK
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: 📥 Clonar Código-Fonte
        uses: actions/checkout@v4

      - name: ☕ Configurar Java JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: 🐘 Configurar Ambiente Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: 🔑 Configurar Keystores e Variáveis de Ambiente
        run: |
          echo "=== 1. Gerando debug.keystore e keystores de assinatura ==="
          keytool -genkeypair -alias androiddebugkey -keypass android -keystore debug.keystore -storepass android -dname "CN=Android Debug,O=Android,C=US" -validity 10000 -keyalg RSA -keysize 2048 || true
          mkdir -p ~/.android
          cp debug.keystore ~/.android/debug.keystore || true
          mkdir -p app
          cp debug.keystore app/debug.keystore || true
          cp debug.keystore my-upload-key.jks || true

          echo "=== 2. Configurando .env para Secrets Gradle Plugin ==="
          if [ ! -f ".env" ] && [ -f ".env.example" ]; then
            cp .env.example .env
          elif [ ! -f ".env" ]; then
            touch .env
          fi

          echo "=== 3. Configurando google-services.json caso ausente ==="
          if [ ! -f "app/google-services.json" ] && [ ! -f "google-services.json" ]; then
            echo '{"project_info":{"project_number":"123456789","project_id":"dummy-build-ci","storage_bucket":"dummy.appspot.com"},"client":[{"client_info":{"mobilesdk_app_id":"1:123456789:android:dummy","android_client_info":{"package_name":"com.example"}},"api_key":[{"current_key":"dummy_key"}]}]}' > app/google-services.json
          fi

      - name: 🛠️ Garantir Gradle Wrapper Executável
        run: |
          if [ ! -f "./gradlew" ]; then
            echo "gradlew ausente no repositório. Inicializando wrapper..."
            gradle wrapper || true
          fi
          if [ -f "./gradlew" ]; then
            chmod +x ./gradlew
          fi

      - name: 🔨 Compilar APK do Projeto
        run: |
          BUILD_TYPE="${'$'}{{ github.event.inputs.build_variant || 'release' }}"
          GRADLE_CMD="${'$'}{{ github.event.inputs.gradle_task || '' }}"

          if [ -z "${'$'}GRADLE_CMD" ]; then
            GRADLE_CMD="assembleRelease"
          fi

          echo "Iniciando compilação: ${'$'}GRADLE_CMD"

          if [ -f "./gradlew" ]; then
            ./gradlew ${'$'}GRADLE_CMD --no-daemon --stacktrace || ./gradlew assemble --no-daemon --stacktrace
          else
            gradle ${'$'}GRADLE_CMD --no-daemon --stacktrace || gradle assemble --no-daemon --stacktrace
          fi

      - name: 📦 Localizar e Empacotar APKs Gerados
        run: |
          mkdir -p output_apks
          find . -name "*.apk" -type f -exec cp {} output_apks/ \;
          echo "Arquivos APK encontrados:"
          ls -la output_apks/

      - name: 🚀 Publicar Artefato APK para Download
        uses: actions/upload-artifact@v4
        with:
          name: app-compiled-apk
          path: output_apks/*.apk
          retention-days: 14
          if-no-files-found: error
""".trimIndent()
}
