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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("NullabilityAnnotations")
public class Vendor {
  private static final Path PROPERTIES_FILE = Path.of("src/vendored/vendored.properties");

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

  private static void run(String... command) throws IOException, InterruptedException {
    runIn(Path.of(System.getProperty("user.dir")), command);
  }

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

  private static String join(Collection<String> values) {
    return values.stream().sorted().collect(Collectors.joining(","));
  }

  private record LibraryConfig(
      String name,
      String repo,
      String commit,
      String path,
      List<String> include,
      Set<String> exclude) {
    LibraryConfig(
        String name, String repo, String commit, String path, String include, String exclude) {
      this(
          name,
          repo,
          commit,
          path,
          List.of(include.split(",", -1)),
          exclude == null ? Set.of() : Set.of(exclude.split(",", -1)));
    }
  }

  private static class Config {
    private final Properties props;
    private final Map<String, LibraryConfig> configs = new HashMap<>();

    Config() throws IOException {
      // load file
      this.props = new Properties();
      try (InputStream in = Files.newInputStream(PROPERTIES_FILE)) {
        this.props.load(in);
      }

      // load config
      for (String key : this.props.stringPropertyNames()) {
        int dot = key.indexOf('.');
        if (dot == -1) {
          continue;
        }
        String name = key.substring(0, dot);
        this.configs.put(
            name,
            new LibraryConfig(
                name,
                this.props.getProperty("%s.repo".formatted(name)),
                this.props.getProperty("%s.commit".formatted(name)),
                this.props.getProperty("%s.path".formatted(name)),
                this.props.getProperty("%s.include".formatted(name)),
                this.props.getProperty("%s.exclude".formatted(name))));
      }
    }

    Set<String> libraryNames() {
      return this.configs.keySet();
    }

    private void write() throws IOException {
      List<LibraryConfig> sortedConfigs =
          this.configs.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(Map.Entry::getValue)
              .toList();

      List<String> lines = new ArrayList<>();
      lines.add("# use `scripts/vendor` to update");
      for (LibraryConfig lib : sortedConfigs) {
        lines.add("#");
        lines.add("%s.repo=%s".formatted(lib.name, lib.repo));
        lines.add("%s.path=%s".formatted(lib.name, lib.path));
        lines.add("%s.commit=%s".formatted(lib.name, lib.commit));
        lines.add("%s.include=%s".formatted(lib.name, join(lib.include)));
        if (!lib.exclude.isEmpty()) {
          lines.add("%s.exclude=%s".formatted(lib.name, join(lib.exclude)));
        }
      }
      Files.write(PROPERTIES_FILE, lines);
    }

    LibraryConfig get(String name) {
      return this.configs.get(name);
    }

    void set(String name, String value) throws IOException {
      this.props.setProperty(name, value);
      this.write();
    }
  }

  private record FileCopy(Path src, Path dst) {}

  public static void main(String[] args) {
    try {
      if (args.length != 1) {
        throw new RuntimeException("usage: scripts/vendor <library-name>");
      }
      String libraryName = args[0];

      // load config
      Config config = new Config();
      LibraryConfig lib = config.get(libraryName);
      if (lib == null) {
        throw new RuntimeException(
            "unknown library: %s\navailable libraries: %s"
                .formatted(libraryName, config.libraryNames()));
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

      // build list of files to copy
      List<FileCopy> copyFilepaths = new ArrayList<>();
      for (String srcFilename : lib.include) {
        if (srcFilename.endsWith("/")) {
          Path path = srcPath.resolve(srcFilename.substring(0, srcFilename.length() - 1));
          try (Stream<Path> files = Files.walk(path)) {
            files
                .filter(Files::isRegularFile)
                .filter((Path fp) -> !lib.exclude.contains(fp.getFileName().toString()))
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
      config.set("%s.commit".formatted(lib.name), sha);

      // test build
      run("make", "clean", "test", "build");

      System.out.printf("\n%s updated to %s%n", lib.name, sha);

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
