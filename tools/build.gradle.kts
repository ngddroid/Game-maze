import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Asset tools (no downloads, everything is generated):
//   ./gradlew :tools:run --args="audio"   -> app/src/main/res/raw/*.wav (synthesized SFX + music)
//   ./gradlew :tools:run --args="store"   -> docs/store/icon_512.png + feature_graphic.png
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

application {
    mainClass.set("com.ngstudio.maze.tools.ToolsMainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
    systemProperty("java.awt.headless", "true")
}
