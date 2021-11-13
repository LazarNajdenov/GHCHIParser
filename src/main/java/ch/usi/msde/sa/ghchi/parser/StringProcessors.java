package ch.usi.msde.sa.ghchi.parser;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class StringProcessors {

    // TODO: 13.11.21
    //  Instead of using String.replaceAll,
    //  consider compiling the regex patterns only once
    //  and then calling Matcher.replaceAll

    /**
     * Removes all content between {@code <pre>} tags.
     * @see <a href="https://www.debuggex.com/r/gjOLqSaYf1muJHlk">Regex visualisation</a>
     */
    private static final UnaryOperator<String> REMOVE_PREFORMATTED_TEXT =
            text -> text.replaceAll("(?:\\s\\*\\s)*<pre>[\\s\\S]*?</pre>", " ");

    /**
     * Extracts the reference target names contained within Javadoc references.
     * @see <a href="https://www.debuggex.com/r/3HXy0yv7hLKJpKS4">Regex visualisation</a>
     */
    private static final UnaryOperator<String> EXTRACT_REFERENCE_CONTENT =
            text -> text.replaceAll("\\{@(?:value|link(?:plain)?)\\s(?:.*?#)?(\\w+).*?}", "$1");

    /**
     * Extracts the value contained within the <code>code</code>, <code>literal</code> and <code>serial</code> tags.
     * @see <a href="https://www.debuggex.com/r/fl-hkGAGwiZHsNiJ">Regex visualisation</a>
     */
    private static final UnaryOperator<String> EXTRACT_LITERAL_CONTENT =
            text -> text.replaceAll("\\{@(?:code|literal|serial(?:Data|Field)?|docRoot|inheritDoc)\\s?([^}]*)}", "$1");

    /**
     * Removes all HTML tags in a String.
     * @see <a href="https://www.debuggex.com/r/_gjB-CYKny3xrWl9">Regex visualisation</a>
     */
    private static final UnaryOperator<String> REMOVE_HTML_TAGS = text -> text.replaceAll("<[^>]*>", " ");

    /**
     * Removes all Javadoc metadata information from a string.
     * <br>
     * This includes the likes of {@code @param, @return, @throws,} etc.
     * @see <a href="https://www.debuggex.com/r/mZdM-K4StPe2QuO-">Regex visualisation</a>
     */
    private static final UnaryOperator<String> REMOVE_METADATA = text -> text.split("\\*\\s*@.*")[0];

    /**
     * Removes all <code>/**</code>, <code>*</code> and <code>*&#47;</code> characters from a String.
     * @see <a href="https://www.debuggex.com/r/jqdIQyAOtZRs8NbF">Regex visualisation</a>
     */
    private static final UnaryOperator<String> REMOVE_FORMATTING = text -> text.replaceAll("(?:/\\*)?\\*\\s?/?", " ");

    /**
     * Removes all non-ASCII characters from a String.
     */
    private static final UnaryOperator<String> RETAIN_ASCII = text -> text.replaceAll("[^\\p{ASCII}]", "");

    /**
     * Extracts the fist sentence from a String.
     * <br>
     * Although not exactly a perfect sentence matcher, it is simple and serves us well.
     */
    private static final UnaryOperator<String> GET_FIRST_SENTENCE = text -> text.split("[.!?]")[0];

    /**
     * Removes all punctuation symbols from a String.
     */
    private static final UnaryOperator<String> REMOVE_PUNCTUATION = text -> text.replaceAll("[^A-Za-z0-9\\s]", "");

    /**
     * @see StringUtils#normalizeSpace(String)
     */
    private static final UnaryOperator<String> NORMALIZE_SPACE = StringUtils::normalizeSpace;

    /**
     * @see String#trim()
     */
    private static final UnaryOperator<String> TRIM = String::trim;

    /**
     * Adds a separator tag to a String.
     * <br>
     * If the String is empty replace it with <code>null</code> instead.
     */
    private static final UnaryOperator<String> ADD_SEPARATOR = text -> (!text.isEmpty()) ? text + " <SEP> " : null;

    private static final Function<String, String> JAVADOC_STRING_PROCESSOR =
            REMOVE_PREFORMATTED_TEXT
                    .andThen(EXTRACT_REFERENCE_CONTENT)
                    .andThen(EXTRACT_LITERAL_CONTENT)
                    .andThen(REMOVE_HTML_TAGS)
                    .andThen(REMOVE_METADATA)
                    .andThen(REMOVE_FORMATTING)
                    .andThen(RETAIN_ASCII)
                    .andThen(GET_FIRST_SENTENCE)
                    .andThen(REMOVE_PUNCTUATION)
                    .andThen(NORMALIZE_SPACE)
                    .andThen(TRIM)
                    .andThen(ADD_SEPARATOR);

    private static final Function<String, String> METHOD_STRING_PROCESSOR = RETAIN_ASCII.andThen(NORMALIZE_SPACE);

    public static String processJavadocString(String text) {
        return JAVADOC_STRING_PROCESSOR.apply(text);
    }

    public static String processMethodString(String text) {
        return METHOD_STRING_PROCESSOR.apply(text);
    }
}
