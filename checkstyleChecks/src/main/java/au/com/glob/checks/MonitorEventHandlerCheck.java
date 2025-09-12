package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** checkstyle check that prevents setter method calls in MONITOR priority event handlers. */
@NullMarked
public class MonitorEventHandlerCheck extends AbstractCheck {

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
  public void visitToken(final DetailAST ast) {
    if (!this.hasEventHandlerAnnotation(ast)) {
      return;
    }

    // if it has EventHandler annotation, check if it's MONITOR priority
    if (!this.hasMonitorPriority(ast)) {
      return;
    }

    final String eventParamName = this.getEventParameterName(ast);
    if (eventParamName == null) {
      return;
    }

    // check method body for setter calls on the event parameter
    final DetailAST methodBody = ast.findFirstToken(TokenTypes.SLIST);
    if (methodBody != null) {
      this.checkForSetterCalls(methodBody, eventParamName);
    }
  }

  private boolean hasEventHandlerAnnotation(final DetailAST methodDef) {
    final DetailAST modifiers = methodDef.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null) {
      return false;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    while (annotation != null) {
      final DetailAST annotationName = annotation.findFirstToken(TokenTypes.IDENT);
      if (annotationName != null && "EventHandler".equals(annotationName.getText())) {
        return true;
      }
      do {
        annotation = annotation.getNextSibling();
      } while (annotation != null && annotation.getType() != TokenTypes.ANNOTATION);
    }
    return false;
  }

  private boolean hasMonitorPriority(final DetailAST methodDef) {
    final DetailAST modifiers = methodDef.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers == null) {
      return false;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    while (annotation != null) {
      final DetailAST annotationName = annotation.findFirstToken(TokenTypes.IDENT);
      if (annotationName != null && "EventHandler".equals(annotationName.getText())) {
        return this.annotationHasMonitorPriority(annotation);
      }
      do {
        annotation = annotation.getNextSibling();
      } while (annotation != null && annotation.getType() != TokenTypes.ANNOTATION);
    }
    return false;
  }

  private boolean annotationHasMonitorPriority(final DetailAST annotation) {
    // simplified approach: search for "MONITOR" text anywhere in the annotation
    return this.containsMonitorText(annotation);
  }

  private boolean containsMonitorText(final DetailAST node) {
    // check if this node contains "MONITOR" text
    if (node.getType() == TokenTypes.IDENT && "MONITOR".equals(node.getText())) {
      return true;
    }

    // check children recursively
    DetailAST child = node.getFirstChild();
    while (child != null) {
      if (this.containsMonitorText(child)) {
        return true;
      }
      child = child.getNextSibling();
    }

    return false;
  }

  private @Nullable String getEventParameterName(final DetailAST methodDef) {
    final DetailAST parameters = methodDef.findFirstToken(TokenTypes.PARAMETERS);
    if (parameters == null) {
      return null;
    }

    final DetailAST firstParam = parameters.findFirstToken(TokenTypes.PARAMETER_DEF);
    if (firstParam == null) {
      return null;
    }

    final DetailAST nameNode = firstParam.findFirstToken(TokenTypes.IDENT);
    return nameNode != null ? nameNode.getText() : null;
  }

  private void checkForSetterCalls(final DetailAST node, final String eventParamName) {
    if (node.getType() == TokenTypes.METHOD_CALL) {
      this.checkMethodCall(node, eventParamName);
    }

    // recursively check all children
    DetailAST child = node.getFirstChild();
    while (child != null) {
      this.checkForSetterCalls(child, eventParamName);
      child = child.getNextSibling();
    }
  }

  private void checkMethodCall(final DetailAST methodCall, final String eventParamName) {
    final DetailAST dot = methodCall.findFirstToken(TokenTypes.DOT);
    if (dot == null) {
      return;
    }

    final DetailAST receiver = dot.getFirstChild();
    final DetailAST methodName = receiver != null ? receiver.getNextSibling() : null;

    // check if the receiver is the event parameter
    if (receiver != null
        && receiver.getType() == TokenTypes.IDENT
        && eventParamName.equals(receiver.getText())) {

      // check if the method name starts with "set"
      if (methodName != null && methodName.getType() == TokenTypes.IDENT) {
        final String method = methodName.getText();
        if (method.startsWith("set")
            && method.length() > 3
            && Character.isUpperCase(method.charAt(3))) {
          this.log(
              methodCall,
              "MONITOR priority event handlers cannot call setter methods on the event parameter: %s"
                  .formatted(method));
        }
      }
    }
  }
}
