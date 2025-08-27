package au.com.glob.clodmc.build;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("NullabilityAnnotations")
public class Util {
  // 'static main()' wrapper for standardised exception handling
  static void mainWrapper(ThrowingRunnable runner) {
    try {
      runner.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  // execute command in specified directory and return stdout output
  static String capture(Path path, String... command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();
    pb.directory(path.toFile());
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    return output;
  }

  // execute command in current directory and return stdout output
  static String capture(String... command) throws IOException, InterruptedException {
    return capture(Path.of(System.getProperty("user.dir")), command);
  }

  // execute command in specified directory
  static void runIn(Path path, String... command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(path.toFile());
    pb.inheritIO();
    Process process = pb.start();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  // execute command in current directory
  static void run(String... command) throws IOException, InterruptedException {
    runIn(Path.of(System.getProperty("user.dir")), command);
  }

  // join collection values into sorted comma-separated string
  static String join(Collection<String> values, String delimiter) {
    return values.stream().sorted().collect(Collectors.joining(delimiter));
  }

  // join collection values into sorted comma-separated string
  static String join(Collection<String> values) {
    return join(values, ",");
  }

  // extract field values from annotation tree into map
  static Map<String, Object> extractAnnotationFields(AnnotationTree annotation) {
    Map<String, Object> fields = new HashMap<>();
    for (ExpressionTree argument : annotation.getArguments()) {
      if (argument instanceof AssignmentTree assignment) {
        String fieldName = assignment.getVariable().toString();
        Object value;
        ExpressionTree expression = assignment.getExpression();
        value =
            switch (expression) {
              case LiteralTree literalTree -> literalTree.getValue();
              case MemberSelectTree memberSelect -> memberSelect.getIdentifier().toString();
              case IdentifierTree identifierTree -> identifierTree.getName().toString();
              default -> expression.toString();
            };
        fields.put(fieldName, value);
      }
    }
    return fields;
  }
}
