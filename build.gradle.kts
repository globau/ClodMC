import net.ltgt.gradle.errorprone.errorprone
import java.io.BufferedReader

plugins {
    id("java-library")
    id("com.diffplug.spotless") version "7.0.2"
    id("checkstyle")
    id("net.ltgt.errorprone") version "4.1.0"
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.bluecolored.de/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.4")
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
    compileOnly("org.jetbrains:annotations:15.0")
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    api("org.jetbrains:annotations:15.0")
}

group = "au.com.glob"

description = "ClodMC"

version =
    ProcessBuilder("./src/build/version.py")
        .start()
        .inputStream
        .bufferedReader()
        .use(BufferedReader::readText)
        .trim()

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone.excludedPaths.set(".*/vendored/.*")
}

checkstyle {
    toolVersion = "10.14.0"
    maxWarnings = 0
}

configurations.checkstyle {
    resolutionStrategy.capabilitiesResolution.withCapability(
        "com.google.collections:google-collections",
    ) {
        select("com.google.guava:guava:23.0")
    }
}

spotless {
    format("text") {
        target(
            "src/main/resources/*.txt",
            ".gitignore",
            "gradle.properties",
            "LICENSE",
            "Makefile",
            "README.md",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    java {
        googleJavaFormat("1.25.2").reflowLongStrings().skipJavadocFormatting()
        formatAnnotations()
    }
    kotlin {
        target("*.kts")
        ktfmt()
        ktlint()
    }
    yaml {
        target("src/main/resources/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") { expand(props) }
}

if (file("local.gradle.kts").exists()) {
    apply(from = "local.gradle.kts")
}
