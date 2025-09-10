package au.com.glob.clodmc.build;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** generates README.md from @Doc annotations and src/doc/README.md */
@SuppressWarnings("NullabilityAnnotations")
public class GenerateReadme {
  private static void generateReadme(List<ModuleInfo> modules) throws Exception {
    String template = Files.readString(Path.of("src/doc/README.md"));

    // sort modules by title
    modules.sort(Comparator.comparing(ModuleInfo::title));

    // replace placeholders
    template = template.replace("{{player-modules}}", generateModuleList(modules, "PLAYER"));
    template = template.replace("{{server-modules}}", generateModuleList(modules, "SERVER"));
    template = template.replace("{{admin-modules}}", generateModuleList(modules, "ADMIN"));
    template = template.replace("{{player-commands}}", generateCommandList(modules, "PLAYER"));
    template = template.replace("{{admin-commands}}", generateCommandList(modules, "ADMIN"));

    System.out.print(template);
  }

  private static String generateModuleList(List<ModuleInfo> modules, String audience) {
    StringBuilder sb = new StringBuilder();
    modules.stream()
        .filter((ModuleInfo module) -> audience.equals(module.audience()) && !module.hidden())
        .forEach(
            (ModuleInfo module) ->
                sb.append("- **")
                    .append(module.title())
                    .append("** - ")
                    .append(module.description())
                    .append(" [[src](")
                    .append(module.sourcePath())
                    .append(")]")
                    .append("\n"));
    return sb.toString().trim();
  }

  private static String generateCommandList(List<ModuleInfo> modules, String audience) {
    StringBuilder sb = new StringBuilder();
    List<CommandInfo> allCommands = new ArrayList<>();

    modules.stream()
        .filter((ModuleInfo module) -> audience.equals(module.audience()) && !module.hidden())
        .forEach((ModuleInfo module) -> allCommands.addAll(module.commands()));

    allCommands.sort(Comparator.comparing(CommandInfo::name));

    for (CommandInfo command : allCommands) {
      sb.append("- `/").append(command.name()).append("`");
      if (command.description() != null && !command.description().isEmpty()) {
        sb.append(" - ").append(command.description());
      }
      sb.append("\n");
    }

    return sb.toString().trim();
  }

  private static class DocAnnotationScanner extends TreeScanner<Void, Void> {
    private final List<ModuleInfo> modules = new ArrayList<>();
    private boolean inDocAnnotatedClass = false;
    private String currentClassName = null;
    private String currentFilePath = null;
    private Map<String, CommandInfo> commandMap = new HashMap<>();

    List<ModuleInfo> getModules() {
      return this.modules;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
      if (node.getSourceFile() != null) {
        this.currentFilePath = node.getSourceFile().getName();
      }
      return super.visitCompilationUnit(node, null);
    }

    @Override
    public Void visitClass(ClassTree node, Void unused) {
      final boolean wasInDocClass = this.inDocAnnotatedClass;
      final String previousClassName = this.currentClassName;
      final Map<String, CommandInfo> previousCommands = new HashMap<>(this.commandMap);

      this.inDocAnnotatedClass = false;
      this.currentClassName = node.getSimpleName().toString();
      this.commandMap.clear();

      for (AnnotationTree annotation : node.getModifiers().getAnnotations()) {
        if (!annotation.getAnnotationType().toString().equals("Doc")) {
          continue;
        }
        this.inDocAnnotatedClass = true;
        Map<String, Object> annotationFields = Util.extractAnnotationFields(annotation);

        // scan for commands
        super.visitClass(node, null);

        this.modules.add(
            new ModuleInfo(
                this.currentClassName,
                (String) annotationFields.get("audience"),
                (String) annotationFields.get("title"),
                (String) annotationFields.get("description"),
                (Boolean) annotationFields.getOrDefault("hidden", false),
                new ArrayList<>(this.commandMap.values()),
                this.currentFilePath.replaceFirst("^.+?/src", "/src")));
        break;
      }

      if (!this.inDocAnnotatedClass) {
        super.visitClass(node, null);
      }

      this.inDocAnnotatedClass = wasInDocClass;
      this.currentClassName = previousClassName;
      this.commandMap = previousCommands;
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      if (this.inDocAnnotatedClass) {
        CommandInfo commandInfo = extractCommandInfo(node);
        if (commandInfo != null) {
          CommandInfo existing = this.commandMap.get(commandInfo.name());
          if (commandInfo.description() != null || existing == null) {
            this.commandMap.put(commandInfo.name(), commandInfo);
          }
        }
      }
      return super.visitMethodInvocation(node, null);
    }

    private static CommandInfo extractCommandInfo(MethodInvocationTree node) {
      if (isDescriptionMethod(node)) {
        String description = extractStringArgument(node);
        if (description != null) {
          String commandName = findCommandNameInChain(node);
          if (commandName != null) {
            return new CommandInfo(commandName, description);
          }
        }
      } else if (isCommandBuilderBuild(node)) {
        String commandName = extractStringArgument(node);
        if (commandName != null) {
          return new CommandInfo(commandName, null);
        }
      }
      return null;
    }

    private static String findCommandNameInChain(MethodInvocationTree node) {
      ExpressionTree current = node;
      while (current instanceof MethodInvocationTree methodInvocation) {
        if (isCommandBuilderBuild(methodInvocation)) {
          return extractStringArgument(methodInvocation);
        }
        ExpressionTree methodSelect = methodInvocation.getMethodSelect();
        if (methodSelect instanceof MemberSelectTree memberSelect) {
          current = memberSelect.getExpression();
        } else {
          break;
        }
      }
      return null;
    }

    private static boolean isDescriptionMethod(MethodInvocationTree node) {
      ExpressionTree methodSelect = node.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree memberSelect) {
        return "description".equals(memberSelect.getIdentifier().toString());
      }
      return false;
    }

    private static boolean isCommandBuilderBuild(MethodInvocationTree node) {
      ExpressionTree methodSelect = node.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree memberSelect) {
        if (!"build".equals(memberSelect.getIdentifier().toString())) {
          return false;
        }
        ExpressionTree expression = memberSelect.getExpression();
        return "CommandBuilder".equals(expression.toString());
      }
      return false;
    }

    private static String extractStringArgument(MethodInvocationTree node) {
      if (!node.getArguments().isEmpty()) {
        ExpressionTree firstArg = node.getArguments().getFirst();
        if (firstArg instanceof LiteralTree literalTree) {
          Object value = literalTree.getValue();
          if (value instanceof String string) {
            return string;
          }
        }
      }
      return null;
    }
  }

  private record CommandInfo(String name, String description) {}

  private record ModuleInfo(
      String className,
      String audience,
      String title,
      String description,
      boolean hidden,
      List<CommandInfo> commands,
      String sourcePath) {}

  public static void main(String[] args) {
    Util.mainWrapper(
        () -> {
          List<Path> javaFiles = new ArrayList<>();
          try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            paths.filter((Path path) -> path.toString().endsWith(".java")).forEach(javaFiles::add);
          }

          JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
          try (StandardJavaFileManager fileManager =
              compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromPaths(javaFiles);

            JavacTask javacTask =
                (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
            Iterable<? extends CompilationUnitTree> trees = javacTask.parse();

            DocAnnotationScanner scanner = new DocAnnotationScanner();
            for (CompilationUnitTree tree : trees) {
              scanner.scan(tree, null);
            }

            generateReadme(scanner.getModules());
          }
        });
  }
}
