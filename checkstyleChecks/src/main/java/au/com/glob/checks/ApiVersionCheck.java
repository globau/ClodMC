package au.com.glob.checks;

import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

/**
 * checkstyle check that ensures paper-api version in libs.versions.toml matches api-version in
 * paper-plugin.yml.
 */
@NullMarked
public class ApiVersionCheck extends AbstractFileSetCheck {
  private @Nullable String buildVersion;
  private @Nullable String pluginVersion;

  @Override
  protected void processFiltered(final File file, final FileText fileText) {
    final String relativeFilename = CheckUtils.getRelativeFilename(file);
    if (relativeFilename.equals("gradle/libs.versions.toml")) {
      this.processVersionsFile(file);
    } else if (relativeFilename.equals("src/main/resources/paper-plugin.yml")) {
      this.processPluginFile(file);
    }
  }

  private void processVersionsFile(final File file) {
    try (final Reader reader = Files.newBufferedReader(file.toPath())) {
      final TomlTable table = Toml.from(reader);
      this.buildVersion = ((String) table.get("versions.paper"));
      this.buildVersion = this.buildVersion.replaceFirst("-R0\\.1-SNAPSHOT$", "");
    } catch (final IOException e) {
      this.log(
          0,
          "failed to read paper-api version from %s: %s"
              .formatted(CheckUtils.getRelativeFilename(file), e));
    }

    if (this.buildVersion == null) {
      this.log(0, "failed to find paper-api version in %s".formatted(file.getPath()));
    }

    this.checkVersions();
  }

  private void processPluginFile(final File file) {
    final Yaml yaml = new Yaml();
    try (final InputStream in = new FileInputStream(file)) {
      final Map<String, Object> obj = yaml.load(in);
      this.pluginVersion = (String) obj.get("api-version");
    } catch (final IOException e) {
      this.log(
          0,
          "failed to read api-version version from %s: %s"
              .formatted(CheckUtils.getRelativeFilename(file), e));
    }

    if (this.pluginVersion == null) {
      this.log(
          0, "failed to find api-version in %s".formatted(CheckUtils.getRelativeFilename(file)));
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
