package au.com.glob.clodmc.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** augment README.md with a changes since the last release */
@SuppressWarnings("NullabilityAnnotations")
public final class ReleaseReadme {
  public static void main(final String[] args) {
    Util.mainWrapper(
        () -> {
          final String rootPath = Util.capture("git", "rev-parse", "--show-toplevel").trim();

          System.out.println(Files.readString(Path.of(rootPath, "README.md")));
          System.out.println("## Changes\n");

          final String commitLog = Util.capture("git", "log", "--pretty=format:[%d] %h %s");
          for (String commitLine : commitLog.split("\n", -1)) {
            commitLine = commitLine.trim();

            final Matcher matcher =
                Pattern.compile("^\\[([^]]*)] (\\S+) (.+)$").matcher(commitLine);
            if (!matcher.matches()) {
              throw new RuntimeException("Failed to parse commit line: %s".formatted(commitLine));
            }

            final String meta = matcher.group(1).trim();
            final String sha = matcher.group(2);
            final String desc = matcher.group(3);

            if (meta.startsWith("(tag:")) {
              break;
            }

            System.out.printf("- %s %s%n", sha, desc);
          }
        });
  }
}
