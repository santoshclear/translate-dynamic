package in.acc.translate_dynamic.controller;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

@RestController
public class TranslationController {
    @PostMapping("/translate")
    public void translateDocx(@RequestParam("file") MultipartFile file, @RequestParam("targetLang") String targetLang) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String originalText = paragraph.getText();
                String translatedText = translateTextUsingPython(originalText, "en", targetLang);
                paragraph.getRuns().forEach(run -> run.setText(translatedText, 0));
            }

            try (FileOutputStream out = new FileOutputStream("translated.docx")) {
                document.write(out);
            }
        }
    }

    private String translateTextUsingPython(String text, String srcLang, String tgtLang) throws IOException {
        // Get the script path from resources
        //String scriptPath = new ClassPathResource("python/translate.py").getFile().getAbsolutePath();
        final String PYTHON_PATH = "python"; // or the path to your Python interpreter
        final String SCRIPT_PATH = "C:\\Users\\s.kumar.valaboju\\code\\dev\\translate-dynamic\\src\\main\\python\\translate.py"; // Absolute path
        //ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_PATH, scriptPath, text);
        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_PATH, SCRIPT_PATH, text);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String translatedText = reader.readLine();
        process.destroy();
        return translatedText;
        /*StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        process.destroy();
        return result.toString();*/
    }
}
