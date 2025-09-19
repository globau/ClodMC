package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that ensures classes have javadoc descriptions. */
@NullMarked
public class DescriptionCheck extends AbstractCheck {

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
      TokenTypes.CLASS_DEF, TokenTypes.RECORD_DEF, TokenTypes.ENUM_DEF, TokenTypes.INTERFACE_DEF
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
  public boolean isCommentNodesRequired() {
    return true;
  }

  @Override
  public void visitToken(final DetailAST ast) {
    // modules use @doc
    if (CheckUtils.classImplements(ast, "Module")) {
      return;
    }

    if (!this.hasJavadocComment(ast)) {
      this.log(ast, "class '%s' is missing javadoc description".formatted(CheckUtils.getName(ast)));
    }
  }

  private boolean hasJavadocComment(final DetailAST classDef) {
    final int classLineNo = classDef.getLineNo();

    // traverse the entire file tree to find block comments near this class
    DetailAST root = classDef;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return this.findCommentBeforeLine(root, classLineNo);
  }

  private boolean findCommentBeforeLine(final DetailAST node, final int targetLine) {
    if (node.getType() == TokenTypes.BLOCK_COMMENT_BEGIN) {
      final int commentLine = node.getLineNo();
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
