package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

/**
 * checkstyle check that ensures java-version in github workflows matches javaVersion in
 * gradle.properties.
 */
@NullMarked
public class GitHubWorkflowJavaVersionCheck extends AbstractFileSetCheck {
  private @Nullable String gradleJavaVersion;

  @Override
  protected void processFiltered(File file, FileText fileText) {
    String relativeFilename = CheckUtils.getRelativeFilename(file);
    if (relativeFilename.equals("gradle.properties")) {
      this.processGradlePropertiesFile(file);
    } else if (CheckUtils.isRelativeTo(file, ".github/workflows")
        && relativeFilename.endsWith(".yml")) {
      this.processWorkflowFile(file);
    }
  }

  private void processGradlePropertiesFile(File file) {
    Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(file.toPath())) {
      properties.load(in);
      this.gradleJavaVersion = properties.getProperty("javaVersion");
    } catch (IOException e) {
      this.log(
          0,
          "failed to read java version from %s: %s"
              .formatted(CheckUtils.getRelativeFilename(file), e));
    }

    if (this.gradleJavaVersion == null) {
      this.log(
          0,
          "failed to find javaVersion property in %s"
              .formatted(CheckUtils.getRelativeFilename(file)));
    }
  }

  @SuppressWarnings("unchecked")
  private void processWorkflowFile(File file) {
    if (this.gradleJavaVersion == null) {
      this.log(0, "GitHubWorkflowJavaVersionCheck: missing java version");
      return;
    }

    Yaml yaml = new Yaml();
    try (InputStream in = Files.newInputStream(file.toPath())) {
      Map<String, Object> workflow = yaml.load(in);
      Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
      if (jobs == null) {
        return;
      }

      for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
        Map<String, Object> job = (Map<String, Object>) jobEntry.getValue();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
        if (steps == null) {
          continue;
        }

        for (Map<String, Object> step : steps) {
          String uses = (String) step.get("uses");
          if (uses == null || !uses.startsWith("actions/setup-java@")) {
            continue;
          }
          Map<String, Object> with = (Map<String, Object>) step.get("with");
          if (with == null) {
            continue;
          }
          Object javaVersionObj = with.get("java-version");
          String workflowJavaVersion = null;
          if (javaVersionObj instanceof String) {
            workflowJavaVersion = (String) javaVersionObj;
          } else if (javaVersionObj instanceof Number) {
            workflowJavaVersion = javaVersionObj.toString();
          }
          if (workflowJavaVersion != null) {
            if (!workflowJavaVersion.equals(this.gradleJavaVersion)) {
              this.log(
                  0,
                  "workflow java version '%s' in %s does not match gradle.properties version '%s'"
                      .formatted(
                          workflowJavaVersion,
                          CheckUtils.getRelativeFilename(file),
                          this.gradleJavaVersion));
            }
          }
        }
      }
    } catch (IOException e) {
      this.log(0, "failed to read workflow from %s: %s".formatted(file.getPath(), e));
    } catch (ClassCastException e) {
      this.log(
          0,
          "unexpected yaml structure in %s: %s".formatted(CheckUtils.getRelativeFilename(file), e));
    }
  }
}
