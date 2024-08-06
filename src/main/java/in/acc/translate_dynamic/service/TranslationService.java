package in.acc.translate_dynamic.service;

import org.apache.commons.io.IOUtils;
import org.apache.juli.logging.Log;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TranslationService {
    private String currentSrcLang = "en"; // Initialize to English

    //    private Log logger;
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    //translateText
    public Map<String, String> translateText(Map<String, String> request) throws IOException {

        logger.info("Translating text...");
        String text = request.get("text");
        String targetLang = request.get("targetLang");

        String translatedText = translateTextUsingPython(text, "en", targetLang);

        logger.info("Text translation completed.");
        return Map.of("translatedText", translatedText);
    }

    public List<String> translatePage(Map<String, Object> request) throws IOException {
        logger.info("Translating page...");
        List<String> texts = (List<String>) request.get("texts");
        String targetLang = (String) request.get("targetLang");

        // Use the current source language
        String srcLang = currentSrcLang;
        logger.info("Source language set to {}", srcLang);

        List<String> translatedTexts = texts.stream()
                .map(text -> {
                    try {
                        String translated = translateTextUsingPython(text, srcLang, targetLang);
                        System.out.println("Original text: " + text + ", Translated text: " + translated);
                        return translated;
                    } catch (IOException e) {
                        logger.error("Error translating text: {}", e.getMessage());
                        return text; // Return the original text in case of an error
                    }
                })
                .collect(Collectors.toList());
        System.out.println("Page translation completed");

        // Update the source language to the target language for future use
        currentSrcLang = targetLang;
        logger.info("Source language is now set to {}", currentSrcLang);

        return translatedTexts;
    }

    // Method to translate DOCX files
    public byte[] translateDocx(MultipartFile file, String targetLang) throws IOException {
        logger.info("Translating DOCX file to {}", targetLang);

        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String originalText = paragraph.getText();
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);

                List<XWPFRun> runs = paragraph.getRuns();
                if (runs != null) {
                    int startRun = 0;
                    int textIndex = 0;
                    for (XWPFRun run : runs) {
                        String runText = run.toString();
                        int runLength = runText.length();
                        if (textIndex + runLength > translatedText.length()) {
                            run.setText(translatedText.substring(textIndex), 0);
                            break;
                        } else {
                            run.setText(translatedText.substring(textIndex, textIndex + runLength), 0);
                            textIndex += runLength;
                        }
                    }
                }
            }

            // Convert the modified document to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }


    public byte[] translatePdf(MultipartFile file, String targetLang) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument originalDocument = PDDocument.load(inputStream)) {

            // Create a new PDF document for the translated content
            PDDocument translatedDocument = new PDDocument();

            // Define a font for the translated text
            PDType0Font font = PDType0Font.load(translatedDocument, new File("C:\\path\\to\\your\\font.ttf"));

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);

            // Iterate through each page in the original document
            for (int page = 1; page <= originalDocument.getNumberOfPages(); ++page) {
                PDPage originalPage = originalDocument.getPage(page - 1);
                PDPage translatedPage = new PDPage(originalPage.getMediaBox());
                translatedDocument.addPage(translatedPage);

                // Extract text from the current page
                textStripper.setStartPage(page);
                textStripper.setEndPage(page);
                String originalText = textStripper.getText(originalDocument);

                // Translate the extracted text
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);

                // Draw the translated text on the new page
                try (PDPageContentStream contentStream = new PDPageContentStream(translatedDocument, translatedPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.setFont(font, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, translatedPage.getMediaBox().getHeight() - 50);
                    for (String line : translatedText.split("\n")) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -15); // Adjust line spacing as needed
                    }
                    contentStream.endText();
                }

                // Handle images
                PDResources pageResources = originalPage.getResources();
                for (COSName xObjectName : pageResources.getXObjectNames()) {
                    PDXObject xObject = pageResources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        try (PDPageContentStream imageContentStream = new PDPageContentStream(translatedDocument, translatedPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                            imageContentStream.drawImage(image, 50, 50, image.getWidth(), image.getHeight());
                        }
                    }
                }
            }

            // Convert the translated document to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            translatedDocument.save(outputStream);
            translatedDocument.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Error translating PDF", e);
            throw new IOException("Error translating PDF", e);
        }
    }


    public String translateTextUsingPython(String text, String srcLang, String tgtLang) throws IOException {
        // Call the Python script with dynamic source and target languages
        ProcessBuilder pb = new ProcessBuilder("python", "C:\\Users\\s.kumar.valaboju\\code\\dev\\translate-1\\translate-dynamic\\src\\main\\python\\translate.py", text, srcLang, tgtLang);

        // Set the HF_HUB_DISABLE_SYMLINKS_WARNING environment variable
        Map<String, String> env = pb.environment();
        env.put("HF_HUB_DISABLE_SYMLINKS_WARNING", "1");

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.contains("UserWarning")) {  // Filter out UserWarning lines
                output.append(line);
            }
        }

        // Wait for the process to complete
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Translation process was interrupted", e);
        }

        return output.toString().trim();
    }

}