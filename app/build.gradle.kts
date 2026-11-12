import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
}

// local.properties 의 Pleos Client ID/Secret 읽기 (없으면 빈 문자열)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.orai.dahta"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.orai.dahta"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "PLEOS_CLIENT_ID",
            "\"${localProps.getProperty("pleosClientId", "")}\""
        )
        buildConfigField(
            "String",
            "PLEOS_CLIENT_SECRET",
            "\"${localProps.getProperty("pleosClientSecret", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":res"))
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}