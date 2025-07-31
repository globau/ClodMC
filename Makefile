java-files := $(shell find src -name '*.java') $(shell find checkstyleChecks/src -name '*.java')
config-files := $(shell find src -name '*.yml') *.gradle.kts Makefile $(shell find config -type f)
xml-files := $(shell find src -name '*.xml')
version := $(shell make -f Makefile-build version)
gradle := ./gradlew -Dorg.gradle.java.home=$(shell make -f Makefile-build javahome)

build: build/libs/ClodMC-$(version).jar
build/libs/ClodMC-$(version).jar: $(java-files) $(config-files)
	$(gradle) build
	@echo built build/libs/ClodMC-$(version).jar

.PHONY: format
format: build/format
build/format: $(java-files) $(test-files) $(config-files) $(xml-files)
	@mkdir -p build
	uvx ruff check --config .ruff.toml --fix-only --unsafe-fixes --exit-zero --show-fixes
	uvx ruff format --config .ruff.toml
	$(gradle) :spotlessApply
	@touch $@

.PHONY: clean
clean:
	$(gradle) clean

.PHONY: test
test:
	uvx ruff check --config .ruff.toml
	uvx ruff format --config .ruff.toml --check
	$(gradle) check --rerun-tasks
