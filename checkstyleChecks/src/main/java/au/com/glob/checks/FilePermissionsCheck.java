package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import java.io.File;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that ensures files in src/main directory don't have execute permissions. */
@NullMarked
public class FilePermissionsCheck extends AbstractFileSetCheck {

  @Override
  protected void processFiltered(File file, FileText fileText) {
    if (file.canExecute() && CheckUtils.isRelativeTo(file.getAbsolutePath(), "src/main")) {
      this.log(0, "bad file permissions (+x): %s".formatted(file.getPath()));
    }
  }
}
