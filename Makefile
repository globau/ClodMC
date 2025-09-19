java-files := $(shell find src -name '*.java') $(shell find checkstyleChecks/src -name '*.java')
config-files := $(shell find src -name '*.yml') *.gradle.kts Makefile $(shell find config -type f)
version := $(shell ./scripts/version)
gradle := ./gradlew -Dorg.gradle.java.home=$(shell ./scripts/java-home)

build: build/libs/ClodMC-$(version).jar
build/libs/ClodMC-$(version).jar: $(java-files) $(config-files)
	$(gradle) build

.PHONY: format
format:
	$(gradle) :spotlessApply generateReadme

.PHONY: clean
clean:
	$(gradle) clean

.PHONY: test
test:
	$(gradle) check --rerun-tasks
