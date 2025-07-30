package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CheckUtils {
  private CheckUtils() {}

  public static boolean isRelativeTo(String filename, String relativePath) {
    Path path = Paths.get(filename).getParent();
    while (!path.getFileName().toString().equals("src")) {
      path = path.getParent();
    }
    String rootPath = path.getParent().toString() + "/";

    if (filename.startsWith(rootPath)) {
      filename = filename.substring(rootPath.length() - 1);
    }
    if (!relativePath.startsWith("/")) {
      relativePath = "/" + relativePath;
    }
    if (!relativePath.endsWith("/")) {
      relativePath = relativePath + "/";
    }

    return filename.startsWith(relativePath);
  }

  public static @Nullable DetailAST getAnnotation(DetailAST methodDef, String name) {
    DetailAST modifiers = methodDef.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null) {
      return null;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    while (annotation != null) {
      DetailAST annotationName = annotation.findFirstToken(TokenTypes.IDENT);
      if (annotationName != null && name.equals(annotationName.getText())) {
        return annotation;
      }
      do {
        annotation = annotation.getNextSibling();
      } while (annotation != null && annotation.getType() != TokenTypes.ANNOTATION);
    }
    return null;
  }

  public static @Nullable String getName(DetailAST classDef) {
    DetailAST nameNode = classDef.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : null;
  }

  public static @Nullable String getParameterType(DetailAST paramDef) {
    DetailAST typeNode = paramDef.findFirstToken(TokenTypes.TYPE);
    if (typeNode == null) {
      return null;
    }

    // handle simple type (IDENT)
    DetailAST identNode = typeNode.findFirstToken(TokenTypes.IDENT);
    if (identNode != null) {
      return identNode.getText();
    }

    // handle qualified type (DOT)
    DetailAST dotNode = typeNode.findFirstToken(TokenTypes.DOT);
    if (dotNode != null) {
      return getQualifiedName(dotNode);
    }

    return null;
  }

  public static String getTypeName(DetailAST ast) {
    DetailAST nameNode = ast.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : "unknown";
  }

  public static String getTypeKind(int tokenType) {
    return switch (tokenType) {
      case TokenTypes.CLASS_DEF -> "Class";
      case TokenTypes.INTERFACE_DEF -> "Interface";
      case TokenTypes.ENUM_DEF -> "Enum";
      case TokenTypes.RECORD_DEF -> "Record";
      case TokenTypes.ANNOTATION_DEF -> "Annotation";
      default -> "Type";
    };
  }

  private static String getQualifiedName(DetailAST dotNode) {
    StringBuilder sb = new StringBuilder();
    buildQualifiedName(dotNode, sb);
    return sb.toString();
  }

  private static void buildQualifiedName(DetailAST node, StringBuilder sb) {
    if (node.getType() == TokenTypes.IDENT) {
      sb.append(node.getText());
    } else if (node.getType() == TokenTypes.DOT) {
      DetailAST left = node.getFirstChild();
      DetailAST right = left.getNextSibling();
      buildQualifiedName(left, sb);
      sb.append('.');
      buildQualifiedName(right, sb);
    }
  }

  public static boolean isTopLevelDeclaration(DetailAST ast) {
    // check if this is a type declaration
    int tokenType = ast.getType();
    if (tokenType != TokenTypes.CLASS_DEF
        && tokenType != TokenTypes.INTERFACE_DEF
        && tokenType != TokenTypes.ENUM_DEF
        && tokenType != TokenTypes.RECORD_DEF) {
      return false;
    }

    // check if it's at the top level (not nested inside another type)
    DetailAST parent = ast.getParent();
    while (parent != null) {
      int parentType = parent.getType();
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

  public static boolean classImplements(DetailAST classDef, String name) {
    DetailAST implementsClause = classDef.findFirstToken(TokenTypes.IMPLEMENTS_CLAUSE);
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
        String qualifiedName = getQualifiedName(child);
        if (qualifiedName.endsWith("." + name) || name.equals(qualifiedName)) {
          return true;
        }
      }
      child = child.getNextSibling();
    }

    return false;
  }

  public static boolean isInnerClass(DetailAST classAst) {
    DetailAST parent = classAst.getParent();
    while (parent != null) {
      if (parent.getType() == TokenTypes.CLASS_DEF
          || parent.getType() == TokenTypes.INTERFACE_DEF
          || parent.getType() == TokenTypes.ENUM_DEF
          || parent.getType() == TokenTypes.RECORD_DEF) {
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }
}
