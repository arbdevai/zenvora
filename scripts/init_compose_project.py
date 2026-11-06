#!/usr/bin/env python3
"""
1-Click Android Jetpack Compose Project Starter Kit
Menginisiasi struktur proyek Android Compose modern lengkap dari nol dalam 2 detik:
- Gradle Version Catalog (libs.versions.toml) + Kotlin 2.2 + AGP 8.13 + Compose Material3
- Keystore & FileProvider untuk In-App Update
- CI/CD GitHub Actions (.github/workflows/build-apk.yml) dengan auto-release & cache
- Adaptive Vector Icon murni (bebas AI slop)
- Konfigurasi proguard dan signing release otomatis
"""

import os
import sys
import argparse
import subprocess

def init_project(target_dir, app_name, package_name):
    target_dir = os.path.abspath(target_dir)
    os.makedirs(target_dir, exist_ok=True)
    pkg_path = package_name.replace(".", "/")

    print(f"Menginisiasi proyek '{app_name}' ({package_name}) di: {target_dir}")

    # 1. settings.gradle.kts
    settings_content = f"""pluginManagement {{
    repositories {{
        google {{
            content {{
                includeGroupByRegex("com\\\\.android.*")
                includeGroupByRegex("com\\\\.google.*")
                includeGroupByRegex("androidx.*")
            }}
        }}
        mavenCentral()
        gradlePluginPortal()
    }}
}}
dependencyResolutionManagement {{
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {{
        google()
        mavenCentral()
    }}
}}

rootProject.name = "{app_name}"
include(":app")
"""
    with open(os.path.join(target_dir, "settings.gradle.kts"), "w") as fp:
        fp.write(settings_content)

    # 2. root build.gradle.kts
    root_build = """plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
"""
    with open(os.path.join(target_dir, "build.gradle.kts"), "w") as fp:
        fp.write(root_build)

    # 3. gradle/libs.versions.toml
    gradle_dir = os.path.join(target_dir, "gradle")
    os.makedirs(gradle_dir, exist_ok=True)
    toml_content = """[versions]
agp = "8.13.0"
kotlin = "2.2.20"
compose-bom = "2025.09.00"
activity-compose = "1.11.0"
core-ktx = "1.17.0"
lifecycle = "2.9.4"
navigation = "2.9.4"
datastore = "1.1.7"
retrofit = "3.0.0"
okhttp = "5.1.0"
serialization = "1.9.0"
coil = "3.3.0"
zxing = "3.5.3"
work = "2.10.0"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-datastore = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { module = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter", version = "1.0.0" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version = "1.10.2" }
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }
zxing-core = { module = "com.google.zxing:core", version.ref = "zxing" }
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
"""
    with open(os.path.join(gradle_dir, "libs.versions.toml"), "w") as fp:
        fp.write(toml_content)

    # 4. app/build.gradle.kts
    app_dir = os.path.join(target_dir, "app")
    os.makedirs(app_dir, exist_ok=True)
    app_build = f"""plugins {{
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}}

android {{
    namespace = "{package_name}"
    compileSdk = 36

    defaultConfig {{
        applicationId = "{package_name}"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {{
            useSupportLibrary = true
        }}
    }}

    signingConfigs {{
        create("release") {{
            val keystorePath = System.getenv("SAPAY_KEYSTORE") ?: System.getenv("RELEASE_KEYSTORE")
            if (keystorePath != null && file(keystorePath).exists()) {{
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("SAPAY_STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: System.getenv("SAPAY_KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("SAPAY_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }}
        }}
    }}

    buildTypes {{
        release {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }}
        debug {{
            applicationIdSuffix = ".debug"
        }}
    }}

    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}

    kotlinOptions {{
        jvmTarget = "17"
    }}

    buildFeatures {{
        compose = true
        buildConfig = true
    }}

    packaging {{
        resources {{
            excludes += "/META-INF/{{AL2.0,LGPL2.1}}"
        }}
    }}
}}

dependencies {{
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)

    // Networking & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Image & Media
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Utilities
    implementation(libs.zxing.core)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}}
"""
    with open(os.path.join(app_dir, "build.gradle.kts"), "w") as fp:
        fp.write(app_build)

    # 5. Proguard & Manifest
    with open(os.path.join(app_dir, "proguard-rules.pro"), "w") as fp:
        fp.write("# Proguard rules\n")

    src_main = os.path.join(app_dir, "src", "main")
    res_dir = os.path.join(src_main, "res")
    xml_dir = os.path.join(res_dir, "xml")
    values_dir = os.path.join(res_dir, "values")
    os.makedirs(xml_dir, exist_ok=True)
    os.makedirs(values_dir, exist_ok=True)

    with open(os.path.join(xml_dir, "file_paths.xml"), "w") as fp:
        fp.write("""<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="external_files" path="." />
    <external-path name="external_download" path="Download" />
    <cache-path name="cache" path="." />
</paths>
""")

    with open(os.path.join(values_dir, "strings.xml"), "w") as fp:
        fp.write(f"""<resources>
    <string name="app_name">{app_name}</string>
</resources>
""")

    manifest_content = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@android:style/Theme.Material.Light.NoActionBar"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${{applicationId}}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
