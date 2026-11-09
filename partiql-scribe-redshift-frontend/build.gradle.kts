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

// The parser generated from the Redshift grammar (`src/parser/antlr/Redshift.g4`) is its own source set: the main
// code is compiled against it and the tests run with it, but the jar does not package it. A deployment generates the
// parser from its own copy of the grammar and puts the classes on the class path next to this module; see the README.
val parser: SourceSet by sourceSets.creating

val antlrPackage = "org.partiql.scribe.frontend.redshift.antlr"

dependencies {
    antlr("org.antlr:antlr4:4.10.1")
    api(project(":"))
    api("org.partiql:partiql-ast:$partiqlVersion")
    api("org.partiql:partiql-spi:$partiqlVersion")
    implementation("org.antlr:antlr4-runtime:4.10.1")
    compileOnly(parser.output)
    "parserImplementation"("org.antlr:antlr4-runtime:4.10.1")

    testImplementation(parser.output)
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

tasks.named<AntlrTask>("generateParserGrammarSource") {
    arguments = listOf("-visitor", "-no-listener", "-long-messages", "-package", antlrPackage)
    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/parser/${antlrPackage.replace('.', '/')}").get().asFile
}

// Only the parser source set has a grammar: the plugin's generation tasks and output directories for main and test
// have nothing to generate, and are taken out so that no task reads them.
tasks.generateGrammarSource { enabled = false }
tasks.generateTestGrammarSource { enabled = false }
sourceSets {
    main { java.setSrcDirs(java.srcDirs - tasks.generateGrammarSource.get().outputDirectory) }
    test { java.setSrcDirs(java.srcDirs - tasks.generateTestGrammarSource.get().outputDirectory) }
}

tasks.named("runKtlintCheckOverParserSourceSet") {
    dependsOn(tasks.named("generateParserGrammarSource"))
}

ktlint {
    filter {
        exclude { it.file.path.contains("generated-src") }
    }
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
