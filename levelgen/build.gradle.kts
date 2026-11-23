import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Offline level generator. Run:  ./gradlew :levelgen:run
// Writes app/src/main/assets/levels/levels.bin (5000), daily.bin (366) and docs/levels_report.html.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("com.ngstudio.maze.levelgen.LevelGenMainKt")
    applicationDefaultJvmArgs = listOf("-Xmx6g")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    maxHeapSize = "6g"
}
