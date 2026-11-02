import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    antlr
    `java-library`
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    antlr("org.antlr:antlr4:4.10.1")
    implementation("org.antlr:antlr4-runtime:4.10.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
}

// The antlr plugin adds the full ANTLR tool to the api configuration; only the runtime is needed at run time.
configurations.api {
    setExtendsFrom(extendsFrom.filter { it.name != "antlr" })
}

val antlrPackage = "org.partiql.scribe.frontend.redshift.antlr"

tasks.generateGrammarSource {
    arguments = listOf("-visitor", "-no-listener", "-long-messages", "-package", antlrPackage)
    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/main/${antlrPackage.replace('.', '/')}").get().asFile
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn(tasks.generateGrammarSource)
}

tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn(tasks.generateTestGrammarSource)
}

ktlint {
    filter {
        exclude { it.file.path.contains("generated-src") }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events.add(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        freeCompilerArgs.add("-jvm-default=enable")
    }
}
