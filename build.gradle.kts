import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    kotlin("jvm") version "1.6.20"
    application
    `maven-publish`
}

val properties = "$buildDir/properties"

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
    const val partiql = "0.14.9"
}

object Deps {
    const val jansi = "org.fusesource.jansi:jansi:${Versions.jansi}"
    const val jline = "org.jline:jline:${Versions.jline}"
    const val junitParams = "org.junit.jupiter:junit-jupiter-params:${Versions.junit5}"
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlin}"
    const val picoCli = "info.picocli:picocli:${Versions.picoCli}"
    const val partiql = "org.partiql:partiql-lang-kotlin:${Versions.partiql}"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(Deps.partiql)
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
    withJavadocJar()
    withSourcesJar()
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

sourceSets {
    main {
        output.dir(properties)
    }
}

application {
    applicationName = "scribe"
    mainClass.set("org.partiql.scribe.shell.Main")
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}

tasks.processResources {
    dependsOn(tasks.findByName("generateProperties"))
}

tasks.create("generateProperties") {
    val propertiesFile = file("$properties/scribe.properties")
    val commit = ByteArrayOutputStream().apply {
        exec {
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = this@apply
        }
    }
    // write properties
    propertiesFile.parentFile.mkdirs()
    val properties = Properties()
    properties.setProperty("version", version.toString())
    properties.setProperty("commit", commit.toString().trim())
    properties.store(FileOutputStream(propertiesFile), null)
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("import"))
        }
    }
    publications {
        create<MavenPublication>("main") {

            artifactId = "scribe"
            from(components["java"])

            pom {
                name = "PartiQL Scribe"
                description = "The PartiQL Scribe query transpiler framework."
                url = "https://partiql.org"

                packaging = "jar"
                groupId = "org.partiql"
                version = "0.1"

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("PartiQL Team")
                        email.set("partiql-dev@amazon.com")
                        organization.set("PartiQL")
                        organizationUrl.set("https://github.com/partiql")
                    }
                }
            }
        }
    }
}
