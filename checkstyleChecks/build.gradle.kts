plugins { id("java-library") }

repositories { mavenCentral() }

dependencies {
    implementation("com.puppycrawl.tools:checkstyle:10.26.1")
    api("org.jspecify:jspecify:1.0.0")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
