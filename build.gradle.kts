import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.ktlint)
}

group = "no.nav.amt-tiltaksarrangor-bff"
version = "0.0.1-SNAPSHOT"

val ktlintVersion = "1.4.1"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework:spring-aspects")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation(libs.logstash.logback.encoder)
    implementation(libs.nav.common.audit.log)
    implementation(libs.nav.common.log)

    implementation("org.springframework.kafka:spring-kafka")
    implementation(libs.kafka.clients)

    implementation(libs.nav.token.validation.spring)
    implementation(libs.nav.token.client.spring)
    implementation(libs.okhttp)
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(libs.unleash.client.java)

    implementation("org.postgresql:postgresql")

    implementation(libs.nav.amt.lib.models)
    implementation(libs.nav.amt.lib.kafka)

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("com.vaadin.external.google", "android-json")
    }
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.token.validation.spring.test)
    testImplementation(libs.testcontainers.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation("org.awaitility:awaitility")
    testImplementation(libs.nav.amt.lib.testing)
}

// er denne nødvendig?
tasks.getByName<BootJar>("bootJar") {
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
    jvmToolchain(21)
}

ktlint {
    version = ktlintVersion
}

tasks.withType<Test> {
    useJUnitPlatform()
}
