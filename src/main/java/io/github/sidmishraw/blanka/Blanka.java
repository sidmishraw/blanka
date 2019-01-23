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

        if (args.length != 2) {

            // Error if the source PDF path is not given.
            //
            System.out.println("Please enter the source PDF directory path!");
            System.out.println("blanka <the-source-pdf-path> <the-output-directory-path>");
            System.exit(1);
        }

        try {

            processSourcePDFs(args[0], args[1]);
        } catch (IOException e) {

            System.out.println("Failed to process the PDFs - ERR-001");

            e.printStackTrace();
        } catch (Exception e) {

            System.out.println("Failed to process the PDFs - ERR-002");

            e.printStackTrace();
        }
    }

    private static void processSourcePDFs(String strSourcePDFPath, String strOutputJSONDirPath) throws IOException {

        Path sourcePDFPath = Path.of(strSourcePDFPath);

        Path outputJSONDirPath = Path.of(strOutputJSONDirPath);

        if (Files.isDirectory(sourcePDFPath)) {

            processPDFSourceDirectory(sourcePDFPath, outputJSONDirPath);
        } else if (Files.isRegularFile(sourcePDFPath)) {

            processSourcePDF(sourcePDFPath, outputJSONDirPath);
        } else {

            System.out.println("Not a valid file -- do nothing!");
        }
    }

    private static void processSourcePDF(Path sourcePDfFilePath, Path outputJSONDirPath) throws IOException {

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

            ContentHandler xhtmlContentHandler = new XHTMLContentHandler(BlankaContentHandler.with(textModel), metadata);

            parser.parse(iostream, xhtmlContentHandler, metadata, context);

            Path outputJSONFilePath = outputJSONDirPath.resolve(pdfFileName + ".json");

            ObjectMapper objectMapper = new ObjectMapper();

            String outputJSONString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(textModel);

            writeToFile(outputJSONFilePath, outputJSONString);
        } catch (TikaException | SAXException | IOException e) {

            e.printStackTrace();
        }
    }

    private static void processPDFSourceDirectory(Path sourcePDFDirectoryPath, Path outputJSONDirPath) throws IOException {

        // @formatter:off
        List<Optional<IOException>> exs =
                Files.list(sourcePDFDirectoryPath)
                        .filter(f -> f.toAbsolutePath().getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .map(f -> {

                            try {

                                processSourcePDF(f, outputJSONDirPath);

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
