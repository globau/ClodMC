package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that forbids inner classes, records, enums, interfaces, and annotations. */
@NullMarked
public class NoInnerTypesCheck extends AbstractCheck {

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
      TokenTypes.CLASS_DEF,
      TokenTypes.INTERFACE_DEF,
      TokenTypes.ENUM_DEF,
      TokenTypes.RECORD_DEF,
      TokenTypes.ANNOTATION_DEF
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
  public void visitToken(final DetailAST ast) {
    DetailAST parent = ast.getParent();
    while (parent != null) {
      if (parent.getType() == TokenTypes.CLASS_DEF
          || parent.getType() == TokenTypes.INTERFACE_DEF
          || parent.getType() == TokenTypes.ENUM_DEF
          || parent.getType() == TokenTypes.RECORD_DEF
          || parent.getType() == TokenTypes.ANNOTATION_DEF) {

        final String typeName = CheckUtils.getTypeName(ast);
        final String typeKind = CheckUtils.getTypeKind(ast.getType());

        this.log(
            ast,
            "inner.%s.forbidden".formatted(typeKind.toLowerCase(Locale.ROOT)),
            "%s '%s' is defined inside another type".formatted(typeKind, typeName));
        break;
      }
      parent = parent.getParent();
    }
  }
}
