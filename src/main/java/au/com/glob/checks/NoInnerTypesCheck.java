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
  public void visitToken(DetailAST ast) {
    DetailAST parent = ast.getParent();
    while (parent != null) {
      if (parent.getType() == TokenTypes.CLASS_DEF
          || parent.getType() == TokenTypes.INTERFACE_DEF
          || parent.getType() == TokenTypes.ENUM_DEF
          || parent.getType() == TokenTypes.RECORD_DEF
          || parent.getType() == TokenTypes.ANNOTATION_DEF) {

        String typeName = this.getTypeName(ast);
        String typeKind = this.getTypeKind(ast.getType());

        this.log(
            ast,
            "inner." + typeKind.toLowerCase(Locale.ROOT) + ".forbidden",
            typeKind + " '" + typeName + "' is defined inside another type");
        break;
      }
      parent = parent.getParent();
    }
  }

  private String getTypeName(DetailAST ast) {
    DetailAST nameNode = ast.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : "unknown";
  }

  private String getTypeKind(int tokenType) {
    return switch (tokenType) {
      case TokenTypes.CLASS_DEF -> "Class";
      case TokenTypes.INTERFACE_DEF -> "Interface";
      case TokenTypes.ENUM_DEF -> "Enum";
      case TokenTypes.RECORD_DEF -> "Record";
      case TokenTypes.ANNOTATION_DEF -> "Annotation";
      default -> "Type";
    };
  }
}
