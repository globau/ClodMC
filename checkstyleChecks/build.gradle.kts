plugins { id("java-library") }

repositories { mavenCentral() }

dependencies {
    implementation(libs.checkstyle)
    implementation(libs.snakeyaml)
    implementation(libs.toml)
    api(libs.jspecify)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
