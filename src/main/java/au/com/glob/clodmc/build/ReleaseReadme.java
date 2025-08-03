package au.com.glob.clodmc.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** augment README.md with a list of recent changes from the commit log */
@SuppressWarnings("NullabilityAnnotations")
public class ReleaseReadme {
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

  public static void main(String[] args) {
    try {
      String rootPath = capture("git", "rev-parse", "--show-toplevel").trim();

      System.out.println(Files.readString(Path.of(rootPath, "README.md")));
      System.out.println("## Recent Changes\n");

      int tagCount = 0;
      String lastTag = null;

      String commitLog = capture("git", "log", "--pretty=format:[%d] %h %s");
      String[] commitLines = commitLog.split("\n", -1);

      for (String commitLine : commitLines) {
        commitLine = commitLine.trim();
        Matcher matcher = Pattern.compile("^\\[([^]]*)] (\\S+) (.+)$").matcher(commitLine);

        if (!matcher.matches()) {
          throw new RuntimeException("Failed to parse commit line: %s".formatted(commitLine));
        }

        String meta = matcher.group(1).trim();
        String sha = matcher.group(2);
        String desc = matcher.group(3);

        // extract tags
        List<String> tags = new ArrayList<>();
        if (!meta.isEmpty()) {
          String[] parts = meta.replaceAll("^\\(|\\)$", "").split(", ", -1);
          for (String part : parts) {
            if (part.startsWith("tag: v")) {
              tags.add(part.substring(5)); // remove "tag: " prefix
            }
          }
        }

        String tag = tags.isEmpty() ? null : tags.getFirst();
        if (tag != null) {
          tagCount++;
          if (tagCount == 6) {
            break;
          }
          if (!tag.equals(lastTag)) {
            System.out.printf("\n#### %s%n", tag);
            lastTag = tag;
          }
        }

        System.out.printf("- %s %s%n", sha, desc);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
