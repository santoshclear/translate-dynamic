package in.acc.translate_dynamic.controller;

import ch.qos.logback.core.model.Model;
import in.acc.translate_dynamic.service.TranslationService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Controller
public class TranslationController {
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    @Autowired
    private TranslationService translationService;

    @PostMapping("/translate-text")
    @ResponseBody
    public Map<String, String> translateText(@RequestBody Map<String, String> request) throws IOException {
        return translationService.translateText(request);
    }

    @PostMapping("/translate-page")
    @ResponseBody
    public List<String> translatePage(@RequestBody Map<String, Object> request) throws IOException {
        return translationService.translatePage(request);
    }

    @PostMapping("/translate-docx")
    @ResponseBody
    public void translateDocx(@RequestParam("file") MultipartFile file,
                              @RequestParam("targetLang") String targetLang) throws IOException {
        translationService.translateDocx(file, targetLang);
    }

    @PostMapping("/translate-pdf")
    @ResponseBody
    public void translatePdf(@RequestParam("file") MultipartFile file,
                             @RequestParam("targetLang") String targetLang) throws IOException {
        translationService.translatePdf(file, targetLang);
    }

    @PostMapping("/translate-document")
    public ResponseEntity<?> translateDocument(@RequestParam("file") MultipartFile file,
                                               @RequestParam("targetLang") String targetLang) throws IOException {
        logger.info("Received file: {}", file.getOriginalFilename());
        logger.info("Target language: {}", targetLang);

        String fileName = file.getOriginalFilename();
        byte[] translatedDocx = null;

        if (fileName != null && fileName.endsWith(".docx")) {
            translatedDocx = translationService.translateDocx(file, targetLang);
        } else if (fileName != null && fileName.endsWith(".pdf")) {
            // Assuming translationService.translatePdf returns byte[] for translated PDF
            translatedDocx = translationService.translatePdf(file, targetLang);
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }

        if (translatedDocx != null) {
            // Prepare the translated document as a resource for download
            ByteArrayResource resource = new ByteArrayResource(translatedDocx);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translated_document.docx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(translatedDocx.length)
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to translate the document");
        }
    }
}
