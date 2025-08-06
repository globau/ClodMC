package au.com.glob.checks;

/*
 * SPDX-FileCopyrightText: Copyright (c) 2011-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 * https://github.com/yegor256/qulice/
 *
 * Modified for ClodMC
 */

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtil;
import org.jspecify.annotations.NullMarked;

/** Checks that non static method must contain at least one reference to `this`. */
@NullMarked
public final class NonStaticMethodCheck extends AbstractCheck {
  @Override
  public int[] getDefaultTokens() {
    return new int[] {
      TokenTypes.METHOD_DEF,
    };
  }

  @Override
  public int[] getAcceptableTokens() {
    return this.getDefaultTokens();
  }

  @Override
  public int[] getRequiredTokens() {
    return this.getDefaultTokens();
  }

  @Override
  public void visitToken(final DetailAST ast) {
    if (TokenTypes.CLASS_DEF == ast.getParent().getParent().getType()) {
      this.checkClassMethod(ast);
    }
  }

  private void checkClassMethod(final DetailAST method) {
    final DetailAST modifiers = method.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null) {
      return;
    }
    if (!AnnotationUtil.containsAnnotation(method, "Override")
        && !AnnotationUtil.containsAnnotation(method, "EventHandler")
        && !CheckUtils.isInAbstractOrNativeMethod(method)
        && !CheckUtils.branchContains(method, TokenTypes.LITERAL_THIS)) {
      final int line = method.getLineNo();
      this.log(line, "This method must be static, because it does not refer to \"this\"");
    }
  }
}
