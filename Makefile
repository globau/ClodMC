java-files := $(shell find src -name '*.java')
test-files := $(shell find src/test -name '*.java' -or -name '*.py')
config-files := $(shell find src -name '*.yml') *.gradle.kts Makefile
version := $(shell src/build/version.py)
gradle := ./gradlew $(shell ./src/build/gradle-args.py)

build: build/libs/ClodMC-$(version).jar
build/libs/ClodMC-$(version).jar: $(java-files) $(config-files)
	$(gradle) build
	@echo built build/libs/ClodMC-$(version).jar

.PHONY: format
format: build/format
build/format: $(java-files) $(test-files) $(config-files)
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
	@for N in src/test/test-*; do $$N || exit 1; done
	$(gradle) check --rerun-tasks
