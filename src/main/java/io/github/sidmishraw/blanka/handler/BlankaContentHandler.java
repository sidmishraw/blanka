package io.github.sidmishraw.blanka.handler;

import com.rometools.utils.Strings;
import io.github.sidmishraw.blanka.model.TextModel;
import lombok.Data;
import lombok.NonNull;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * The content handler for Blanka, it is used for custom PDF content processing.
 *
 * @author sidmishraw
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class BlankaContentHandler implements ContentHandler {

    public static final String REMOVE_PUNCTUATION = "remove-punctuation";

    /**
     * Creates a new instance of the {@link BlankaContentHandler}.
     *
     * @param textModel the text model.
     * @return the content handler.
     */
    public static BlankaContentHandler with(TextModel textModel, String... flags) {

        BlankaContentHandler blankaContentHandler = new BlankaContentHandler();

        blankaContentHandler.setTextModel(textModel);

        blankaContentHandler.addFlags(flags);

        return blankaContentHandler;
    }


    /**
     * Removes any trailing commas, semi-colons, periods etc.
     *
     * @param text the dirty text.
     * @return the cleansed text.
     */
    private static String cleanseText(String text) {

        if (Objects.isNull(text) || Strings.isEmpty(text)) {

            return text;
        }

        String cleansedText = text;

        if (!Character.isLetterOrDigit(text.charAt(0))) {

            cleansedText = text.length() > 1 ? text.substring(1) : "";
        }

        if (!Character.isLetterOrDigit(text.charAt(text.length() - 1))) {

            cleansedText = text.length() > 1 ? text.substring(0, text.length() - 1) : "";
        }

        return cleansedText;
    }


    /**
     * Recursively cleanses the text.
     *
     * @param dirtyText    the dirty text from previous round
     * @param cleansedText the cleansed text from previous round
     * @return the cleansed text
     */
    private static String cleanseText(String dirtyText, String cleansedText) {

        if (dirtyText.equals(cleansedText)) {

            return cleansedText;
        }

        return cleanseText(cleansedText, cleanseText(cleansedText));
    }


    /**
     * Recursively cleanses the dirty text.
     *
     * @param dirtyText the dirty text.
     * @return the cleansed text.
     */
    private static String rcleanseText(@NonNull String dirtyText) {

        return cleanseText(dirtyText, cleanseText(dirtyText));
    }


    /**
     * the PDF text model.
     */
    private TextModel textModel;

    /**
     * the page counter.
     */
    private int pageCounter = -1;

    /**
     * for holding the words being parsed out.
     */
    private final Set<String> words = new LinkedHashSet<>();

    /**
     * the string buffer for the content handler.
     */
    private final StringBuffer buffer = new StringBuffer();

    /**
     * The processing flags.
     */
    private final List<String> flags = new ArrayList<>();


    /**
     * Adds the processing flags to the processor.
     *
     * @param flags the processing flags.
     */
    public void addFlags(String... flags) {

        Optional.ofNullable(flags).ifPresent(fs -> Collections.addAll(this.flags, fs));
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        if ("div".equalsIgnoreCase(localName)) {

            // When the first `div` is encountered, it signals a new page, hence the page is incremented.
            //
            pageCounter++;

            // clear the words so that the round of extraction can begin for the current page.
            //
            this.words.clear();
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        for (int i = start; i < length; i++) {

            // @formatter:off
            if (Character.isWhitespace(ch[i])) {
            // @formatter:on

                String word = this.buffer.toString();

                if (this.flags.contains(REMOVE_PUNCTUATION)) {

                    // cleanse the text of preceeding and trailing comma, semi-colon, colon, and periods recursively
                    //

                    word = rcleanseText(this.buffer.toString());
                }

                word = Strings.trimToEmpty(word);

                if (Strings.isNotEmpty(word)) {

                    this.words.add(word);

                    this.buffer.setLength(0);

                    continue;
                }
            }

            this.buffer.append(ch[i]);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (localName.equalsIgnoreCase("div")) {

            this.textModel.addPage(this.pageCounter, new ArrayList<>(this.words));
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        //
        // NOP
        //
    }

    @Override
    public void startDocument() throws SAXException {

        this.pageCounter = 0; // signal start of document processing

        this.words.clear();

        this.buffer.setLength(0);
    }

    @Override
    public void endDocument() throws SAXException {

        this.pageCounter = -1; // signal end of document processing

        this.words.clear();

        this.buffer.setLength(0);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        //
        // NOP
        //
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        //
        // NOP
        //
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        //
        // NOP
        //
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        //
        // NOP
        //
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        //
        // NOP
        //
    }


}
