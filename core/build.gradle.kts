import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure-Kotlin game rules shared by the Android app and the level generator:
// level model, slide physics, validator, solver, generator, difficulty and the binary level codec.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    maxHeapSize = "3g"
    // The replay test reads the generated asset files from the app module.
    systemProperty("levelsDir", rootProject.file("app/src/main/assets/levels").absolutePath)
    testLogging {
        events("failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
