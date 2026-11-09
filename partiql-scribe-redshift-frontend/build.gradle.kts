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

// `src/main/antlr/Redshift.g4` is a placeholder for a deployment's grammar, blank as shipped. While it is blank, the
// module is built without a parser (the jar packages none) and compiled against the parser of the example grammar in
// the test tree, `RedshiftTestExample.g4`, which the tests run with. With a grammar in it, the parser is generated from
// that grammar into the module, the jar packages it, and the tests run against it; the example is left out.
val grammar = layout.projectDirectory.file("src/main/antlr/Redshift.g4")
val grammarIsPlaceholder = grammar.asFile.readText().isBlank()

val antlrPackage = "org.partiql.scribe.frontend.redshift.antlr"
val antlrArguments = listOf("-visitor", "-no-listener", "-long-messages", "-package", antlrPackage)

// ANTLR names the generated classes after the grammar, and the front end is compiled against `RedshiftParser`, so the
// example is generated through a grammar named `Redshift` that imports it. That stub is written by the build.
val exampleGrammarDir = layout.projectDirectory.dir("src/test/antlr/example")

val writeExampleGrammarStub by tasks.registering {
    enabled = grammarIsPlaceholder
    val stubDir = layout.buildDirectory.dir("example-grammar")
    outputs.dir(stubDir)
    doLast {
        stubDir.get().file("Redshift.g4").asFile.writeText(
            """
            // Written by the build: the example grammar under the name the front end's parser classes have.
            grammar Redshift;
            options { caseInsensitive = true; }
            import RedshiftTestExample;

            """.trimIndent(),
        )
    }
}

val exampleParser: SourceSet by sourceSets.creating {
    extensions.getByType<AntlrSourceDirectorySet>().setSrcDirs(listOf(writeExampleGrammarStub))
}

dependencies {
    antlr("org.antlr:antlr4:4.10.1")
    api(project(":"))
    api("org.partiql:partiql-ast:$partiqlVersion")
    api("org.partiql:partiql-spi:$partiqlVersion")
    implementation("org.antlr:antlr4-runtime:4.10.1")
    "exampleParserImplementation"("org.antlr:antlr4-runtime:4.10.1")
    if (grammarIsPlaceholder) {
        compileOnly(exampleParser.output)
        testImplementation(exampleParser.output)
    }

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

// The deployment's grammar, when the placeholder is filled in.
tasks.generateGrammarSource {
    enabled = !grammarIsPlaceholder
    arguments = antlrArguments
    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/main/${antlrPackage.replace('.', '/')}").get().asFile
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn(tasks.generateGrammarSource)
}

// The example grammar, while the placeholder is blank.
tasks.named<AntlrTask>("generateExampleParserGrammarSource") {
    enabled = grammarIsPlaceholder
    // `-lib` is where the stub's import is resolved; the example is an input, so that a change to it regenerates.
    inputs.dir(exampleGrammarDir)
    arguments = antlrArguments + listOf("-lib", exampleGrammarDir.asFile.path)
    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/exampleParser/${antlrPackage.replace('.', '/')}").get().asFile
}

tasks.named("runKtlintCheckOverExampleParserSourceSet") {
    dependsOn(tasks.named("generateExampleParserGrammarSource"))
}

// The test source set holds the example grammar but generates nothing itself: the example is generated above, under
// the name `Redshift`, or not at all.
tasks.generateTestGrammarSource { enabled = false }
sourceSets {
    test {
        java.setSrcDirs(java.srcDirs - tasks.generateTestGrammarSource.get().outputDirectory)
        extensions.getByType<AntlrSourceDirectorySet>().setSrcDirs(emptyList<File>())
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
