package in.acc.translate_dynamic.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TranslationController {

    @PostMapping("/translate-docx")
    public void translateDocx(@RequestParam("file") MultipartFile file,
                              @RequestParam("targetLang") String targetLang) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String originalText = paragraph.getText();
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);

                // Replace text in runs to maintain formatting
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

            // Save the translated document to a dynamic path or return as response
            try (FileOutputStream out = new FileOutputStream("translated.docx")) {
                document.write(out);
            }
        }
    }

    @PostMapping("/translate-pdf")
    public void translatePdf(@RequestParam("file") MultipartFile file,
                             @RequestParam("targetLang") String targetLang) throws IOException {
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

                // Using text extraction by area to preserve formatting
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                // Define the region you want to extract text from
                Rectangle2D region = new Rectangle2D.Double(0, 0, originalPage.getMediaBox().getWidth(), originalPage.getMediaBox().getHeight());
                stripper.addRegion("region", region);
                stripper.extractRegions(originalPage);

                String regionText = stripper.getTextForRegion("region");
                String[] lines = regionText.split("\n");

                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);

                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15); // Adjust the offset for each new line
                }

                contentStream.endText();
                contentStream.close();

                // Copying images and other graphics
                PDResources pageResources = originalPage.getResources();
                for (COSName xObjectName : pageResources.getXObjectNames()) {
                    PDXObject xObject = pageResources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        PDPageContentStream imageContentStream = new PDPageContentStream(translatedDocument, translatedPage, PDPageContentStream.AppendMode.APPEND, true, true);
                        imageContentStream.drawImage(image, image.getWidth(), image.getHeight());
                        imageContentStream.close();
                    }
                }
            }

            translatedDocument.save("translated.pdf");
            translatedDocument.close();
        }
    }

    private String translateTextUsingPython(String text, String srcLang, String tgtLang) throws IOException {
        final String PYTHON_PATH = "python"; // or the path to your Python interpreter
        final String SCRIPT_PATH = "C:\\Users\\sai.sree.gudikandula\\OneDrive - Accenture\\Desktop\\New folder (2)\\translate-dynamic\\src\\main\\python\\translate.py"; // Absolute path to your Python script

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
