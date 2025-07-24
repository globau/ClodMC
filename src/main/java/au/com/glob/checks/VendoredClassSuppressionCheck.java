package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** checkstyle check that ensures vendored classes have @SuppressWarnings({"all"}) annotation. */
@NullMarked
public class VendoredClassSuppressionCheck extends AbstractCheck {
  private static final String EXPECTED_ANNOTATION = "@SuppressWarnings({\"all\"})";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {TokenTypes.CLASS_DEF};
  }

  @Override
  public int[] getAcceptableTokens() {
    return this.getDefaultTokens();
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[0];
  }

  @Override
  public void visitToken(DetailAST ast) {
    Path filePath = Paths.get(this.getFilePath());
    try {
      Path cwd = Path.of(".").toRealPath();
      Path relativePath = cwd.relativize(filePath);
      if (!relativePath.startsWith("src/main/java/vendored")) {
        return;
      }
    } catch (IOException e) {
      return;
    }

    if (this.isInnerClass(ast)) {
      return;
    }

    DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null) {
      this.logMissingAnnotation(ast);
      return;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    boolean foundCorrectAnnotation = false;

    while (annotation != null) {
      if (this.isSuppressWarningsAll(annotation)) {
        foundCorrectAnnotation = true;
        break;
      }
      annotation = annotation.getNextSibling();
      if (annotation != null && annotation.getType() != TokenTypes.ANNOTATION) {
        annotation = null;
      }
    }

    if (!foundCorrectAnnotation) {
      this.logMissingAnnotation(ast);
    }
  }

  private boolean isInnerClass(DetailAST classAst) {
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

  private boolean isSuppressWarningsAll(DetailAST annotation) {
    DetailAST annotationNameNode = annotation.findFirstToken(TokenTypes.IDENT);
    if (annotationNameNode == null || !"SuppressWarnings".equals(annotationNameNode.getText())) {
      return false;
    }

    DetailAST annotationArgs = annotation.findFirstToken(TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR);
    if (annotationArgs == null) {
      DetailAST args = annotation.findFirstToken(TokenTypes.EXPR);
      if (args != null) {
        return this.hasAllParameter(args);
      }
      return false;
    }

    return this.hasAllParameter(annotationArgs);
  }

  private boolean hasAllParameter(@Nullable DetailAST node) {
    DetailAST arrayInit = this.findTokenRecursively(node, TokenTypes.ANNOTATION_ARRAY_INIT);
    if (arrayInit != null) {
      DetailAST child = arrayInit.getFirstChild();
      while (child != null) {
        if (child.getType() == TokenTypes.EXPR) {
          DetailAST stringLiteral = child.findFirstToken(TokenTypes.STRING_LITERAL);
          if (stringLiteral != null && "\"all\"".equals(stringLiteral.getText())) {
            return true;
          }
        }
        child = child.getNextSibling();
      }
    }

    DetailAST stringLiteral = this.findTokenRecursively(node, TokenTypes.STRING_LITERAL);
    return stringLiteral != null && "\"all\"".equals(stringLiteral.getText());
  }

  private @Nullable DetailAST findTokenRecursively(@Nullable DetailAST parent, int tokenType) {
    if (parent == null) {
      return null;
    }

    if (parent.getType() == tokenType) {
      return parent;
    }

    DetailAST child = parent.getFirstChild();
    while (child != null) {
      DetailAST found = this.findTokenRecursively(child, tokenType);
      if (found != null) {
        return found;
      }
      child = child.getNextSibling();
    }

    return null;
  }

  private void logMissingAnnotation(DetailAST classAst) {
    DetailAST nameNode = classAst.findFirstToken(TokenTypes.IDENT);
    String className = nameNode != null ? nameNode.getText() : "unknown";

    this.log(
        classAst,
        "vendored.class.missing.suppression",
        "Vendored class '"
            + className
            + "' must have "
            + EXPECTED_ANNOTATION
            + " annotation to suppress all warnings");
  }
}
