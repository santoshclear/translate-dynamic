package in.acc.translate_dynamic.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TranslationService {
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

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

        List<String> translatedTexts = texts.stream()
                .map(text -> {
                    try {
                        String translated = translateTextUsingPython(text, "en", targetLang);
                        System.out.println("Original text: " + text + ", Translated text: " + translated);
                        return translated;

                    } catch (IOException e) {
                        logger.error("Error translating text: {}", e.getMessage());
                        return text; // Return the original text in case of an error
                    }
                })
                .collect(Collectors.toList());
        System.out.println("Page translation completed");

        logger.info("Source language is changing to {}", targetLang);
        String srcLang = targetLang; // Set selected target language as source language
        logger.info("Source language is now set to {}", srcLang);
        return translatedTexts;
    }


    public void translateDocx(MultipartFile file, String targetLang) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String originalText = paragraph.getText();
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);

                List<XWPFRun> runs = paragraph.getRuns();
                if (runs != null) {
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

            try (FileOutputStream out = new FileOutputStream("translated.docx")) {
                document.write(out);
            }
        }
    }

    public void translatePdf(MultipartFile file, String targetLang) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDDocument translatedDocument = new PDDocument();
            PDType0Font font = PDType0Font.load(translatedDocument, new File("C:\\Users\\sai.sree.gudikandula\\OneDrive - Accenture\\Desktop\\python\\Noto_Sans\\static\\NotoSans_SemiCondensed-SemiBoldItalic.ttf"));

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);

            for (int page = 1; page <= document.getNumberOfPages(); ++page) {
                PDPage originalPage = document.getPage(page - 1);
                PDPage translatedPage = new PDPage(originalPage.getMediaBox());
                translatedDocument.addPage(translatedPage);

                textStripper.setStartPage(page);
                textStripper.setEndPage(page);

                String originalText = textStripper.getText(document);
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);

                PDPageContentStream contentStream = new PDPageContentStream(translatedDocument, translatedPage, PDPageContentStream.AppendMode.APPEND, true, true);
                contentStream.setFont(font, 12);

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                Rectangle2D region = new Rectangle2D.Double(0, 0, originalPage.getMediaBox().getWidth(), originalPage.getMediaBox().getHeight());
                stripper.addRegion("region", region);
                stripper.extractRegions(originalPage);

                String regionText = stripper.getTextForRegion("region");
                String[] lines = regionText.split("\n");

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);

                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                }

                contentStream.endText();
                contentStream.close();

                PDResources pageResources = originalPage.getResources();
                for (COSName xObjectName : pageResources.getXObjectNames()) {
                    PDXObject xObject = pageResources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        PDPageContentStream imageContentStream = new PDPageContentStream(translatedDocument, translatedPage, PDPageContentStream.AppendMode.APPEND, true, true);
                        imageContentStream.drawImage(image, 50, 700, image.getWidth(), image.getHeight());
                        imageContentStream.close();
                    }
                }
            }

            translatedDocument.save("translated.pdf");
            translatedDocument.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String translateTextUsingPython(String text, String srcLang, String tgtLang) throws IOException {
        final String PYTHON_PATH = "python";
        final String SCRIPT_PATH = "C:\\Users\\sai.sree.gudikandula\\OneDrive - Accenture\\Desktop\\New folder (2)\\translate-dynamic\\src\\main\\python\\translate.py";

        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_PATH, SCRIPT_PATH, text, srcLang, tgtLang);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } finally {
            process.destroy();
        }
    }
}