"""
    with open(os.path.join(src_main, "AndroidManifest.xml"), "w") as fp:
        fp.write(manifest_content)

    # 6. MainActivity.kt & Theme
    kt_code_dir = os.path.join(src_main, "java", pkg_path)
    os.makedirs(kt_code_dir, exist_ok=True)
    main_activity = f"""package {package_name}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {{
    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {{
            MaterialTheme {{
                Scaffold(modifier = Modifier.fillMaxSize()) {{ innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {{
                        Text(text = "Selamat Datang di {app_name}")
                    }}
                }}
            }}
        }}
    }}
}}
"""
    with open(os.path.join(kt_code_dir, "MainActivity.kt"), "w") as fp:
        fp.write(main_activity)

    # 6.b ui/components/SkeletonComponents.kt
    ui_components_dir = os.path.join(kt_code_dir, "ui", "components")
    os.makedirs(ui_components_dir, exist_ok=True)
    skeleton_content = f"""package {package_name}.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Reusable shimmer modifier for skeleton screens in Jetpack Compose.
 * Creates a smooth, GPU-accelerated gradient sweep without external dependencies.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1100
): Modifier = composed {{
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val isDark = MaterialTheme.colorScheme.surface.run {{ (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }}
    val baseColor = if (isDark) Color(0xFF2B2020) else Color(0xFFF1E6E5)
    val highlightColor = if (isDark) Color(0xFF3F3030) else Color(0xFFFFFFFF)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnimation - 350f, y = translateAnimation - 350f),
        end = Offset(x = translateAnimation, y = translateAnimation)
    )

    clip(shape).background(brush)
}}

