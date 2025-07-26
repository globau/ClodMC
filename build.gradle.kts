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
    maven(url = "https://repo.opencollab.dev/main")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.4")
    compileOnly("org.geysermc.geyser:api:2.7.0-SNAPSHOT")
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
    compileOnly("com.puppycrawl.tools:checkstyle:10.26.1")
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    api("org.jspecify:jspecify:1.0.0")
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

val checkstyleChecksJar by
    tasks.registering(Jar::class) {
        from(layout.buildDirectory.dir("classes/java/main")) { include("au/com/glob/checks/**") }
        archiveFileName.set("checkstyle-checks.jar")
        dependsOn(tasks.compileJava)
    }

checkstyle {
    toolVersion = "10.14.0"
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    dependsOn(checkstyleChecksJar)
    checkstyleClasspath += files(checkstyleChecksJar)
}

tasks.checkstyleMain {
    source += fileTree("src/main/resources") { include("**/*.yml") }
    source += fileTree(".") { include("build.gradle.kts") }
}

configurations.checkstyle {
    resolutionStrategy.capabilitiesResolution.withCapability(
        "com.google.collections:google-collections",
    ) {
        select("com.google.guava:guava:23.0")
    }
}

configurations.compileClasspath {
    resolutionStrategy.capabilitiesResolution.withCapability(
        "com.google.collections:google-collections",
    ) {
        select("com.google.guava:guava:33.3.1-jre")
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
    format("xml") {
        target("**/*.xml")
        targetExclude(".*/**")
        eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.XML)
            .configFile("config/spotless.xml.prefs")
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
