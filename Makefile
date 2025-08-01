java-files := $(shell find src -name '*.java') $(shell find checkstyleChecks/src -name '*.java')
config-files := $(shell find src -name '*.yml') *.gradle.kts Makefile $(shell find config -type f)
xml-files := $(shell find src -name '*.xml')
version := $(shell ./scripts/version)
gradle := ./gradlew -Dorg.gradle.java.home=$(shell ./scripts/java-home)

build: build/libs/ClodMC-$(version).jar
build/libs/ClodMC-$(version).jar: $(java-files) $(config-files)
	$(gradle) build
	@echo built build/libs/ClodMC-$(version).jar

.PHONY: format
format: build/format
build/format: $(java-files) $(test-files) $(config-files) $(xml-files)
	@mkdir -p build
	$(gradle) :spotlessApply
	@touch $@

.PHONY: clean
clean:
	$(gradle) clean

.PHONY: test
test:
	$(gradle) check --rerun-tasks
