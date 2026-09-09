import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
    id("org.jlleitschuh.gradle.ktlint")
}

val partiqlVersion: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":"))
    api("org.partiql:partiql-plan:$partiqlVersion")
    implementation("org.partiql:partiql-function-extensions:$partiqlVersion")
    implementation("org.partiql:partiql-ast:$partiqlVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.20")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.9"
    kotlinOptions.languageVersion = "1.9"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.9"
    kotlinOptions.languageVersion = "1.9"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events.add(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
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
            artifactId = "partiql-scribe-function-extensions"
            from(components["java"])

            pom {
                name = "PartiQL Scribe Function Extensions"
                description = "Optional Scribe translations for PartiQL function extensions."
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
