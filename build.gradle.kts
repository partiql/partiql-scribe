import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.time.Duration
import java.util.Properties

plugins {
    kotlin("jvm") version "1.9.20"
    application
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
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
}

object Deps {
    const val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT_5}"
    const val KOTLIN_TEST = "org.jetbrains.kotlin:kotlin-test:${Versions.KOTLIN}"
    const val KOTLIN_TEST_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.KOTLIN}"
}

val partiqlVersion: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.partiql:partiql-lang:$partiqlVersion")
    // Test
    testImplementation(Deps.KOTLIN_TEST)
    testImplementation(Deps.KOTLIN_TEST_JUNIT)
    testImplementation(Deps.JUNIT_PARAMS)
    testImplementation(project(":partiql-scribe-function-extensions"))
    testImplementation("org.partiql:partiql-function-extensions:$partiqlVersion")
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(providers.gradleProperty("centralPortalUsername"))
            password.set(providers.gradleProperty("centralPortalPassword"))
        }
    }
    connectTimeout.set(Duration.ofMinutes(3))
    clientTimeout.set(Duration.ofMinutes(3))
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
                url = "https://github.com/partiql/partiql-scribe"

                packaging = "jar"
                groupId = "org.partiql"

                scm {
                    connection.set("scm:git:https://github.com/partiql/partiql-scribe.git")
                    developerConnection.set("scm:git:ssh://git@github.com/partiql/partiql-scribe.git")
                    url.set("https://github.com/partiql/partiql-scribe")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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

signing {
    setRequired {
        !version.toString().endsWith("-SNAPSHOT") &&
            gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }
    sign(publishing.publications["main"])
}
