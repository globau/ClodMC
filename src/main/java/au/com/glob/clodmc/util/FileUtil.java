package au.com.glob.clodmc.util;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** file and path related utils */
@NullMarked
public final class FileUtil {
  private FileUtil() {}

  public static List<File> listFiles(final File file, final FileFilter filter) {
    return new ArrayList<>(
        Arrays.asList(Objects.requireNonNullElse(file.listFiles(filter), new File[0])));
  }

  public static List<File> listFiles(final Path path, final FileFilter filter) {
    return listFiles(path.toFile(), filter);
  }

  public static List<File> listFiles(
      final File file, final FileFilter filter, final Comparator<File> comparator) {
    final List<File> files = listFiles(file, filter);
    files.sort(comparator);
    return files;
  }
}
