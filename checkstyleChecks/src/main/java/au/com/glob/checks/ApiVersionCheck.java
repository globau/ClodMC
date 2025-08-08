package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * checkstyle check that ensures paper-api version in libs.versions.toml matches api-version in
 * paper-plugin.yml.
 */
@NullMarked
public class ApiVersionCheck extends AbstractFileSetCheck {

  private static final Pattern BUILD_PATTERN = Pattern.compile("^paper\\s*=\\s*\"([^-]+)");
  private static final Pattern PLUGIN_PATTERN = Pattern.compile("api-version:(.+)$");

  private @Nullable String buildVersion;
  private @Nullable String pluginVersion;

  @Override
  protected void processFiltered(File file, FileText fileText) {
    String relativeFilename = CheckUtils.getRelativeFilename(file.getAbsolutePath());
    if (relativeFilename.equals("gradle/libs.versions.toml")) {
      this.processBuildFile(file, fileText);
    } else if (relativeFilename.equals("src/main/resources/paper-plugin.yml")) {
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