@Composable
fun SkeletonBox(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {{
    Box(modifier = modifier.shimmerEffect(shape))
}}
"""
    with open(os.path.join(ui_components_dir, "SkeletonComponents.kt"), "w") as fp:
        fp.write(skeleton_content)

    # 7. CI/CD GitHub Actions (.github/workflows/build-apk.yml)
    gh_dir = os.path.join(target_dir, ".github", "workflows")
    os.makedirs(gh_dir, exist_ok=True)
    ci_content = """name: Build Android APK

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: write

concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Cache Android SDK components
        uses: actions/cache@v4
        id: android-sdk-cache
        with:
          path: |
            ${{ env.ANDROID_HOME }}/platforms/android-36
            ${{ env.ANDROID_HOME }}/build-tools/36.0.0
          key: android-sdk-36-36.0.0

      - name: Accept Android SDK licenses & install components
        if: steps.android-sdk-cache.outputs.cache-hit != 'true'
        run: |
          yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null 2>&1 || true
          $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" || true

      - name: Set up Gradle with caching
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: false

      - name: Make Gradle executable
        run: chmod +x ./gradlew

      - name: Decode release keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          test -n "$KEYSTORE_BASE64"
          mkdir -p "$RUNNER_TEMP/signing"
          printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/signing/release.keystore"
          chmod 600 "$RUNNER_TEMP/signing/release.keystore"

      - name: Build signed release APK
        env:
          RELEASE_KEYSTORE: ${{ runner.temp }}/signing/release.keystore
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease --build-cache --parallel --stacktrace

      - name: Upload release APK
        uses: actions/upload-artifact@v4
        with:
          name: app-release-${{ github.sha }}
          path: app/build/outputs/apk/release/*.apk
          if-no-files-found: error
          retention-days: 14

      - name: Check for Release
        id: check_release
        run: |
          if [[ "${{ github.event.head_commit.message }}" == Release\\ v* ]]; then
            echo "is_release=true" >> $GITHUB_OUTPUT
            TAG=$(echo "${{ github.event.head_commit.message }}" | grep -o 'v[0-9]\\+\\.[0-9]\\+\\.[0-9]\\+' | head -n 1)
            echo "tag=$TAG" >> $GITHUB_OUTPUT
          else
            echo "is_release=false" >> $GITHUB_OUTPUT
          fi

      - name: Publish tagged release
        if: steps.check_release.outputs.is_release == 'true'
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ steps.check_release.outputs.tag }}
          files: app/build/outputs/apk/release/*.apk
          generate_release_notes: true
"""
    with open(os.path.join(gh_dir, "build-apk.yml"), "w") as fp:
        fp.write(ci_content)

    # 8. .gitignore
    with open(os.path.join(target_dir, ".gitignore"), "w") as fp:
        fp.write(""".gradle/
build/
app/build/
*.jks
*.keystore
local.properties
.idea/
*.iml
""")

    # 9. docs/PRD.md dan docs/STATE.md (Persistent Memory System)
    docs_dir = os.path.join(target_dir, "docs")
    os.makedirs(docs_dir, exist_ok=True)
    prd_path = os.path.join(docs_dir, "PRD.md")
    if not os.path.exists(prd_path):
        with open(prd_path, "w") as fp:
            fp.write(f"""# Product Requirement Document (PRD)

## 1. Identitas Aplikasi
- **Nama Aplikasi:** {app_name}
- **Package Name:** {package_name}
- **Min SDK / Target SDK:** 26 / 35 (Android 15 Ready)
- **Arsitektur:** Single-Activity, Clean Architecture (Data -> Domain -> UI), Jetpack Compose Material3.

## 2. Kebutuhan Teknis & Native
- **Native Engine:** None
- **Storage:** Room Database / DataStore
- **Network / Hardware:** OkHttp / Retrofit

## 3. Daftar Fitur Utama (Vertical Slices)
- [ ] Fitur 1: Inisiasi UI Dashboard dan State Management
- [ ] Fitur 2: Integrasi In-App Updater dengan streaming .part

## 4. Batasan & Desain Sistem
- Bebas AI slop (Nol pulsing green dot, nol marquee, nol glow).
- Nol emoji di seluruh UI, microcopy, dan log.
- Gunakan Vector Icons resmi Material3.
- Shimmer skeleton loading untuk semua status tunggu.
""")

    state_path = os.path.join(docs_dir, "STATE.md")
    if not os.path.exists(state_path):
        with open(state_path, "w") as fp:
            fp.write(f"""# Project State & Session Memory

## Status Saat Ini
- **Fase Aktif:** Fase 1 - Fondasi Proyek
- **Versi Terakhir:** v1.0.0 (versionCode: 10000)
- **Status CI Terakhir:** Ready for initial commit

## Fitur yang Selesai Tuntas (Vertical Slices)
- [x] Inisiasi struktur proyek Compose {app_name} ({package_name})
- [x] Template CI/CD GitHub Actions dan Proguard rules
- [x] Setup file persistent state dan PRD

## Catatan Bug Terselesaikan & Workaround (Anti-Mengulang Masalah)
- Belum ada catatan bug.

## Tugas Aktif Selanjutnya (Next Actions)
1. Tentukan kebutuhan spesifik fitur di `docs/PRD.md`.
2. Implementasikan data layer dan ViewModel tuntas hingga Compose Screen.
3. Jalankan `python3 scripts/verify_kotlin_syntax.py .` sebelum push.
""")

    # 10. Panggil generate_launcher_icons.py
    script_dir = os.path.dirname(os.path.abspath(__file__))
    icon_script = os.path.join(script_dir, "generate_launcher_icons.py")
    if os.path.exists(icon_script):
        subprocess.run([sys.executable, icon_script, res_dir, "--app-name", app_name], check=False)

    print(f"\nSelesai! Struktur proyek '{app_name}' berhasil dibuat.")
    print("Petunjuk Signing Release Keystore (Anti-Muter-Muter):")
    print("  Jalankan perintah berikut untuk generate keystore dan pasang GitHub Secrets dalam 3 detik:")
    print(f"  python3 {os.path.join(script_dir, 'setup_release_keystore.py')}\n")

def main():
    parser = argparse.ArgumentParser(description="Inisiasi Proyek Android Compose Baru")
    parser.add_argument("--dir", default=".", help="Target directory")
    parser.add_argument("--name", default="MyAwesomeApp", help="Nama Aplikasi")
    parser.add_argument("--package", default="com.example.app", help="Package Name")
    args = parser.parse_args()

    init_project(args.dir, args.name, args.package)

if __name__ == "__main__":
    main()
