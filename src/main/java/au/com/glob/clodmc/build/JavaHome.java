package au.com.glob.clodmc.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** outputs the java home for the correct version (as per gradle.properties) */
@SuppressWarnings("NullabilityAnnotations")
public final class JavaHome {
  private static final String JDK_VERSION = readJavaVersion();
  private static final Path CACHE_FILE = Path.of("build", "java_home-%s".formatted(JDK_VERSION));

  // read java version from gradle.properties
  private static String readJavaVersion() {
    final Properties props = new Properties();
    try {
      props.load(Files.newBufferedReader(Path.of("gradle.properties")));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final String javaVersion = props.getProperty("javaVersion");
    if (javaVersion == null) {
      throw new RuntimeException("javaVersion not found in gradle.properties");
    }
    try {
      Integer.parseInt(javaVersion);
    } catch (final NumberFormatException e) {
      throw new RuntimeException("malformed javaVersion in gradle.properties");
    }
    return javaVersion;
  }

  // read cached java home path if valid
  private static String readCached() {
    try {
      final String cachedJavaHome = Files.readString(CACHE_FILE).trim();
      if (Files.exists(Path.of("%s/bin/java".formatted(cachedJavaHome)))) {
        return cachedJavaHome;
      }
      Files.delete(CACHE_FILE);
    } catch (final IOException e) {
      // ignored
    }
    return null;
  }

  // write java home path to cache file
  private static void writeCached(final String path) {
    try {
      Files.writeString(CACHE_FILE, "%s\n".formatted(path));
    } catch (final IOException e) {
      // ignored
    }
  }

  // find and output java home directory
  public static void main(final String[] args) {
    Util.mainWrapper(
        () -> {
          final String cached = readCached();
          if (cached != null) {
            System.out.println(cached);
            return;
          }

          // check the running java version
          final Matcher matcher =
              Pattern.compile("^(\\d+)\\.").matcher(System.getProperty("java.version"));
          if (matcher.find()) {
            final String version = matcher.group(1);
            if (version.equals(JDK_VERSION)) {
              final String javaHome = System.getProperty("java.home");
              writeCached(javaHome);
              System.out.println(javaHome);
              return;
            }
          }

          // try to find the correct jdk version in the standard macOS locations
          final List<Path> paths =
              List.of(
                  Path.of(
                      "%s/Library/Java/JavaVirtualMachines"
                          .formatted(System.getProperty("user.home"))),
                  Path.of("/Library/Java/JavaVirtualMachines"));
          for (final Path jvmPath : paths) {
            try (final Stream<Path> stream = Files.list(jvmPath)) {
              final List<String> versionPaths =
                  stream.filter(Files::isDirectory).map(Path::toString).toList();
              for (final String versionPath : versionPaths) {
                final String javaHome = "%s/Contents/Home".formatted(versionPath);
                final String javaFilename = "%s/bin/java".formatted(javaHome);
                if (Files.exists(Path.of(javaFilename))) {
                  final String versionOutput = Util.capture(javaFilename, "--version");
                  final Matcher matcher1 =
                      Pattern.compile("^\\S+ (\\d+)\\.").matcher(versionOutput);
                  if (matcher1.find()) {
                    final String version = matcher1.group(1);
                    if (version.equals(JDK_VERSION)) {
                      writeCached(javaHome);
                      System.out.println(javaHome);
                      return;
                    }
                  }
                }
              }
            }
          }

          throw new RuntimeException("failed to find Java %s".formatted(JDK_VERSION));
        });
  }
}
