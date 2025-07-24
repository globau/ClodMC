package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

/**
 * checkstyle check that ensures files with top-level type declarations have @NullMarked annotation.
 */
@NullMarked
public class NullMarkedCheck extends AbstractCheck {

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
      TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.ENUM_DEF, TokenTypes.RECORD_DEF
    };
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
    if (!CheckUtils.isRelativeTo(this.getFilePath(), "src/main/java/au/com/glob/")) {
      return;
    }

    // check if this is a top-level type declaration
    if (this.isTopLevelDeclaration(ast)) {
      // check if the file has @NullMarked annotation
      if (!this.hasNullMarkedAnnotation(ast)) {
        this.log(ast, "file with top-level type declarations is missing @NullMarked annotation");
      }
    }
  }

  private boolean hasNullMarkedAnnotation(DetailAST ast) {
    // traverse up to the compilation unit (root) and search for @NullMarked annotation
    DetailAST root = ast;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return this.findNullMarkedAnnotation(root);
  }

  private boolean findNullMarkedAnnotation(DetailAST node) {
    // check if this node is a @NullMarked annotation
    if (node.getType() == TokenTypes.ANNOTATION) {
      DetailAST annotationName = node.findFirstToken(TokenTypes.IDENT);
      if (annotationName != null && "NullMarked".equals(annotationName.getText())) {
        return true;
      }
    }

    // recursively search children
    DetailAST child = node.getFirstChild();
    while (child != null) {
      if (this.findNullMarkedAnnotation(child)) {
        return true;
      }
      child = child.getNextSibling();
    }

    return false;
  }

  private boolean isTopLevelDeclaration(DetailAST ast) {
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
}
