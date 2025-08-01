import net.ltgt.gradle.errorprone.errorprone
import java.io.BufferedReader

plugins {
    id("java-library")
    id("com.diffplug.spotless") version "7.2.1"
    id("checkstyle")
    id("net.ltgt.errorprone") version "4.2.0"
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.bluecolored.de/releases")
    maven(url = "https://repo.opencollab.dev/main")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.4")
    compileOnly("org.geysermc.geyser:api:2.8.2-SNAPSHOT")
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
    errorprone("com.google.errorprone:error_prone_core:2.41.0")
    api("org.jspecify:jspecify:1.0.0")
    checkstyle(project(":checkstyleChecks"))
}

group = "au.com.glob"

description = "ClodMC"

version =
    ProcessBuilder("make", "-f", "Makefile-build", "version")
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
    toolVersion = "10.26.1"
    maxWarnings = 0
}

tasks.checkstyleMain {
    source += fileTree("src/main/resources") { include("**/*.yml") }
    source += fileTree(".") { include("build.gradle.kts") }
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        java {
            googleJavaFormat("1.28.0").reflowLongStrings().skipJavadocFormatting()
            formatAnnotations()
        }
        kotlin {
            target("*.kts")
            ktfmt()
            ktlint()
        }
    }
}

configure(subprojects) {
    spotless {
        format("xml") {
            target("**/*.xml")
            targetExclude(".*/**")
            eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.XML)
                .configFile(rootProject.file("config/spotless.xml.prefs"))
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

tasks.named("spotlessApply") { dependsOn(subprojects.map { "${it.path}:spotlessApply" }) }

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
