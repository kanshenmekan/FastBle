import com.android.build.gradle.internal.api.LibraryVariantOutputImpl

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}
val VERSION_NAME = latestGitTag().ifEmpty { "1.0.1" }
val GROUP_ID = "com.github.kanshenmekan"
val ARTIFACT_ID = "FastBle"
android {
    namespace = "com.hyh.fastble"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        // 剔除这个包下的所有文件（不会移除签名信息）
        resources.excludes.add("META-INF/*******")
    }
//    libraryVariants.configureEach {
//        this.outputs.filterIsInstance<LibraryVariantOutputImpl>().forEach {
//            it.outputFileName = "${project.rootProject.name}-$VERSION_NAME.aar"
//        }
//    }

}
fun latestGitTag(): String {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0").start()
    return process.inputStream.bufferedReader().use { bufferedReader ->
        bufferedReader.readText().trim()
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = GROUP_ID
            artifactId = ARTIFACT_ID
            version = VERSION_NAME
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}