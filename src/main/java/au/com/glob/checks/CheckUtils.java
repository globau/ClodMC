package au.com.glob.checks;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CheckUtils {
  private CheckUtils() {}

  public static boolean isRelativeTo(String filename, String relativePath) {
    Path path = Paths.get(filename).getParent();
    while (!path.getFileName().toString().equals("src")) {
      path = path.getParent();
    }
    String rootPath = path.getParent().toString() + "/";

    if (filename.startsWith(rootPath)) {
      filename = filename.substring(rootPath.length() - 1);
    }
    if (!relativePath.startsWith("/")) {
      relativePath = "/" + relativePath;
    }
    if (!relativePath.endsWith("/")) {
      relativePath = relativePath + "/";
    }

    return filename.startsWith(relativePath);
  }
}
