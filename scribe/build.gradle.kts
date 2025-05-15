import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    kotlin("jvm") version "1.9.20"
    application
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
}

val properties = "$buildDir/properties"

object Versions {
    // Language
    const val KOTLIN = "1.9.20"
    const val KOTLIN_LANGUAGE = "1.9"
    const val KOTLIN_API = "1.9"
    const val JVM_TARGET = "1.8"

    // Deps
    const val JUNIT_5 = "5.9.3"
    const val PARTIQL = "1.2.1"
}

object Deps {
    const val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT_5}"
    const val KOTLIN_TEST = "org.jetbrains.kotlin:kotlin-test:${Versions.KOTLIN}"
    const val KOTLIN_TEST_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.KOTLIN}"
    const val PARTIQL = "org.partiql:partiql-lang:${Versions.PARTIQL}"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
//    api(Deps.PARTIQL)
    implementation("org.partiql:partiql-lang:1.2.2-SNAPSHOT")
//    add("instrumentedRuntime", project("partiql:partiql-lang"))
    // Test
    testImplementation(Deps.KOTLIN_TEST)
    testImplementation(Deps.KOTLIN_TEST_JUNIT)
    testImplementation(Deps.JUNIT_PARAMS)
}

tasks.register<JavaExec>("runWithInstrumentation") {
    classpath = configurations["instrumentedRuntime"]
//    mainClass.set("com.example.Main")
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
    targetCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
    withJavadocJar()
    withSourcesJar()
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.JVM_TARGET
    kotlinOptions.apiVersion = Versions.KOTLIN_API
    kotlinOptions.languageVersion = Versions.KOTLIN_LANGUAGE
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.JVM_TARGET
    kotlinOptions.apiVersion = Versions.KOTLIN_API
    kotlinOptions.languageVersion = Versions.KOTLIN_LANGUAGE
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
    val commit =
        ByteArrayOutputStream().apply {
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

kotlin {
    explicitApi = ExplicitApiMode.Strict
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

configurations {
    create("instrumentedRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("instrumented-jar"))
        }
    }
}
