package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that ensures module classes have javadoc descriptions. */
@NullMarked
public class ModuleDescriptionCheck extends AbstractCheck {

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
  public boolean isCommentNodesRequired() {
    return true;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (ast.getType() == TokenTypes.CLASS_DEF) {
      if (!CheckUtils.isRelativeTo(
          this.getFilePath(), "src/main/java/au/com/glob/clodmc/modules/")) {
        return;
      }

      if (!CheckUtils.classImplements(ast, "Module")) {
        return;
      }

      if (!this.hasJavadocComment(ast)) {
        this.log(
            ast, "module class '" + CheckUtils.getName(ast) + "' is missing javadoc description");
      }
    }
  }

  private boolean hasJavadocComment(DetailAST classDef) {
    int classLineNo = classDef.getLineNo();

    // traverse the entire file tree to find block comments near this class
    DetailAST root = classDef;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return this.findCommentBeforeLine(root, classLineNo);
  }

  private boolean findCommentBeforeLine(DetailAST node, int targetLine) {
    if (node.getType() == TokenTypes.BLOCK_COMMENT_BEGIN) {
      int commentLine = node.getLineNo();
      if (commentLine < targetLine) {
        return true;
      }
    }

    DetailAST child = node.getFirstChild();
    while (child != null) {
      if (this.findCommentBeforeLine(child, targetLine)) {
        return true;
      }
      child = child.getNextSibling();
    }

    return false;
  }
}
