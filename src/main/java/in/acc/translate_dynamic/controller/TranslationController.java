package in.acc.translate_dynamic.controller;

import in.acc.translate_dynamic.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class TranslationController {


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
    public void translateDocx(@RequestParam("file") MultipartFile file,
                              @RequestParam("targetLang") String targetLang) throws IOException {
        translationService.translateDocx(file, targetLang);
    }

    @PostMapping("/translate-pdf")
    public void translatePdf(@RequestParam("file") MultipartFile file,
                             @RequestParam("targetLang") String targetLang) throws IOException {
        translationService.translatePdf(file, targetLang);
    }
}
