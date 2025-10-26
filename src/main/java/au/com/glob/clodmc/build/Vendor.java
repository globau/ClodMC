package au.com.glob.clodmc.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/** manages vendoring of third-party dependencies into the codebase */
@SuppressWarnings("NullabilityAnnotations")
public final class Vendor {
  private static final Path VENDORED_PATH = Path.of("src/vendored");
  private static final String VENDORED_FILENAME = "vendored.properties";

  private static class LibraryConfig {
    private final String name;
    String repo;
    String commit;
    String path;
    List<String> include;
    Set<String> exclude;

    LibraryConfig(final Path path) throws IOException {
      final Properties props = new Properties();
      try (final InputStream in = Files.newInputStream(path.resolve(VENDORED_FILENAME))) {
        props.load(in);
      }
      this.name = path.getFileName().toString();
      this.repo = props.getProperty("repo");
      this.commit = props.getProperty("commit");
      this.path = props.getProperty("path");
      this.include = List.of(props.getProperty("include").split(",", -1));
      this.exclude =
          props.getProperty("exclude") == null
              ? Set.of()
              : Set.of(props.getProperty("exclude").split(",", -1));
    }

    // write configuration back to properties file
    void write() throws IOException {
      final List<String> lines = new ArrayList<>();
      lines.add("# use `scripts/vendor %s` to update".formatted(this.name));
      lines.add("repo=%s".formatted(this.repo));
      lines.add("path=%s".formatted(this.path));
      lines.add("commit=%s".formatted(this.commit));
      lines.add("include=%s".formatted(Util.join(this.include)));
      if (!this.exclude.isEmpty()) {
        lines.add("exclude=%s".formatted(Util.join(this.exclude)));
      }
      Files.write(VENDORED_PATH.resolve(this.name).resolve(VENDORED_FILENAME), lines);
    }
  }

  private record FileCopy(Path src, Path dst) {}

