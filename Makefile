.PHONY: format test

java-files := $(shell find src -name '*.java')
config-files := $(shell find src -name '*.yml') build.gradle settings.gradle Makefile
version := $(shell src/build/version.py)
gradle := ./gradlew $(shell ./src/build/gradle-args)

build: build/libs/ClodMC-$(version).jar
build/libs/ClodMC-$(version).jar: $(java-files) $(config-files)
	$(gradle) build
	@echo built build/libs/ClodMC-$(version).jar

format: build/format
build/format: $(java-files) $(config-files)
	@mkdir -p build
	$(gradle) :spotlessApply
	@touch $@

test:
	@for N in src/test/test-*; do $$N || exit 1; done
	$(gradle) check
