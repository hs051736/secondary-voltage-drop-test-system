package com.hspyx.controller;

import com.hspyx.pojo.RecordDTO;
import com.hspyx.pojo.Result;
import com.hspyx.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/recognize")
    public Result recognizeImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp)$")) {
            return Result.error("仅支持 jpg、jpeg、png、bmp 格式的图片");
        }

        try {
            RecordDTO dto = ocrService.recognizeImage(file);
            return Result.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("OCR识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/auto-recognize")
    public Result autoRecognize(@RequestBody java.util.Map<String, String> request) {
        try {
            String imageBase64 = request.get("imageBase64");
            RecordDTO dto;
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                dto = ocrService.recognizeFromBase64(imageBase64);
            } else {
                dto = ocrService.autoRecognizeFromUrl();
            }
            return Result.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("自动识别失败: " + e.getMessage());
        }
    }

    @GetMapping("/get-image")
    public Result getImageFromService() {
        try {
            String imageBase64 = ocrService.getImageBase64FromUrl();
            return Result.success("data:image/jpeg;base64," + imageBase64);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取图片失败: " + e.getMessage());
        }
    }
}
