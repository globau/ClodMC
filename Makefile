.PHONY: build force-build dist \
	clean delete-jar distclean \
	format checkstyle \
	update java \
	test test-perms test-formatting test-style

java-files := $(shell find src -name '*.java')
config-files := $(shell find src -name '*.json') build.gradle settings.gradle
gradle := ./gw

plugin-name := $(shell ./src/build/gradle-var plugin_name)
plugin-dirname := $(shell basename `pwd`)
plugin-build-jar = build/libs/$(plugin-name).jar
plugin-dist-jar := dist/$(plugin-name)-$(shell ./src/build/sem-ver).jar

google-java-format-ver := $(shell ./src/build/gradle-var google_java_format_version)
google-java-format-jar = libs/google-java-format-$(google-java-format-ver).jar
google-java-format-run = ./jdk/bin/java \
	--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
	--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
	--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
	--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
	--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
	-jar $(google-java-format-jar)

checkstyle-ver := $(shell ./src/build/gradle-var checkstyle_version)
checkstyle-jar = libs/checkstyle-$(checkstyle-ver).jar
checkstyle-run = ./jdk/bin/java \
	-jar $(checkstyle-jar) \
	-c .checkstyle/checkstyle.xml

_ := $(shell mkdir -p build dist libs)

build: $(plugin-build-jar)

force-build: delete-jar build
delete-jar:
	@rm -f $(module-build-jar)

$(plugin-build-jar): java $(java-files) $(config-files)
	@rm -f $(module-build-jar)
	@$(gradle) build
	@touch $@

dist: $(plugin-dist-jar)
$(plugin-dist-jar): build
	@cp $(plugin-build-jar) $(plugin-dist-jar)

java:
	@./src/build/download-java

test: test-perms test-style test-formatting

format: build/format
build/format: java $(google-java-format-jar) $(java-files)
	@$(google-java-format-run) --dry-run $(java-files)
	@$(google-java-format-run) --replace $(java-files)
	@touch $@

test-perms:
	@./src/build/test-perms

test-style: java $(checkstyle-jar)
	@$(checkstyle-run) $(java-files)

test-formatting: java $(google-java-format-jar)
	@echo Checking formatting
	@$(google-java-format-run) --dry-run --set-exit-if-changed $(java-files)

$(google-java-format-jar):
	@echo Downloading $(google-java-format-jar)
	@curl --location --output $@ --silent https://github.com/google/google-java-format/releases/download/v$(google-java-format-ver)/google-java-format-$(google-java-format-ver)-all-deps.jar
	@touch $@

checkstyle: build/checkstyle
build/checkstyle: java $(checkstyle-jar) $(java-files) .checkstyle/checkstyle.xml .checkstyle/suppressions.xml
	@$(checkstyle-run) $(java-files)
	@touch $@

$(checkstyle-jar):
	@echo Downloading $(checkstyle-jar)
	@curl --location --output $@ --silent https://github.com/checkstyle/checkstyle/releases/download/checkstyle-$(checkstyle-ver)/checkstyle-$(checkstyle-ver)-all.jar
	@touch $@

update:
	@./src/build/update-deps

clean: java
	@rm -rf bin build/format build/checkstyle
	@$(gradle) clean

distclean: clean
	@rm -rf libs/*.jar
