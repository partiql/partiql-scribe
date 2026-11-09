import org.gradle.api.plugins.antlr.AntlrSourceDirectorySet
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

val partiqlVersion: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

// The tests run with a parser generated from an example of the grammar, kept in the test tree (`src/test/antlr`):
// the main code is compiled against it, and the jar does not package it. A deployment generates its parser from its
// own grammar; see the README.
val exampleParser: SourceSet by sourceSets.creating {
    extensions.getByType<AntlrSourceDirectorySet>().setSrcDirs(listOf("src/test/antlr"))
}

val antlrPackage = "org.partiql.scribe.frontend.redshift.antlr"

dependencies {
    antlr("org.antlr:antlr4:4.10.1")
    api(project(":"))
    api("org.partiql:partiql-ast:$partiqlVersion")
    api("org.partiql:partiql-spi:$partiqlVersion")
    implementation("org.antlr:antlr4-runtime:4.10.1")
    compileOnly(exampleParser.output)
    "exampleParserImplementation"("org.antlr:antlr4-runtime:4.10.1")

    testImplementation(exampleParser.output)
    testImplementation("org.partiql:partiql-parser:$partiqlVersion")
    testImplementation("org.partiql:partiql-planner:$partiqlVersion")
    testImplementation("org.partiql:partiql-function-extensions:$partiqlVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
}

// The antlr plugin adds the full ANTLR tool to the api configuration; only the runtime is needed at run time.
configurations.api {
    setExtendsFrom(extendsFrom.filter { it.name != "antlr" })
}

tasks.named<AntlrTask>("generateExampleParserGrammarSource") {
    arguments = listOf("-visitor", "-no-listener", "-long-messages", "-package", antlrPackage)
    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/exampleParser/${antlrPackage.replace('.', '/')}").get().asFile
}

// The example grammar is generated into the exampleParser source set only: the plugin's generation tasks for main and
// test have nothing to generate, and their output directories are taken out so that no task reads them.
tasks.generateGrammarSource { enabled = false }
tasks.generateTestGrammarSource { enabled = false }
sourceSets {
    main { java.setSrcDirs(java.srcDirs - tasks.generateGrammarSource.get().outputDirectory) }
    test {
        java.setSrcDirs(java.srcDirs - tasks.generateTestGrammarSource.get().outputDirectory)
        extensions.getByType<AntlrSourceDirectorySet>().setSrcDirs(emptyList<File>())
    }
}

tasks.named("runKtlintCheckOverExampleParserSourceSet") {
    dependsOn(tasks.named("generateExampleParserGrammarSource"))
}

// Reuse the Scribe test utilities (catalog loading, fixture loading) and the Scribe test resources (catalogs and
// fixtures) from the root project.
sourceSets {
    test {
        kotlin.srcDir(rootProject.file("src/test/kotlin/org/partiql/scribe/utils"))
        resources.srcDir(rootProject.file("src/test/resources"))
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
