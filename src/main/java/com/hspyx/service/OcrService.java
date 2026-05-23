package com.hspyx.service;

import com.hspyx.pojo.RecordDTO;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * 识别上传的图片文件中的文本
     */
    RecordDTO recognizeImage(MultipartFile file) throws Exception;

    /**
     * 自动识别URL中的图片文本
     */
    RecordDTO autoRecognizeFromUrl() throws Exception;

    /**
     * 从URL获取图片的Base64编码
     */
    String getImageBase64FromUrl() throws Exception;

    /**
     * 从Base64编码的图片文本中识别文本
     */
    RecordDTO recognizeFromBase64(String base64Image) throws Exception;
}
