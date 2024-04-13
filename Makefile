.PHONY: format test

java-files := $(shell find src -name '*.java')
config-files := $(shell find src -name '*.yml') build.gradle settings.gradle
version := $(shell src/build/version.py)

build: build/libs/PortalNetwork-$(version).jar
build/libs/PortalNetwork-$(version).jar: $(java-files) $(config-files)
	./gradlew build
	@echo built build/libs/PortalNetwork-$(version).jar

format: build/format
build/format: $(java-files) $(config-files)
	@mkdir -p build
	./gradlew :spotlessApply
	@touch $@

test:
	./gradlew check