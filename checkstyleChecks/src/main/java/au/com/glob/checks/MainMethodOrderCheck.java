package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** checkstyle check that ensures main() methods are defined last in build classes. */
@NullMarked
public class MainMethodOrderCheck extends AbstractCheck {

  private final List<DetailAST> methods = new ArrayList<>();
  @Nullable private DetailAST mainMethod = null;

  @Override
  public int[] getDefaultTokens() {
    return new int[] {TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF};
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
  public void beginTree(DetailAST rootAST) {
    this.methods.clear();
    this.mainMethod = null;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (!CheckUtils.isRelativeTo(this.getFilePath(), "src/main/java/au/com/glob/clodmc/build")) {
      return;
    }

    if (ast.getType() == TokenTypes.CLASS_DEF) {
      // process accumulated methods when we finish a class
      this.finishTree(ast.getParent());
      return;
    }

    if (ast.getType() == TokenTypes.METHOD_DEF) {
      String methodName = CheckUtils.getName(ast);
      if ("main".equals(methodName)) {
        // check if it's a static method
        DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
        if (CheckUtils.branchContains(modifiers, TokenTypes.LITERAL_STATIC)) {
          this.mainMethod = ast;
        }
      }
      this.methods.add(ast);
    }
  }

  @Override
  public void finishTree(DetailAST rootAST) {
    if (this.mainMethod != null && !this.methods.isEmpty()) {
      // check if main() is the last method
      DetailAST lastMethod = this.methods.getLast();
      if (!this.mainMethod.equals(lastMethod)) {
        this.log(
            this.mainMethod,
            "main() method should be defined last in build classes, found at line %d but last method is at line %d"
                .formatted(this.mainMethod.getLineNo(), lastMethod.getLineNo()));
      }
    }

    this.methods.clear();
    this.mainMethod = null;
  }
}
