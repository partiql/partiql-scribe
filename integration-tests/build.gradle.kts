plugins {
    kotlin("jvm") version "1.9.20"
    application
}

object Versions {
    const val AWS_SDK = "2.29.0"
    const val ION = "1.11.9"
    const val CLIKT = "4.4.0"
    const val PARQUET = "1.14.4"
    const val JACKSON = "2.17.2"
    const val COROUTINES = "1.8.1"
    const val HADOOP = "3.3.6"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // CLI
    implementation("com.github.ajalt.clikt:clikt:${Versions.CLIKT}")

    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:${Versions.AWS_SDK}"))
    implementation("software.amazon.awssdk:athena")
    implementation("software.amazon.awssdk:redshiftdata")
    implementation("software.amazon.awssdk:glue")
    implementation("software.amazon.awssdk:s3")

    // Ion (catalog parsing)
    implementation("com.amazon.ion:ion-java:${Versions.ION}")

    // Parquet (data generation) — deferred until data loader is implemented
    // implementation("org.apache.parquet:parquet-avro:${Versions.PARQUET}")
    // implementation("org.apache.hadoop:hadoop-common:${Versions.HADOOP}") {
    //     exclude(group = "org.slf4j")
    // }

    // JSON (skip-lists, Redshift SUPER loading, reports)
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON}")

    // Coroutines (parallel execution)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.20")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "17"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("org.partiql.scribe.integ.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
