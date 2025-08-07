import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}
val VERSION_NAME = "1.0"
val GROUP_ID = "com.github.kanshenmekan"
val ARTIFACT_ID = "FastBle"
android {
    namespace = "com.huyuhui.fastble"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    packaging {
        // 剔除这个包下的所有文件（不会移除签名信息）
        resources.excludes.add("META-INF/*******")
    }
    publishing {
        singleVariant("release") {}
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = GROUP_ID
                artifactId = ARTIFACT_ID
                version = VERSION_NAME
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
}