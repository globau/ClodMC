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
    if (CheckUtils.isTopLevelDeclaration(ast)) {
      // check if the file has @NullMarked annotation
      if (CheckUtils.getAnnotation(ast, "NullMarked") == null) {
        this.log(ast, "file with top-level type declarations is missing @NullMarked annotation");
      }
    }
  }
}
