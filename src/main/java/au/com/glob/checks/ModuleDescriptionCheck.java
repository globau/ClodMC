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

      if (!this.implementsModule(ast)) {
        return;
      }

      if (!this.hasJavadocComment(ast)) {
        String className = this.getClassName(ast);
        this.log(ast, "module class '" + className + "' is missing javadoc description");
      }
    }
  }

  private boolean implementsModule(DetailAST classDef) {
    DetailAST implementsClause = classDef.findFirstToken(TokenTypes.IMPLEMENTS_CLAUSE);
    if (implementsClause == null) {
      return false;
    }

    DetailAST child = implementsClause.getFirstChild();
    while (child != null) {
      if (child.getType() == TokenTypes.IDENT && "Module".equals(child.getText())) {
        return true;
      }
      // check for qualified names like some.package.Module
      if (child.getType() == TokenTypes.DOT) {
        String qualifiedName = this.getQualifiedName(child);
        if (qualifiedName.endsWith(".Module") || "Module".equals(qualifiedName)) {
          return true;
        }
      }
      child = child.getNextSibling();
    }

    return false;
  }

  private boolean hasJavadocComment(DetailAST classDef) {
    int classLineNo = classDef.getLineNo();

    // traverse the entire file tree to find block comments near this class
    DetailAST root = classDef;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return this.findCommentNearLine(root, classLineNo);
  }

  private boolean findCommentNearLine(DetailAST node, int targetLine) {
    if (node.getType() == TokenTypes.BLOCK_COMMENT_BEGIN) {
      int commentLine = node.getLineNo();
      if (commentLine < targetLine && (targetLine - commentLine) <= 3) {
        return true;
      }
    }

    DetailAST child = node.getFirstChild();
    while (child != null) {
      if (this.findCommentNearLine(child, targetLine)) {
        return true;
      }
      child = child.getNextSibling();
    }

    return false;
  }

  private String getClassName(DetailAST classDef) {
    DetailAST nameNode = classDef.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : "unknown";
  }

  private String getQualifiedName(DetailAST dotNode) {
    StringBuilder sb = new StringBuilder();
    this.buildQualifiedName(dotNode, sb);
    return sb.toString();
  }

  private void buildQualifiedName(DetailAST node, StringBuilder sb) {
    if (node.getType() == TokenTypes.IDENT) {
      sb.append(node.getText());
    } else if (node.getType() == TokenTypes.DOT) {
      DetailAST left = node.getFirstChild();
      DetailAST right = left.getNextSibling();
      this.buildQualifiedName(left, sb);
      sb.append('.');
      this.buildQualifiedName(right, sb);
    }
  }
}
