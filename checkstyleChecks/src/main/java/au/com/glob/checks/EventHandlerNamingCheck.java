package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;

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
    if (CheckUtils.getAnnotation(ast, "EventHandler") == null) {
      return;
    }

    String methodName = CheckUtils.getName(ast);
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
      return;
    }

    String paramName = CheckUtils.getName(firstParam);
    String paramType = CheckUtils.getParameterType(firstParam);
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
}
