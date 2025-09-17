import net.ltgt.gradle.errorprone.errorprone
import java.io.BufferedReader
import java.io.ByteArrayOutputStream

plugins {
    id("java-library")
    alias(libs.plugins.spotless)
    id("checkstyle")
    alias(libs.plugins.errorprone)
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.bluecolored.de/releases")
    maven(url = "https://repo.opencollab.dev/main")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.griefprevention)
    compileOnly(libs.geyser.api)
    compileOnly(libs.bluemap.api)
    errorprone(libs.errorprone)
    api(libs.jspecify)
    checkstyle(project(":checkstyleChecks"))
}

group = "au.com.glob"

description = "ClodMC"

version =
    ProcessBuilder("./scripts/version")
        .start()
        .inputStream
        .bufferedReader()
        .use(BufferedReader::readText)
        .trim()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString().toInt()))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone.excludedPaths.set(".*/vendored/.*")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    configProperties = configProperties ?: mutableMapOf()
    configProperties!!["basedir"] = projectDir.absolutePath
}

tasks.checkstyleMain {
    source += fileTree(".") { include("build.gradle.kts", "gradle.properties") }
    source += fileTree(".github/workflows") { include("**/*.yml") }
    source += fileTree("gradle") { include("libs.versions.toml") }
    source += fileTree("src/main/resources") { include("**/*.yml") }
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        java {
            googleJavaFormat(
                libs.versions.google.java.format
                    .get(),
            ).reflowLongStrings()
                .skipJavadocFormatting()
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

tasks.register<Exec>("generateReadme") {
    description = "Generate README.md using scripts/generate-readme"
    group = "documentation"
    commandLine("./scripts/generate-readme")
    standardOutput = ByteArrayOutputStream()
    doLast { file("README.md").writeText(standardOutput.toString()) }
}

tasks.register("printVersion") {
    val jarTask = tasks.named<Jar>("jar")
    val jarPath = jarTask.map { task -> project.relativePath(task.archiveFile.get().asFile) }
    doLast { logger.quiet("built " + jarPath.get()) }
}

tasks.named("build") {
    finalizedBy("generateReadme")
    finalizedBy("printVersion")
}

if (file("local.gradle.kts").exists()) {
    apply(from = "local.gradle.kts")
}
