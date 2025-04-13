plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    kotlin("plugin.serialization") version "2.1.10"
}

android {
    namespace = "ru.maplyb.printmap"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
        version = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {  }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
                groupId = "com.github.abelozDev"
                artifactId = "lib"
                version = libs.versions.lib.version
            }
        }
    }
}
tasks.withType<Test> {
    useJUnitPlatform()
}
dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.window.manager)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil)
    implementation(libs.kotlinx.serialization.json)

    //test
    androidTestImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlin("test"))
    
    //network
    implementation(libs.retrofit)
    implementation(libs.moshi)
    implementation(libs.retrofit.moshi.converter)
    implementation(libs.okhttp.loggint.interceptor)

    implementation(libs.datastore)
    implementation(libs.androidx.ui.android)

    //geo
    implementation(libs.proj4j)
}