package au.com.glob.clodmc.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

  private static String readStream(InputStream stream) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      return reader.readLine();
    }
  }

  private static String versionFromBin(String javaFilename)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(javaFilename, "--version");
    Process process = pb.start();
    String stdout = readStream(process.getInputStream());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("java: " + readStream(process.getErrorStream()));
    }
    return stdout.trim();
  }

  public static void main(String[] args) {
    try {
      // check the running java version
      Matcher matcher = Pattern.compile("^(\\d+)\\.").matcher(System.getProperty("java.version"));
      if (matcher.find()) {
        int version = Integer.parseInt(matcher.group(1));
        if (version == JDK_VERSION) {
          System.out.println(System.getProperty("java.home"));
          return;
        }
      }

      // try to find the correct jdk version in the standard macOS locations
      List<Path> paths =
          List.of(
              Path.of(System.getProperty("user.home") + "/Library/Java/JavaVirtualMachines"),
              Path.of("/Library/Java/JavaVirtualMachines"));
      for (Path jvmPath : paths) {
        try (Stream<Path> stream = Files.list(jvmPath)) {
          List<String> versionPaths =
              stream.filter(Files::isDirectory).map(Path::toString).toList();
          for (String versionPath : versionPaths) {
            String javaHome = versionPath + "/Contents/Home";
            String javaFilename = javaHome + "/bin/java";
            if (Files.exists(Path.of(javaFilename))) {
              String versionOutput = versionFromBin(javaFilename);
              Matcher matcher1 = Pattern.compile("^\\S+ (\\d+)\\.").matcher(versionOutput);
              if (matcher1.find()) {
                int version = Integer.parseInt(matcher1.group(1));
                if (version == JDK_VERSION) {
                  System.out.println(javaHome);
                  return;
                }
              }
            }
          }
        }
      }

      throw new RuntimeException("failed to find Java " + JDK_VERSION);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
