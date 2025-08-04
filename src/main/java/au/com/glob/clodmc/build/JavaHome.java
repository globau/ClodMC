package au.com.glob.clodmc.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** outputs the java home */
@SuppressWarnings("NullabilityAnnotations")
public class JavaHome {
  private static final int JDK_VERSION = 21;
  private static final Path CACHE_FILE = Path.of("build/java_home");

  // execute command and return stdout output
  private static String capture(String... command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    return output;
  }

  // read cached java home path if valid
  private static String readCached() {
    try {
      String cachedJavaHome = Files.readString(CACHE_FILE).trim();
      if (Files.exists(Path.of("%s/bin/java".formatted(cachedJavaHome)))) {
        return cachedJavaHome;
      }
      Files.delete(CACHE_FILE);
    } catch (IOException e) {
      // ignored
    }
    return null;
  }

  // write java home path to cache file
  private static void writeCached(String path) {
    try {
      Files.writeString(CACHE_FILE, "%s\n".formatted(path));
    } catch (IOException e) {
      // ignored
    }
  }

  // find and output java 21 home directory
  public static void main(String[] args) {
    try {
      String cached = readCached();
      if (cached != null) {
        System.out.println(cached);
        return;
      }

      // check the running java version
      Matcher matcher = Pattern.compile("^(\\d+)\\.").matcher(System.getProperty("java.version"));
      if (matcher.find()) {
        int version = Integer.parseInt(matcher.group(1));
        if (version == JDK_VERSION) {
          String javaHome = System.getProperty("java.home");
          writeCached(javaHome);
          System.out.println(javaHome);
          return;
        }
      }

      // try to find the correct jdk version in the standard macOS locations
      List<Path> paths =
          List.of(
              Path.of(
                  "%s/Library/Java/JavaVirtualMachines".formatted(System.getProperty("user.home"))),
              Path.of("/Library/Java/JavaVirtualMachines"));
      for (Path jvmPath : paths) {
        try (Stream<Path> stream = Files.list(jvmPath)) {
          List<String> versionPaths =
              stream.filter(Files::isDirectory).map(Path::toString).toList();
          for (String versionPath : versionPaths) {
            String javaHome = "%s/Contents/Home".formatted(versionPath);
            String javaFilename = "%s/bin/java".formatted(javaHome);
            if (Files.exists(Path.of(javaFilename))) {
              String versionOutput = capture(javaFilename, "--version");
              Matcher matcher1 = Pattern.compile("^\\S+ (\\d+)\\.").matcher(versionOutput);
              if (matcher1.find()) {
                int version = Integer.parseInt(matcher1.group(1));
                if (version == JDK_VERSION) {
                  writeCached(javaHome);
                  System.out.println(javaHome);
                  return;
                }
              }
            }
          }
        }
      }

      throw new RuntimeException("failed to find Java %d".formatted(JDK_VERSION));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
