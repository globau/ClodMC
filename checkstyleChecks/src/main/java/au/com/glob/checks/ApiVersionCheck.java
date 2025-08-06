package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * checkstyle check that ensures paper-api version in build.gradle.kts matches api-version in
 * paper-plugin.yml.
 */
@NullMarked
public class ApiVersionCheck extends AbstractFileSetCheck {

  private static final Pattern BUILD_PATTERN =
      Pattern.compile("^\\s*compileOnly\\(\"io\\.papermc\\.paper:paper-api:([^-]+)");
  private static final Pattern PLUGIN_PATTERN = Pattern.compile("api-version:(.+)$");

  private @Nullable String buildVersion;
  private @Nullable String pluginVersion;

  @Override
  protected void processFiltered(File file, FileText fileText) {
    String fileName = file.getName();

    if ("build.gradle.kts".equals(fileName)) {
      this.processBuildFile(file, fileText);
    } else if ("paper-plugin.yml".equals(fileName)
        && file.getPath().contains("src/main/resources")) {
      this.processPluginFile(file, fileText);
    }
  }

  private void processBuildFile(File file, FileText fileText) {
    for (int i = 0; i < fileText.size(); i++) {
      String line = fileText.get(i);
      Matcher matcher = BUILD_PATTERN.matcher(line);
      if (matcher.find()) {
        this.buildVersion = matcher.group(1);
        break;
      }
    }

    if (this.buildVersion == null) {
      this.log(0, "failed to find paper-api version in %s".formatted(file.getPath()));
    }

    this.checkVersions();
  }

  private void processPluginFile(File file, FileText fileText) {
    for (int i = 0; i < fileText.size(); i++) {
      String line = fileText.get(i);
      Matcher matcher = PLUGIN_PATTERN.matcher(line);
      if (matcher.find()) {
        this.pluginVersion = matcher.group(1).strip().replaceAll("[\"']", "");
        break;
      }
    }

    if (this.pluginVersion == null) {
      this.log(0, "failed to find api-version in %s".formatted(file.getPath()));
    }

    this.checkVersions();
  }

  private void checkVersions() {
    if (this.buildVersion != null && this.pluginVersion != null) {
      if (!this.buildVersion.equals(this.pluginVersion)) {
        this.log(
            0,
            "build paper-api version '%s' does not match plugin '%s'"
                .formatted(this.buildVersion, this.pluginVersion));
      }
    }
  }
}
