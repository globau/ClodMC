package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** checkstyle check that enforces event handler naming conventions. */
@NullMarked
public class EventHandlerNamingCheck extends AbstractCheck {

  @Override
  public int[] getDefaultTokens() {
    return new int[] {TokenTypes.METHOD_DEF};
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
    if (!this.hasEventHandlerAnnotation(ast)) {
      return;
    }

    String methodName = this.getMethodName(ast);
    if (methodName == null) {
      return;
    }

    DetailAST parameters = ast.findFirstToken(TokenTypes.PARAMETERS);
    if (parameters == null) {
      return;
    }

    // should have exactly one parameter
    DetailAST firstParam = parameters.findFirstToken(TokenTypes.PARAMETER_DEF);
    if (firstParam == null) {
      return;
    }

    DetailAST secondParam = firstParam.getNextSibling();
    while (secondParam != null && secondParam.getType() != TokenTypes.PARAMETER_DEF) {
      secondParam = secondParam.getNextSibling();
    }
    if (secondParam != null) {
      // more than one parameter
      return;
    }

    String paramName = this.getParameterName(firstParam);
    String paramType = this.getParameterType(firstParam);

    if (paramName == null || paramType == null) {
      return;
    }

    // check parameter name is 'event'
    if (!"event".equals(paramName)) {
      this.log(ast, "event parameter must be named 'event', found: " + paramName);
      return;
    }

    // check parameter type ends with 'Event'
    if (!paramType.endsWith("Event")) {
      this.log(ast, "event parameter type must end with 'Event', found: " + paramType);
      return;
    }

    // check method name follows pattern on{EventType}
    String eventType = paramType.substring(0, paramType.length() - 5); // remove "Event"
    String expectedMethodName = "on" + eventType;

    if (!expectedMethodName.equals(methodName)) {
      this.log(
          ast,
          "event handler method name should be '" + expectedMethodName + "', found: " + methodName);
    }
  }

  private boolean hasEventHandlerAnnotation(DetailAST methodDef) {
    DetailAST modifiers = methodDef.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null) {
      return false;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    while (annotation != null) {
      DetailAST annotationName = annotation.findFirstToken(TokenTypes.IDENT);
      if (annotationName != null && "EventHandler".equals(annotationName.getText())) {
        return true;
      }
      do {
        annotation = annotation.getNextSibling();
      } while (annotation != null && annotation.getType() != TokenTypes.ANNOTATION);
    }
    return false;
  }

  private @Nullable String getMethodName(DetailAST methodDef) {
    DetailAST nameNode = methodDef.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : null;
  }

  private @Nullable String getParameterName(DetailAST paramDef) {
    DetailAST nameNode = paramDef.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : null;
  }

  private @Nullable String getParameterType(DetailAST paramDef) {
    DetailAST typeNode = paramDef.findFirstToken(TokenTypes.TYPE);
    if (typeNode == null) {
      return null;
    }

    // handle simple type (IDENT)
    DetailAST identNode = typeNode.findFirstToken(TokenTypes.IDENT);
    if (identNode != null) {
      return identNode.getText();
    }

    // handle qualified type (DOT)
    DetailAST dotNode = typeNode.findFirstToken(TokenTypes.DOT);
    if (dotNode != null) {
      return this.getQualifiedTypeName(dotNode);
    }

    return null;
  }

  private String getQualifiedTypeName(DetailAST dotNode) {
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
