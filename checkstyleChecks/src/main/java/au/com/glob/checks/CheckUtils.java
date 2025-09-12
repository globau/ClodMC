package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CheckUtils {
  private CheckUtils() {}

  static String getRelativeFilename(final File file) {
    final Path absolutePath = Paths.get(file.getAbsolutePath());
    Path current = absolutePath.getParent();
    while (current != null) {
      if (current.resolve("build.gradle.kts").toFile().exists()) {
        return current.relativize(absolutePath).toString();
      }
      current = current.getParent();
    }
    return absolutePath.getFileName().toString();
  }

  static String getRelativeFilename(final String filename) {
    return getRelativeFilename(new File(filename));
  }

  static boolean isRelativeTo(final File file, String relativePath) {
    if (!relativePath.endsWith("/")) {
      relativePath = "%s/".formatted(relativePath);
    }
    return getRelativeFilename(file).startsWith(relativePath);
  }

  static boolean isRelativeTo(final String filename, final String relativePath) {
    return isRelativeTo(new File(filename), relativePath);
  }

  static @Nullable String getName(final DetailAST classDef) {
    final DetailAST nameNode = classDef.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : null;
  }

  static @Nullable String getParameterType(final DetailAST paramDef) {
    final DetailAST typeNode = paramDef.findFirstToken(TokenTypes.TYPE);
    if (typeNode == null) {
      return null;
    }

    // handle simple type (IDENT)
    final DetailAST identNode = typeNode.findFirstToken(TokenTypes.IDENT);
    if (identNode != null) {
      return identNode.getText();
    }

    // handle qualified type (DOT)
    final DetailAST dotNode = typeNode.findFirstToken(TokenTypes.DOT);
    if (dotNode != null) {
      return getQualifiedName(dotNode);
    }

    return null;
  }

  static String getTypeName(final DetailAST ast) {
    final DetailAST nameNode = ast.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : "unknown";
  }

  static String getTypeKind(final int tokenType) {
    return switch (tokenType) {
      case TokenTypes.CLASS_DEF -> "Class";
      case TokenTypes.INTERFACE_DEF -> "Interface";
      case TokenTypes.ENUM_DEF -> "Enum";
      case TokenTypes.RECORD_DEF -> "Record";
      case TokenTypes.ANNOTATION_DEF -> "Annotation";
      default -> "Type";
    };
  }

  private static String getQualifiedName(final DetailAST dotNode) {
    final StringBuilder sb = new StringBuilder();
    buildQualifiedName(dotNode, sb);
    return sb.toString();
  }

  private static void buildQualifiedName(final DetailAST node, final StringBuilder sb) {
    if (node.getType() == TokenTypes.IDENT) {
      sb.append(node.getText());
    } else if (node.getType() == TokenTypes.DOT) {
      final DetailAST left = node.getFirstChild();
      final DetailAST right = left.getNextSibling();
      buildQualifiedName(left, sb);
      sb.append('.');
      buildQualifiedName(right, sb);
    }
  }

  static boolean isTopLevelDeclaration(final DetailAST ast) {
    // check if this is a type declaration
    final int tokenType = ast.getType();
    if (tokenType != TokenTypes.CLASS_DEF
        && tokenType != TokenTypes.INTERFACE_DEF
        && tokenType != TokenTypes.ENUM_DEF
        && tokenType != TokenTypes.RECORD_DEF) {
      return false;
    }

    // check if it's at the top level (not nested inside another type)
    DetailAST parent = ast.getParent();
    while (parent != null) {
      final int parentType = parent.getType();
      if (parentType == TokenTypes.CLASS_DEF
          || parentType == TokenTypes.INTERFACE_DEF
          || parentType == TokenTypes.ENUM_DEF
          || parentType == TokenTypes.RECORD_DEF) {
        return false; // this is a nested type
      }
      parent = parent.getParent();
    }

    return true;
  }

  static boolean classImplements(final DetailAST classDef, final String name) {
    final DetailAST implementsClause = classDef.findFirstToken(TokenTypes.IMPLEMENTS_CLAUSE);
    if (implementsClause == null) {
      return false;
    }

    DetailAST child = implementsClause.getFirstChild();
    while (child != null) {
      if (child.getType() == TokenTypes.IDENT && name.equals(child.getText())) {
        return true;
      }
      // check for qualified names like some.package.Module
      if (child.getType() == TokenTypes.DOT) {
        final String qualifiedName = getQualifiedName(child);
        if (qualifiedName.endsWith(".%s".formatted(name)) || name.equals(qualifiedName)) {
          return true;
        }
      }
      child = child.getNextSibling();
    }

    return false;
  }

  static boolean branchContains(final DetailAST node, final int type) {
    return node.getType() == type || children(node).anyMatch(child -> branchContains(child, type));
  }

  private static Stream<DetailAST> children(final DetailAST node) {
    final Stream.Builder<DetailAST> builder = Stream.builder();
    DetailAST child = node.getFirstChild();
    while (child != null) {
      builder.accept(child);
      child = child.getNextSibling();
    }
    return builder.build();
  }

  static boolean isInAbstractOrNativeMethod(final DetailAST method) {
    final DetailAST modifiers = method.findFirstToken(TokenTypes.MODIFIERS);
    return modifiers != null
        && (CheckUtils.branchContains(modifiers, TokenTypes.ABSTRACT)
            || CheckUtils.branchContains(modifiers, TokenTypes.LITERAL_NATIVE));
  }
}
