package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtil;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that ensures module classes have @Doc annotations. */
@NullMarked
public class ModuleDocAnnotationCheck extends AbstractCheck {

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
    if (ast.getType() == TokenTypes.CLASS_DEF) {
      if (!CheckUtils.isRelativeTo(
          this.getFilePath(), "src/main/java/au/com/glob/clodmc/modules/")) {
        return;
      }

      if (CheckUtils.classImplements(ast, "Module")
          && !AnnotationUtil.containsAnnotation(ast, "Doc")) {
        this.log(
            ast, "module class '%s' is missing @Doc annotation".formatted(CheckUtils.getName(ast)));
      }
    }
  }
}
