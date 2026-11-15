import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.c4entertainment.mediahunter"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }
    // Lets Android's per-app language page list the app's languages.
    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "com.c4entertainment.mediahunter"
        minSdk = 25
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }


        // Keys and flags come from, in order: a Gradle property, an environment
        // variable, local.properties (if present), else the default.
        val localProperties = Properties()
        val localFile = project.rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }
        fun setting(name: String, default: String = ""): String =
            (project.findProperty(name) as String?)
                ?: System.getenv(name)
                ?: localProperties.getProperty(name)
                ?: default

        buildConfigField("String", "TMDB_API_KEY", "\"${setting("TMDB_API_KEY")}\"")
        buildConfigField("String", "RAWG_API_KEY", "\"${setting("RAWG_API_KEY")}\"")
        // Only for store screenshots: -PscreenshotMode=true swaps Home's Movies and TV
        // feeds for a fixed list of openly licensed titles. Never true by default.
        buildConfigField("boolean", "SCREENSHOT_MODE", (setting("screenshotMode") == "true").toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Unsigned unless asked: F-Droid signs its own builds. Set
            // signReleaseWithDebugKey=true to sign local release builds.
            val localProps = Properties()
            val lf = project.rootProject.file("local.properties")
            if (lf.exists()) lf.inputStream().use { localProps.load(it) }
            val signWithDebug = ((project.findProperty("signReleaseWithDebugKey") as String?)
                ?: System.getenv("signReleaseWithDebugKey")
                ?: localProps.getProperty("signReleaseWithDebugKey")) == "true"
            if (signWithDebug) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Coil Compose
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Takes a poster's colour for the Details page's bar and glow.
    implementation(libs.androidx.palette.ktx)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp.logging.interceptor)

    // Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // BioMetrics
    implementation(libs.androidx.biometric)

    // Youtube Player
    implementation(libs.youtube.player)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    //Gson
    implementation(libs.gson)

    // TorrentSearch
    implementation(libs.torrentsearch)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

}
