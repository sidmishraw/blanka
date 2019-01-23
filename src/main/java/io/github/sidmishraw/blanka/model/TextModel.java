package io.github.sidmishraw.blanka.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The textual representation of a PDF that is parsed using Apache Tika.
 *
 * @author sidmishraw
 * @version 1.0.0
 * @since 1.0.0
 */
@NoArgsConstructor
@Data
@JsonRootName("pdf_document")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextModel {

    /**
     * Creates a TextModel.
     *
     * @param pdfFileName the PDF file name
     * @param pdfPages    the pages in the PDF
     * @return the TextModel.
     */
    @JsonCreator
    @Builder
    // @formatter:off
    public static TextModel create(
            @JsonProperty("pdf_file_name")          String                  pdfFileName,
            @JsonProperty("pages")                  List<PageModel>         pdfPages
    ) {
    // @formatter:on

        TextModel txtModel = new TextModel();

        txtModel.setPdfFileName(pdfFileName);

        txtModel.setPdfPages(pdfPages);

        return txtModel;
    }


    @JsonProperty("pdf_file_name")
    private String pdfFileName;

    @JsonProperty("pages")
    private List<PageModel> pdfPages = new ArrayList<>();


    /**
     * Adds a page to the text model.
     *
     * @param pageNumber the page number
     * @param words      the list of words to be added to the page
     */
    public void addPage(int pageNumber, List<String> words) {

        // @formatter:off
        PageModel page =
                PageModel
                    .builder()
                    .pageNumber(pageNumber)
                    .words(Optional.ofNullable(words).orElseGet(ArrayList::new))
                    .build();
        // @formatter:on

        this.pdfPages.add(page);
    }


    /**
     * Add the words to a particular page.
     *
     * @param pageNumber the page number -- 0 indexed.
     * @param words      the words to be added to the page.
     */
    public void addWordsToPage(int pageNumber, List<String> words) {

        // @formatter:off
        this.pdfPages
                .stream()
                .filter(pgs -> pgs.getPageNumber() == (pageNumber - 1))
                .distinct()
                .findAny()
                .ifPresent(PageModel::addWords);
        // @formatter:on
    }

}

/**
 * The model for a page in the PDF document. If contains the page index and the list of words that appear in the page. The ordering of the words is maintained.
 *
 * @author sidmishraw
 * @version 1.0.0
 * @since 1.0.0
 */
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class PageModel {


    /**
     * Creates the PageModel.
     *
     * @param pageNumber the page number
     * @param words      the words in a page
     * @return the page in a PDF document.
     */
    @JsonCreator
    @Builder
    // @formatter:off
    public static PageModel create(
            @JsonProperty("page_number")            Integer             pageNumber,
            @JsonProperty("words")                  List<String>        words
    ) {
    // @formatter:on
        PageModel pageModel = new PageModel();

        pageModel.setPageNumber(pageNumber);

        pageModel.setWords(words);

        return pageModel;
    }


    /**
     * the page number.
     */
    @JsonProperty("page_number")
    private Integer pageNumber;


    /**
     * the list of words in the page.
     */
    @JsonProperty("words")
    private List<String> words = new ArrayList<>();


    /**
     * Adds the words to this page model.
     *
     * @param words the list of words to be added to this page model.
     */
    public void addWords(String... words) {

        // @formatter:off
        Optional.ofNullable(words)
                .map(ws -> Arrays.asList(ws))
                .ifPresent(ws -> ws.forEach(this.words::add));
        // @formatter:on
    }


    /**
     * Adds the words to this page model.
     *
     * @param words the list of words to be added to this page model.
     */
    public void addWords(List<String> words) {

        // @formatter:off
        Optional.ofNullable(words)
                .ifPresent(ws -> ws.forEach(this.words::add));
        // @formatter:on
    }
}
