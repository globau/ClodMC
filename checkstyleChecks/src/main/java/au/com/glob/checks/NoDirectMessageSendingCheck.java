package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that forbids direct calls to sendMessage/sendRichMessage */
@NullMarked
public class NoDirectMessageSendingCheck extends AbstractCheck {

  private boolean fileContainsCommandSenderImpl = false;

  @Override
  public int[] getDefaultTokens() {
    return new int[] {TokenTypes.METHOD_CALL, TokenTypes.CLASS_DEF};
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
    this.fileContainsCommandSenderImpl = false;
  }

  @Override
  public void visitToken(DetailAST ast) {
    // skip CommandSender implementors
    if (ast.getType() == TokenTypes.CLASS_DEF) {
      if (CheckUtils.classImplements(ast, "CommandSender")) {
        this.fileContainsCommandSenderImpl = true;
      }
      return;
    }
    if (this.fileContainsCommandSenderImpl) {
      return;
    }

    if (CheckUtils.getRelativeFilename(this.getFilePath())
        .equals("src/main/java/au/com/glob/clodmc/util/Chat.java")) {
      return;
    }

    if (ast.getType() != TokenTypes.METHOD_CALL) {
      return;
    }
    DetailAST dot = ast.findFirstToken(TokenTypes.DOT);
    if (dot == null) {
      return;
    }

    DetailAST receiver = dot.getFirstChild();
    DetailAST method = receiver != null ? receiver.getNextSibling() : null;
    if (method == null || method.getType() != TokenTypes.IDENT) {
      return;
    }

    String methodName = method.getText();
    if ("sendMessage".equals(methodName) || "sendRichMessage".equals(methodName)) {
      this.log(
          ast,
          "direct calls to %s are forbidden, use Chat utility methods instead"
              .formatted(methodName));
    }
  }
}
