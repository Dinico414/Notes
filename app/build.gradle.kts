import com.android.build.api.dsl.ApplicationExtension
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.24"
    alias(libs.plugins.google.gms.google.services)
}

// ── whisper.cpp ──────────────────────────────────────────────────────────────
// Single source of truth is the `whisper` entry in gradle/libs.versions.toml.
// Bumping it: (1) checks out the matching git tag in the vendored clone via
// `syncWhisperCpp`, (2) forces CMake to re-configure with -DWHISPER_CPP_VERSION,
// and (3) changes BuildConfig.WHISPER_CPP_VERSION, which the model manager uses
// to detect and refresh stale on-device model files.
val whisperVersion: String = libs.versions.whisper.get()
val whisperCppDir: File = file("src/main/cpp/whisper.cpp")

/**
 * Checks out the `v<whisper>` tag in the vendored whisper.cpp clone.
 *
 * Idempotent: becomes a no-op once the working tree already sits on the tag,
 * so it stays cheap on every incremental build.
 */
tasks.register<Exec>("syncWhisperCpp") {
    group = "whisper"
    description = "Checks out whisper.cpp v$whisperVersion in src/main/cpp/whisper.cpp"

    val gitDir = whisperCppDir.resolve(".git")
    onlyIf { gitDir.exists() }

    workingDir = whisperCppDir
    commandLine("git", "checkout", "--detach", "--force", "v$whisperVersion")

    // Skip when already on the requested tag so we do not churn the tree.
    onlyIf {
        val current = providers.exec {
            workingDir = whisperCppDir
            commandLine("git", "describe", "--tags", "--exact-match")
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() }.orNull
        current != "v$whisperVersion"
    }
}

tasks.named("preBuild") {
    dependsOn("syncWhisperCpp")
}

/**
 * Keeps the bundled tiny model in step with the version catalog.
 *
 * Bumping `whisper` in libs.versions.toml should not require any manual file work, so this
 * task reconciles the asset itself:
 *
 *  1. If the correctly-stamped asset already exists, it does nothing.
 *  2. Otherwise it re-stamps: `downloadOnBump` decides whether that means a local copy
 *     (default, no network) or re-downloading from upstream.
 *  3. Superseded stamps are pruned so the APK does not carry every old model.
 *
 * Runs before the asset merge, so the renamed file is picked up by the same build.
 */
val upstreamBaseUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
val tinyBaseName = "ggml-tiny"
val tinyAssetDir = file("src/main/assets")
val expectedTinyAsset = "$tinyBaseName-$whisperVersion.bin"

/**
 * Set to `true` to re-download the tiny model from upstream on every version bump instead of
 * copying the previously bundled file. `-Pwhisper.downloadOnBump=true` on the command line.
 *
 * Default is a local copy: faster, works offline, and the weights are unchanged between
 * whisper.cpp releases unless upstream republishes them.
 */
val downloadOnBump: Boolean =
    (project.findProperty("whisper.downloadOnBump") as String?)?.toBoolean() ?: false

tasks.register("syncWhisperAssets") {
    group = "whisper"
    description = "Ensures the bundled tiny model is stamped for whisper $whisperVersion"

    inputs.property("version", whisperVersion)
    inputs.property("downloadOnBump", downloadOnBump)

    // Re-run whenever the catalogue version changes, even if the directory looks unchanged.
    outputs.upToDateWhen { false }

    doLast {
        val expected = File(tinyAssetDir, expectedTinyAsset)

        if (expected.exists() && expected.length() > 0) {
            logger.lifecycle("[whisper] tiny asset already stamped: ${expected.name}")
        } else {
            tinyAssetDir.mkdirs()

            // Prefer an existing stamp; fall back to the un-stamped legacy name.
            val source = tinyAssetDir.listFiles { f: File ->
                f.isFile && f.name.startsWith(tinyBaseName) && f.name.endsWith(".bin")
            }?.maxByOrNull { it.length() }
                ?: tinyAssetDir.resolve("$tinyBaseName.bin").takeIf { it.exists() }

            if (downloadOnBump) {
                val url = "$upstreamBaseUrl$tinyBaseName.bin"
                logger.lifecycle("[whisper] downloading tiny model from $url")
                val temp = File(tinyAssetDir, "${expected.name}.part")
                URI(url).toURL().openStream().use { input ->
                    temp.outputStream().buffered().use { output ->
                        input.copyTo(output, bufferSize = 1 shl 16)
                    }
                }
                check(temp.length() > 0) { "Downloaded tiny model is empty" }
                temp.renameTo(expected)
            } else {
                check(source != null) {
                    "No bundled tiny model found in ${tinyAssetDir.path} to re-stamp as " +
                        "'$expectedTinyAsset'. Place a '$tinyBaseName-*.bin' (or '$tinyBaseName.bin') " +
                        "there, or re-run with -Pwhisper.downloadOnBump=true to fetch it. " +
                        "Upstream: $upstreamBaseUrl$tinyBaseName.bin"
                }
                logger.lifecycle("[whisper] re-stamping ${source.name} -> ${expected.name}")
                source.copyTo(expected, overwrite = true)
            }
        }

        // Prune superseded stamps so only the current one ships in the APK.
        tinyAssetDir.listFiles { f: File ->
            f.isFile && f.name.startsWith("$tinyBaseName-") &&
                f.name.endsWith(".bin") && f.name != expectedTinyAsset
        }?.forEach { stale ->
            logger.lifecycle("[whisper] pruning stale asset ${stale.name}")
            stale.delete()
        }

        // Final guard: fail loudly rather than shipping an APK whose model would be missing.
        check(expected.exists() && expected.length() > 0) {
            "Bundled tiny asset '$expectedTinyAsset' is missing after sync."
        }
    }
}

// Must run before assets are merged into the APK.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn("syncWhisperAssets")
}

tasks.named("preBuild") {
    dependsOn("syncWhisperAssets")
}

configure<ApplicationExtension> {
    namespace = "com.xenonware.notes"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xenonware.notes"
        minSdk = 29
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
        buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
        buildConfigField("String", "WHISPER_CPP_VERSION", "\"$whisperVersion\"")
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DWHISPER_CPP_VERSION=$whisperVersion",
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "28.0.12433566"

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-d"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
            buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
            buildConfigField("String", "WHISPER_CPP_VERSION", "\"$whisperVersion\"")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
            buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
            buildConfigField("String", "WHISPER_CPP_VERSION", "\"$whisperVersion\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.xenon.commons)
    implementation(libs.reorderable)
    implementation(libs.androidx.material3.window.size.class1.android)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.compose.ui.graphics)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.androidx.animation.graphics)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui)
    implementation(libs.androidx.appcompat)
    implementation(libs.mathparser.org.mxparser)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.vosk.android)

    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material3.window.size.class1)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.okhttp)

}
