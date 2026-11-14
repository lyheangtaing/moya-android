import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

group = "io.github.lyheangtaing"
version = "0.1.0-SNAPSHOT"

android {
    namespace = "io.github.lyheangtaing.moya"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    api("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.3.2")
}

afterEvaluate {
    publishing.publications.register<MavenPublication>("release") {
        from(components["release"])
        artifactId = "moya-android"
        pom {
            name.set("Moya Android")
            description.set("A Moya-inspired Kotlin networking SDK for Android")
            url.set("https://github.com/lyheangtaing/moya-android")
        }
    }
}
