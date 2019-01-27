package io.github.sidmishraw.blanka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sidmishraw.blanka.handler.BlankaContentHandler;
import io.github.sidmishraw.blanka.model.TextModel;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Blanka is an application that uses Apache Tika parsers to parse through PDF documents and converts them into JSON documents.
 *
 * @author sidmishraw
 * @version 1.0.0
 * @since 1.0.0
 */
public class Blanka {

    public static void main(String[] args) {

        if (args.length < 2) {

            // Error if the source PDF path is not given.
            //
            System.out.println("Please enter the source PDF directory path!");
            System.out.println("blanka <the-source-pdf-path> <the-output-directory-path> [flags]..]");
            System.out.println("By default all flags are off.");
            System.out.println("Current supported flags: ");
            System.out.println("  " + BlankaContentHandler.REMOVE_PUNCTUATION + "- if you want to remove the punctuations with the words: comma, semi-colon, period, etc.");
            System.out.println();
            System.exit(1);
        }

        try {

            List<String> flags = new ArrayList<>();

            if (args.length > 2) {

                for (int i = 2; i < args.length; i++) {

                    flags.add(args[i]);
                }
            }

            processSourcePDFs(args[0], args[1], flags.toArray(new String[0]));
        } catch (IOException e) {

            System.out.println("Failed to process the PDFs - ERR-001");

            e.printStackTrace();
        } catch (Exception e) {

            System.out.println("Failed to process the PDFs - ERR-002");

            e.printStackTrace();
        }
    }

    /**
     * Processes the source PDFs given the input and output paths, and the flags for processing.
     *
     * @param strSourcePDFPath     the source path
     * @param strOutputJSONDirPath the destination path
     * @param flags                the list of processing paths.
     * @throws IOException if anything goes wrong while processing the PDFs.
     */
    private static void processSourcePDFs(String strSourcePDFPath, String strOutputJSONDirPath, String... flags) throws IOException {

        Path sourcePDFPath = Path.of(strSourcePDFPath);

        Path outputJSONDirPath = Path.of(strOutputJSONDirPath);

        if (Files.isDirectory(sourcePDFPath)) {

            processPDFSourceDirectory(sourcePDFPath, outputJSONDirPath, flags);
        } else if (Files.isRegularFile(sourcePDFPath)) {

            processSourcePDF(sourcePDFPath, outputJSONDirPath, flags);
        } else {

            System.out.println("Not a valid file -- do nothing!");
        }
    }


    /**
     * Processes a PDF file given its file path, the destination path, and processing flags.
     *
     * @param sourcePDfFilePath the source PDF path
     * @param outputJSONDirPath the destination path
     * @param flags             the list of processing flags
     * @throws IOException if processing goes wrong.
     */
    private static void processSourcePDF(Path sourcePDfFilePath, Path outputJSONDirPath, String... flags) throws IOException {

        File sourcePDF = sourcePDfFilePath.toFile();

        // Create the output directory if it doesn't exist.
        //
        if (!Files.isDirectory(outputJSONDirPath)) {

            Files.createDirectory(outputJSONDirPath);
        }

        String pdfFileName = sourcePDfFilePath.getFileName().toString();

        Metadata metadata = new Metadata();

        ParseContext context = new ParseContext();

        Parser parser = new PDFParser();

        PDFParserConfig pdfParserConfig = new PDFParserConfig();

        pdfParserConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);

        pdfParserConfig.setEnableAutoSpace(true);

//        pdfParserConfig.setSpacingTolerance(0.33f); // default is 0.5f, going a little less since this is an academic doc.

        ((PDFParser) parser).setPDFParserConfig(pdfParserConfig);

        try (InputStream iostream = new FileInputStream(sourcePDF)) {

            TextModel textModel = TextModel.create(pdfFileName, new ArrayList<>());

            // the content handler is used for parsing the PDF and converting to blanka's required output JSON format.
            //
            ContentHandler xhtmlContentHandler = new XHTMLContentHandler(BlankaContentHandler.with(textModel, flags), metadata);

            parser.parse(iostream, xhtmlContentHandler, metadata, context);

            Path outputJSONFilePath = outputJSONDirPath.resolve(pdfFileName + ".json");

            ObjectMapper objectMapper = new ObjectMapper();

            String outputJSONString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(textModel);

            writeToFile(outputJSONFilePath, outputJSONString);
        } catch (TikaException | SAXException | IOException e) {

            e.printStackTrace();
        }
    }


    /**
     * Processes the source PDFs given their directory, output directory, and processing flags.
     *
     * @param sourcePDFDirectoryPath the input source PDF directory
     * @param outputJSONDirPath      the output JSON directory path
     * @param flags                  the processing flags
     * @throws IOException if the processing fails.
     */
    private static void processPDFSourceDirectory(Path sourcePDFDirectoryPath, Path outputJSONDirPath, String... flags) throws IOException {

        // @formatter:off
        List<Optional<IOException>> exs =
                Files.list(sourcePDFDirectoryPath)
                        .filter(f -> f.toAbsolutePath().getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .map(f -> {

                            try {

                                processSourcePDF(f, outputJSONDirPath, flags);

                                return Optional.<IOException>empty();
                            } catch (IOException e) {

                                return Optional.of(e);
                            }
                        })
                        .collect(Collectors.toList());
        // @formatter:on

        for (Optional<IOException> ex : exs) {

            if (ex.isPresent()) {

                throw ex.get();
            }
        }
    }

    private static void writeToFile(Path outputJSONFilePath, String outputJSONString) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(outputJSONFilePath)) {

            writer.write(outputJSONString);
        }
    }
}
