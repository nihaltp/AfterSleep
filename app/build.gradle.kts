import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val keystorePropertiesFile = file("key.properties")
val keystoreProperties = Properties()
val hasValidKeystore = if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
    val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    requiredKeys.all { keystoreProperties.containsKey(it) && (keystoreProperties[it] as? String)?.isNotBlank() == true }
} else false

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.nihaltp.aftersleep"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nihaltp.aftersleep"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    testBuildType = "releaseScreenshot"

    signingConfigs {
        if (hasValidKeystore) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "AfterSleep Debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasValidKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("releaseScreenshot") {
            initWith(getByName("release"))
            applicationIdSuffix = ".screenshot"
            versionNameSuffix = "-screenshot"
            resValue("string", "app_name", "AfterSleep")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.12")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("tools.fastlane:screengrab:2.1.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

android.applicationVariants.all {
    val variant = this
    variant.outputs.all {
        val output = this as? com.android.build.gradle.api.ApkVariantOutput
        output?.versionCodeOverride = variant.versionCode * 1000
    }
}

ktlint {
    version.set("1.2.1")
    android.set(true)
    filter {
        exclude("**/*.gradle.kts")
        exclude("**/build/**")
    }
}

tasks.withType<Detekt> {
    jvmTarget = "17"
    source = project.fileTree("${project.projectDir}/src/main") {
        include("**/*.kt")
    } + project.fileTree("${project.projectDir}/src/test") {
        include("**/*.kt")
    }
    config = files(rootProject.file("detekt.yml"))
    reports {
        xml {
            required.set(true)
            outputLocation.set(file("build/reports/detekt/detekt.xml"))
        }
        html {
            required.set(true)
            outputLocation.set(file("build/reports/detekt/detekt.html"))
        }
    }
}
