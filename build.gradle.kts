import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

object Versions {
    // Language
    const val kotlin = "1.6.20"
    const val kotlinLanguage = "1.6"
    const val kotlinApi = "1.6"
    const val jvmTarget = "1.8"
    // Deps
    const val guava = "31.1-jre"
    const val jansi = "2.4.0"
    const val jline = "3.21.0"
    const val junit5 = "5.9.3"
    const val picoCli = "4.7.0"
}

object Deps {
    const val guava = "com.google.guava:guava:${Versions.guava}"
    const val jansi = "org.fusesource.jansi:jansi:${Versions.jansi}"
    const val jline = "org.jline:jline:${Versions.jline}"
    const val junitParams = "org.junit.jupiter:junit-jupiter-params:${Versions.junit5}"
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlin}"
    const val picoCli = "info.picocli:picocli:${Versions.picoCli}"
}

repositories {
    mavenCentral()
}

dependencies {
    // Ship with PartiQL
    api(fileTree("libs") { include("*.jar") })
    implementation(Deps.guava)
    implementation(Deps.jansi)
    implementation(Deps.jline)
    implementation(Deps.picoCli)
    // Test
    testImplementation(Deps.kotlinTest)
    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.junitParams)
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.jvmTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.jvmTarget)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events.add(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

kotlin {
    explicitApi = null
}

application {
    applicationName = "scribe"
    mainClass.set("org.partiql.scribe.shell.Main")
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}

tasks.shadowJar {
    archiveBaseName.set("Scribe")
    archiveClassifier.set("")
    archiveVersion.set("HEAD")
}

tasks.register<Copy>("release") {
    dependsOn("shadowJar")
    from(layout.buildDirectory.file("libs/Scribe-HEAD.jar"))
    into(layout.projectDirectory)
}
