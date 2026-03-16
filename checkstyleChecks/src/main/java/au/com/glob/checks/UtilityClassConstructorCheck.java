package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

/**
 * checkstyle check that ensures utility classes (final, static-only) have a private no-arg
 * constructor.
 */
@NullMarked
public class UtilityClassConstructorCheck extends AbstractCheck {

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
  public void visitToken(final DetailAST ast) {
    if (!CheckUtils.isRelativeTo(this.getFilePath(), "src/main/java/au/com/glob/clodmc/")) {
      return;
    }

    // top-level class
    if (!CheckUtils.isTopLevelDeclaration(ast)) {
      return;
    }

    // must be final
    final DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null || !CheckUtils.branchContains(modifiers, TokenTypes.FINAL)) {
      return;
    }

    // must have all static methods and variables
    final DetailAST objBlock = ast.findFirstToken(TokenTypes.OBJBLOCK);
    if (objBlock == null) {
      return;
    }
    boolean hasMethodOrField = false;
    DetailAST child = objBlock.getFirstChild();
    while (child != null) {
      if (child.getType() == TokenTypes.METHOD_DEF) {
        final DetailAST methodMods = child.findFirstToken(TokenTypes.MODIFIERS);
        if (methodMods == null
            || !CheckUtils.branchContains(methodMods, TokenTypes.LITERAL_STATIC)) {
          return;
        }
        hasMethodOrField = true;
      } else if (child.getType() == TokenTypes.VARIABLE_DEF) {
        final DetailAST varMods = child.findFirstToken(TokenTypes.MODIFIERS);
        if (varMods == null || !CheckUtils.branchContains(varMods, TokenTypes.LITERAL_STATIC)) {
          return;
        }
        hasMethodOrField = true;
      }
      child = child.getNextSibling();
    }
    if (!hasMethodOrField) {
      return;
    }

    // it's a utility class

    // must have a constructor
    DetailAST ctorDef = null;
    int ctorCount = 0;
    child = objBlock.getFirstChild();
    while (child != null) {
      if (child.getType() == TokenTypes.CTOR_DEF) {
        ctorCount++;
        ctorDef = child;
      }
      child = child.getNextSibling();
    }
    if (ctorCount != 1) {
      this.log(ast, "utility class must have a private no-arg constructor");
      return;
    }

    // .. that is private
    final DetailAST ctorMods = ctorDef.findFirstToken(TokenTypes.MODIFIERS);
    if (ctorMods == null || !CheckUtils.branchContains(ctorMods, TokenTypes.LITERAL_PRIVATE)) {
      this.log(ast, "utility class must have a private no-arg constructor");
      return;
    }

    // .. with no args
    final DetailAST parameters = ctorDef.findFirstToken(TokenTypes.PARAMETERS);
    if (parameters != null && parameters.getChildCount() > 0) {
      this.log(ast, "utility class must have a private no-arg constructor");
      return;
    }

    // .. and an empty body
    final DetailAST body = ctorDef.findFirstToken(TokenTypes.SLIST);
    if (body != null) {
      DetailAST stmt = body.getFirstChild();
      while (stmt != null) {
        if (stmt.getType() != TokenTypes.RCURLY) {
          this.log(ast, "utility class's private no-arg constructor must be empty");
          return;
        }
        stmt = stmt.getNextSibling();
      }
    }
  }
}
