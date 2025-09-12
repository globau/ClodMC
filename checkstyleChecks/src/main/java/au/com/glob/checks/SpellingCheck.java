package au.com.glob.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/** checkstyle check that ensures comments and javadoc use proper spelling. */
@NullMarked
public class SpellingCheck extends AbstractCheck {

  private static final Map<String, String> US_TO_UK_SPELLINGS = createSpellingMap();

  private static Map<String, String> createSpellingMap() {
    final Map<String, String> wordMap = new HashMap<>();
    wordMap.put("color", "colour");
    wordMap.put("behavior", "behaviour");
    wordMap.put("organize", "organise");
    wordMap.put("initialize", "initialise");
    wordMap.put("analyze", "analyse");
    wordMap.put("optimize", "optimise");
    wordMap.put("customize", "customise");
    wordMap.put("finalize", "finalise");
    wordMap.put("realize", "realise");
    wordMap.put("recognize", "recognise");
    wordMap.put("apologize", "apologise");
    wordMap.put("criticize", "criticise");
    wordMap.put("emphasize", "emphasise");
    wordMap.put("maximize", "maximise");
    wordMap.put("minimize", "minimise");
    wordMap.put("normalize", "normalise");
    wordMap.put("prioritize", "prioritise");
    wordMap.put("categorize", "categorise");
    wordMap.put("serialization", "serialisation");
    wordMap.put("authorization", "authorisation");
    wordMap.put("localization", "localisation");
    wordMap.put("optimization", "optimisation");
    wordMap.put("organization", "organisation");
    wordMap.put("synchronization", "synchronisation");
    wordMap.put("center", "centre");
    wordMap.put("meter", "metre");
    wordMap.put("fiber", "fibre");
    wordMap.put("theater", "theatre");
    wordMap.put("defense", "defence");
    wordMap.put("offense", "offence");
    wordMap.put("license", "licence");
    wordMap.put("practice", "practise");
    wordMap.put("advisor", "adviser");
    wordMap.put("gray", "grey");
    wordMap.put("catalog", "catalogue");
    wordMap.put("dialog", "dialogue");
    wordMap.put("analog", "analogue");
    wordMap.put("program", "programme");
    wordMap.put("artifact", "artefact");
    wordMap.put("skeptical", "sceptical");
    return wordMap;
  }

  @Override
  public int[] getDefaultTokens() {
    return new int[] {TokenTypes.COMMENT_CONTENT};
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
    if (!CheckUtils.isRelativeTo(this.getFilePath(), "src/main/java/au/com/glob/clodmc")) {
      return;
    }

    final String comment = ast.getText();
    if (comment == null || comment.trim().isEmpty()) {
      return;
    }

    for (final Map.Entry<String, String> entry : US_TO_UK_SPELLINGS.entrySet()) {
      final String badSpelling = entry.getKey();
      final String goodSpelling = entry.getValue();

      final Pattern pattern =
          Pattern.compile("\\b" + Pattern.quote(badSpelling) + "\\b", Pattern.CASE_INSENSITIVE);
      final Matcher matcher = pattern.matcher(comment);

      if (matcher.find()) {
        this.log(ast, "use ''%s'' instead of ''%s''".formatted(goodSpelling, badSpelling));
      }
    }
  }
}