  // vendor specified library into codebase
  public static void main(final String[] args) {
    Util.mainWrapper(
        () -> {
          // find vendored prop files
          final List<Path> librayPaths;
          try (final Stream<Path> stream = Files.list(VENDORED_PATH)) {
            librayPaths =
                stream
                    .filter(Files::isDirectory)
                    .filter((Path p) -> Files.exists(p.resolve(VENDORED_FILENAME)))
                    .sorted(Comparator.naturalOrder())
                    .toList();
          }
          final List<String> libraryNames =
              librayPaths.stream().map((p) -> p.getFileName().toString()).toList();

          if (args.length != 1) {
            throw new RuntimeException(
                "usage: scripts/vendor { %s }".formatted(Util.join(libraryNames, " | ")));
          }
          final String libraryName = args[0];

          // load
          LibraryConfig lib = null;
          for (final Path path : librayPaths) {
            if (path.getFileName().toString().equals(libraryName)) {
              lib = new LibraryConfig(path);
              break;
            }
          }
          if (lib == null) {
            throw new RuntimeException(
                "unknown library: %s\navailable libraries: %s"
                    .formatted(libraryName, Util.join(libraryNames, " ")));
          }

          // vendor
          System.out.printf("vendoring %s...%n", lib.name);

          final Path srcPath = Paths.get(".git/vendored/%s".formatted(lib.name));
          final Path dstPath = Paths.get("src/main/java/vendored/%s".formatted(lib.path));

          // clone or update repo
          System.out.println("updating source...");
          try {
            Files.createDirectories(srcPath.getParent());
            if (!Files.exists(srcPath)) {
              Util.runIn(srcPath.getParent(), "git", "clone", "--depth", "1", lib.repo, lib.name);
            } else {
              Util.runIn(srcPath, "git", "pull");
            }
          } catch (final RuntimeException e) {
            System.exit(1);
          }

          // check for updates
          if (Util.capture(srcPath, "git", "rev-parse", "HEAD").trim().equals(lib.commit)) {
            throw new RuntimeException("'%s' is already up to date".formatted(lib.name));
          }

          // build list of files to copy
          System.out.println("copying...");
          final List<FileCopy> copyFilepaths = new ArrayList<>();
          for (final String srcFilename : lib.include) {
            if (srcFilename.endsWith("/")) {
              final Path path = srcPath.resolve(srcFilename.substring(0, srcFilename.length() - 1));
              try (final Stream<Path> files = Files.walk(path)) {
                final Set<String> exclude = lib.exclude;
                files
                    .filter(Files::isRegularFile)
                    .filter((Path fp) -> !exclude.contains(fp.getFileName().toString()))
                    .forEach(
                        (final Path srcFilepath) -> {
                          final Path dstFilepath = dstPath.resolve(path.relativize(srcFilepath));
                          copyFilepaths.add(new FileCopy(srcFilepath, dstFilepath));
                        });
              }
            } else {
              final Path srcFilepath = srcPath.resolve(srcFilename);
              if (!lib.exclude.contains(srcFilepath.getFileName().toString())) {
                final Path dstFilepath = dstPath.resolve(srcFilepath.getFileName());
                copyFilepaths.add(new FileCopy(srcFilepath, dstFilepath));
              }
            }
          }

          // copy files
          final Set<Path> expectedFilepaths = new HashSet<>();
          copyFilepaths.sort(Comparator.comparing((FileCopy fc) -> fc.src));
          for (final FileCopy fc : copyFilepaths) {
            Files.createDirectories(fc.dst.getParent());
            System.out.printf("%s -> %s%n", srcPath.relativize(fc.src), fc.dst);
            Files.copy(fc.src, fc.dst, StandardCopyOption.REPLACE_EXISTING);
            expectedFilepaths.add(dstPath.relativize(fc.dst));
          }

          // delete extra files
          if (Files.exists(dstPath)) {
            final Set<Path> actualFilepaths = new HashSet<>();
            try (final Stream<Path> files = Files.walk(dstPath)) {
              files
                  .filter(Files::isRegularFile)
                  .forEach((Path fp) -> actualFilepaths.add(dstPath.relativize(fp)));
            }

            for (final Path relFilename : actualFilepaths) {
              if (!expectedFilepaths.contains(relFilename)) {
                final Path filepath = dstPath.resolve(relFilename);
                System.out.printf("deleting %s%n", filepath);
                Files.delete(filepath);
              }
            }
          }

          // format before patches
          System.out.println("formatting...");
          Util.run("make", "format");

          // apply patches
          System.out.println("patching...");
          final Path patchDir = Paths.get("src/vendored/%s".formatted(lib.name));
          if (Files.exists(patchDir)) {
            try (final Stream<Path> patches = Files.list(patchDir)) {
              patches
                  .filter((Path p) -> p.getFileName().toString().endsWith(".patch"))
                  .sorted(
                      Comparator.comparing(
                          (final Path p) -> {
                            String stem = p.getFileName().toString();
                            stem = stem.substring(0, stem.lastIndexOf('.'));
                            return Integer.parseInt(stem.split("-", -1)[0]);
                          }))
                  .forEach(
                      (final Path patchPath) -> {
                        System.out.printf("patching %s%n", patchPath);
                        try {
                          Util.run("git", "apply", patchPath.toString());
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
                      });
            }
          }

          // format after patches
          System.out.println("formatting...");
          Util.run("make", "format");

          // check for changes
          final String modified = Util.capture("git", "status", "--porcelain", dstPath.toString());
          if (modified.trim().isEmpty()) {
            System.out.printf("%n%s is already up to date%n", lib.name);
            return;
          }

          // update commit .properties
          final String sha = Util.capture(srcPath, "git", "rev-parse", "HEAD").trim();
          lib.commit = sha;
          lib.write();

          // test build
          Util.run("make", "clean", "test", "build");

          System.out.printf("%n%s updated to %s%n", lib.name, sha);
        });
  }
}
