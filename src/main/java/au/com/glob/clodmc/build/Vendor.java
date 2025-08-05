package au.com.glob.clodmc.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** manages vendoring of third-party dependencies into the codebase */
@SuppressWarnings("NullabilityAnnotations")
public class Vendor {
  private static final Path VENDORED_PATH = Path.of("src/vendored");
  private static final String VENDORED_FILENAME = "vendored.properties";

  // execute command in specified directory
  private static void runIn(Path path, String... command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(path.toFile());
    pb.inheritIO();
    Process process = pb.start();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  // execute command in current directory
  private static void run(String... command) throws IOException, InterruptedException {
    runIn(Path.of(System.getProperty("user.dir")), command);
  }

  // execute command in specified directory and return stdout output
  private static String capture(Path path, String... command)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(path.toFile());
    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    return output;
  }

  // join collection values into sorted comma-separated string
  private static String join(Collection<String> values) {
    return values.stream().sorted().collect(Collectors.joining(","));
  }

  private static class LibraryConfig {
    private final String name;
    String repo;
    String commit;
    String path;
    List<String> include;
    Set<String> exclude;

    LibraryConfig(Path path) throws IOException {
      Properties props = new Properties();
      try (InputStream in = Files.newInputStream(path.resolve(VENDORED_FILENAME))) {
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
      List<String> lines = new ArrayList<>();
      lines.add("# use `scripts/vendor %s` to update".formatted(this.name));
      lines.add("repo=%s".formatted(this.repo));
      lines.add("path=%s".formatted(this.path));
      lines.add("commit=%s".formatted(this.commit));
      lines.add("include=%s".formatted(join(this.include)));
      if (!this.exclude.isEmpty()) {
        lines.add("exclude=%s".formatted(join(this.exclude)));
      }
      Files.write(VENDORED_PATH.resolve(this.name).resolve(VENDORED_FILENAME), lines);
    }
  }

  private record FileCopy(Path src, Path dst) {}

  // vendor specified library into codebase
  public static void main(String[] args) {
    try {
      if (args.length != 1) {
        throw new RuntimeException("usage: scripts/vendor <library-name>");
      }
      String libraryName = args[0];

      // find vendored prop files
      List<Path> paths;
      try (Stream<Path> stream = Files.list(VENDORED_PATH)) {
        paths =
            stream
                .filter(Files::isDirectory)
                .filter((Path p) -> Files.exists(p.resolve(VENDORED_FILENAME)))
                .sorted(Comparator.naturalOrder())
                .toList();
      }

      // load
      LibraryConfig lib = null;
      for (Path path : paths) {
        if (path.getFileName().toString().equals(libraryName)) {
          lib = new LibraryConfig(path);
          break;
        }
      }
      if (lib == null) {
        throw new RuntimeException(
            "unknown library: %s\navailable libraries: %s"
                .formatted(
                    libraryName,
                    paths.stream()
                        .map((p) -> p.getFileName().toString())
                        .collect(Collectors.joining(" "))));
      }

      // vendor
      System.out.printf("vendoring %s%n", lib.name);

      Path srcPath = Paths.get(".git/vendored/%s".formatted(lib.name));
      Path dstPath = Paths.get("src/main/java/vendored/%s".formatted(lib.path));

      // clone or update repo
      try {
        Files.createDirectories(srcPath.getParent());
        if (!Files.exists(srcPath)) {
          runIn(srcPath.getParent(), "git", "clone", "--depth", "1", lib.repo, lib.name);
        } else {
          runIn(srcPath, "git", "pull");
        }
      } catch (RuntimeException e) {
        System.exit(1);
      }

      // check for updates
      if (capture(srcPath, "git", "rev-parse", "HEAD").trim().equals(lib.commit)) {
        throw new RuntimeException("'%s' is already up to date".formatted(lib.name));
      }

      // build list of files to copy
      List<FileCopy> copyFilepaths = new ArrayList<>();
      for (String srcFilename : lib.include) {
        if (srcFilename.endsWith("/")) {
          Path path = srcPath.resolve(srcFilename.substring(0, srcFilename.length() - 1));
          try (Stream<Path> files = Files.walk(path)) {
            Set<String> exclude = lib.exclude;
            files
                .filter(Files::isRegularFile)
                .filter((Path fp) -> !exclude.contains(fp.getFileName().toString()))
                .forEach(
                    (Path srcFilepath) -> {
                      Path dstFilepath = dstPath.resolve(path.relativize(srcFilepath));
                      copyFilepaths.add(new FileCopy(srcFilepath, dstFilepath));
                    });
          }
        } else {
          Path srcFilepath = srcPath.resolve(srcFilename);
          if (!lib.exclude.contains(srcFilepath.getFileName().toString())) {
            Path dstFilepath = dstPath.resolve(srcFilepath.getFileName());
            copyFilepaths.add(new FileCopy(srcFilepath, dstFilepath));
          }
        }
      }

      // copy files
      Set<Path> expectedFilepaths = new HashSet<>();
      copyFilepaths.sort(Comparator.comparing((FileCopy fc) -> fc.src));
      for (FileCopy fc : copyFilepaths) {
        Files.createDirectories(fc.dst.getParent());
        System.out.printf("%s -> %s%n", srcPath.relativize(fc.src), fc.dst);
        Files.copy(fc.src, fc.dst, StandardCopyOption.REPLACE_EXISTING);
        expectedFilepaths.add(dstPath.relativize(fc.dst));
      }

      // delete extra files
      if (Files.exists(dstPath)) {
        Set<Path> actualFilepaths = new HashSet<>();
        try (Stream<Path> files = Files.walk(dstPath)) {
          files
              .filter(Files::isRegularFile)
              .forEach((Path fp) -> actualFilepaths.add(dstPath.relativize(fp)));
        }

        for (Path relFilename : actualFilepaths) {
          if (!expectedFilepaths.contains(relFilename)) {
            Path filepath = dstPath.resolve(relFilename);
            System.out.printf("deleting %s%n", filepath);
            Files.delete(filepath);
          }
        }
      }

      // format before patches
      run("./gradlew", ":spotlessApply");

      // apply patches
      Path patchDir = Paths.get("src/vendored/%s".formatted(lib.name));
      if (Files.exists(patchDir)) {
        try (Stream<Path> patches = Files.list(patchDir)) {
          patches
              .filter((Path p) -> p.getFileName().toString().endsWith(".patch"))
              .sorted(
                  Comparator.comparing(
                      (Path p) -> {
                        String stem = p.getFileName().toString();
                        stem = stem.substring(0, stem.lastIndexOf('.'));
                        return Integer.parseInt(stem.split("-", -1)[0]);
                      }))
              .forEach(
                  (Path patchPath) -> {
                    System.out.printf("patching %s%n", patchPath);
                    try {
                      run("git", "apply", patchPath.toString());
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  });
        }
      }

      // format after patches
      run("./gradlew", ":spotlessApply");

      // update commit .properties
      String sha = capture(srcPath, "git", "rev-parse", "HEAD").trim();
      lib.commit = sha;
      lib.write();

      // test build
      run("make", "clean", "test", "build");

      System.out.printf("\n%s updated to %s%n", lib.name, sha);

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
