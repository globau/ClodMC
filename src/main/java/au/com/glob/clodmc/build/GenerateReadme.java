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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** generates README.md from @doc annotations and src/doc/README.md */
@SuppressWarnings("NullabilityAnnotations")
public final class GenerateReadme {
  private static void generateReadme(final List<ModuleInfo> modules, final boolean isRelease)
      throws Exception {
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

    if (isRelease) {
      generateReleaseChanges();
    }
  }

  private static void generateReleaseChanges() throws Exception {
    System.out.println("## Changes\n");

    final String commitLog = Util.capture("git", "log", "--pretty=format:[%d] %h %s");
    for (String commitLine : commitLog.split("\n", -1)) {
      commitLine = commitLine.trim();

      final Matcher matcher = Pattern.compile("^\\[([^]]*)] (\\S+) (.+)$").matcher(commitLine);
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
  }

  private static String generateModuleList(final List<ModuleInfo> modules, final String audience) {
    final StringBuilder sb = new StringBuilder();
    modules.stream()
        .filter((final ModuleInfo module) -> audience.equals(module.audience()) && !module.hidden())
        .forEach(
            (final ModuleInfo module) ->
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

  private static String generateCommandList(final List<ModuleInfo> modules, final String audience) {
    final StringBuilder sb = new StringBuilder();
    final List<CommandInfo> allCommands = new ArrayList<>();

    modules.stream()
        .filter((final ModuleInfo module) -> audience.equals(module.audience()) && !module.hidden())
        .forEach((final ModuleInfo module) -> allCommands.addAll(module.commands()));

    allCommands.sort(Comparator.comparing(CommandInfo::name));

    for (final CommandInfo command : allCommands) {
      sb.append("- `/").append(command.name()).append("`");
      if (command.description() != null && !command.description().isEmpty()) {
        sb.append(" - ").append(command.description());
      }
      sb.append("\n");
    }

    return sb.toString().trim();
  }

  private static final class DocAnnotationScanner extends TreeScanner<Void, Void> {
    private final List<ModuleInfo> modules = new ArrayList<>();
    private boolean inDocAnnotatedClass = false;
    private String currentClassName = null;
    private String currentFilePath = null;
    private Map<String, CommandInfo> commandMap = new HashMap<>();

    List<ModuleInfo> getModules() {
      return this.modules;
    }

    @Override
    public Void visitCompilationUnit(final CompilationUnitTree node, final Void unused) {
      if (node.getSourceFile() != null) {
        this.currentFilePath = node.getSourceFile().getName();
      }
      return super.visitCompilationUnit(node, null);
    }

    @Override
    public Void visitClass(final ClassTree node, final Void unused) {
      final boolean wasInDocClass = this.inDocAnnotatedClass;
      final String previousClassName = this.currentClassName;
      final Map<String, CommandInfo> previousCommands = new HashMap<>(this.commandMap);

      this.inDocAnnotatedClass = false;
      this.currentClassName = node.getSimpleName().toString();
      this.commandMap.clear();

      for (final AnnotationTree annotation : node.getModifiers().getAnnotations()) {
        if (!annotation.getAnnotationType().toString().equals("Doc")) {
          continue;
        }
        this.inDocAnnotatedClass = true;
        final Map<String, Object> annotationFields = Util.extractAnnotationFields(annotation);

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
    public Void visitMethodInvocation(final MethodInvocationTree node, final Void unused) {
      if (this.inDocAnnotatedClass) {
        final CommandInfo commandInfo = extractCommandInfo(node);
        if (commandInfo != null) {
          final CommandInfo existing = this.commandMap.get(commandInfo.name());
          if (commandInfo.description() != null || existing == null) {
            this.commandMap.put(commandInfo.name(), commandInfo);
          }
        }
      }
      return super.visitMethodInvocation(node, null);
    }

    private static CommandInfo extractCommandInfo(final MethodInvocationTree node) {
      if (isDescriptionMethod(node)) {
        final String description = extractStringArgument(node);
        if (description != null) {
          final String commandName = findCommandNameInChain(node);
          if (commandName != null) {
            return new CommandInfo(commandName, description);
          }
        }
      } else if (isCommandBuilderBuild(node)) {
        final String commandName = extractStringArgument(node);
        if (commandName != null) {
          return new CommandInfo(commandName, null);
        }
      }
      return null;
    }

    private static String findCommandNameInChain(final MethodInvocationTree node) {
      ExpressionTree current = node;
      while (current instanceof final MethodInvocationTree methodInvocation) {
        if (isCommandBuilderBuild(methodInvocation)) {
          return extractStringArgument(methodInvocation);
        }
        final ExpressionTree methodSelect = methodInvocation.getMethodSelect();
        if (methodSelect instanceof final MemberSelectTree memberSelect) {
          current = memberSelect.getExpression();
        } else {
          break;
        }
      }
      return null;
    }

    private static boolean isDescriptionMethod(final MethodInvocationTree node) {
      final ExpressionTree methodSelect = node.getMethodSelect();
      if (methodSelect instanceof final MemberSelectTree memberSelect) {
        return "description".equals(memberSelect.getIdentifier().toString());
      }
      return false;
    }

    private static boolean isCommandBuilderBuild(final MethodInvocationTree node) {
      final ExpressionTree methodSelect = node.getMethodSelect();
      if (methodSelect instanceof final MemberSelectTree memberSelect) {
        if (!"build".equals(memberSelect.getIdentifier().toString())) {
          return false;
        }
        final ExpressionTree expression = memberSelect.getExpression();
        return "CommandBuilder".equals(expression.toString());
      }
      return false;
    }

    private static String extractStringArgument(final MethodInvocationTree node) {
      if (!node.getArguments().isEmpty()) {
        final ExpressionTree firstArg = node.getArguments().getFirst();
        if (firstArg instanceof final LiteralTree literalTree) {
          final Object value = literalTree.getValue();
          if (value instanceof final String string) {
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

  public static void main(final String[] args) {
    Util.mainWrapper(
        () -> {
          // check for --release flag
          final boolean isRelease = args.length > 0 && "--release".equals(args[0]);

          final List<Path> javaFiles = new ArrayList<>();
          try (final Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            paths
                .filter((final Path path) -> path.toString().endsWith(".java"))
                .forEach(javaFiles::add);
          }

          final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
          try (final StandardJavaFileManager fileManager =
              compiler.getStandardFileManager(null, null, null)) {

            final Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromPaths(javaFiles);

            final JavacTask javacTask =
                (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
            final Iterable<? extends CompilationUnitTree> trees = javacTask.parse();

            final DocAnnotationScanner scanner = new DocAnnotationScanner();
            for (final CompilationUnitTree tree : trees) {
              scanner.scan(tree, null);
            }

            generateReadme(scanner.getModules(), isRelease);
          }
        });
  }
}
