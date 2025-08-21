package au.com.glob.clodmc.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** augment README.md with a changes since the last release */
@SuppressWarnings("NullabilityAnnotations")
public class ReleaseReadme {
  public static void main(String[] args) {
    Util.mainWrapper(
        () -> {
          String rootPath = Util.capture("git", "rev-parse", "--show-toplevel").trim();

          System.out.println(Files.readString(Path.of(rootPath, "README.md")));
          System.out.println("## Changes\n");

          String commitLog = Util.capture("git", "log", "--pretty=format:[%d] %h %s");
          for (String commitLine : commitLog.split("\n", -1)) {
            commitLine = commitLine.trim();

            Matcher matcher = Pattern.compile("^\\[([^]]*)] (\\S+) (.+)$").matcher(commitLine);
            if (!matcher.matches()) {
              throw new RuntimeException("Failed to parse commit line: %s".formatted(commitLine));
            }

            String meta = matcher.group(1).trim();
            String sha = matcher.group(2);
            String desc = matcher.group(3);

            if (meta.startsWith("(tag:")) {
              break;
            }

            System.out.printf("- %s %s%n", sha, desc);
          }
        });
  }
}
