package com.hspyx.service.impl;

import com.hspyx.pojo.RecordDTO;
import com.hspyx.service.OcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class OcrServiceImpl implements OcrService {

    private static final DateTimeFormatter INPUT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUTPUT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 百度OCR API的配置
    private static final String API_KEY = "xhOLopUe7gj84Jyu5rUdFoHz";
    private static final String SECRET_KEY = "18CGFlE9kU0NJqatOcS1PdnXp8S5EhCg";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    private String accessToken = null;
    private long tokenExpireTime = 0;

    /**
     * 识别上传的图片文件中的文本
     */
    @Override
    public RecordDTO recognizeImage(MultipartFile file) throws Exception {
        try {
            String accessToken = getAccessToken();

            // 将文件转为Base64编码
            byte[] imageBytes = file.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            // 构建请求体
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("access_token", accessToken)
                    .add("image", imageBase64)
                    .add("language_type", "CHN_ENG")
                    .build();

            Request request = new Request.Builder()
                    .url(OCR_URL)
                    .post(body)
                    .build();

            // 发送请求并处理响应
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OCR请求失败: " + response);
                }

                String responseBody = response.body().string();
                System.out.println("百度OCR响应: " + responseBody);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                if (rootNode.has("error_code")) {
                    throw new RuntimeException("百度OCR错误: " + rootNode.get("error_msg").asText());
                }

                // 解析百度返回的JSON结果，提取出所有文字并拼接
                StringBuilder ocrText = new StringBuilder();
                JsonNode wordsResult = rootNode.get("words_result");
                if (wordsResult != null && wordsResult.isArray()) {
                    for (JsonNode wordNode : wordsResult) {
                        ocrText.append(wordNode.get("words").asText()).append("\n");
                    }
                }

                String text = ocrText.toString();
                System.out.println("OCR识别结果：\n" + text);

                return parseOcrText(text);
            }
        } catch (Exception e) {
            System.err.println("OCR识别异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 自动从URL获取图片并识别文本（已被禁用）
     */
    @Override
    public RecordDTO autoRecognizeFromUrl() throws Exception {
        String imageUrl = "http://127.0.0.1:8000";
        System.out.println("正在从 " + imageUrl + " 获取图片...");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("获取图片失败: " + response);
            }

            byte[] imageBytes = response.body().bytes();
            System.out.println("获取图片成功，图片大小: " + imageBytes.length + " bytes");

            String accessToken = getAccessToken();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            RequestBody body = new FormBody.Builder()
                    .add("access_token", accessToken)
                    .add("image", imageBase64)
                    .add("language_type", "CHN_ENG")
                    .build();

            Request ocrRequest = new Request.Builder()
                    .url(OCR_URL)
                    .post(body)
                    .build();

            try (Response ocrResponse = client.newCall(ocrRequest).execute()) {
                if (!ocrResponse.isSuccessful()) {
                    throw new RuntimeException("OCR请求失败: " + ocrResponse);
                }

                String responseBody = ocrResponse.body().string();
                System.out.println("百度OCR响应: " + responseBody);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                if (rootNode.has("error_code")) {
                    throw new RuntimeException("百度OCR错误: " + rootNode.get("error_msg").asText());
                }

                StringBuilder ocrText = new StringBuilder();
                JsonNode wordsResult = rootNode.get("words_result");
                if (wordsResult != null && wordsResult.isArray()) {
                    for (JsonNode wordNode : wordsResult) {
                        ocrText.append(wordNode.get("words").asText()).append("\n");
                    }
                }

                String text = ocrText.toString();
                System.out.println("OCR识别结果：\n" + text);

                return parseOcrText(text);
            }
        }
    }


    /**
     * 从URL获取图片的Base64编码字符串
     */
    @Override
    public String getImageBase64FromUrl() throws Exception {
        String imageUrl = "http://127.0.0.1:8000";
        System.out.println("正在从 " + imageUrl + " 获取图片...");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("获取图片失败: " + response);
            }

            // 获取图片的二进制数据
            byte[] imageBytes = response.body().bytes();
            System.out.println("获取图片成功，图片大小: " + imageBytes.length + " bytes");

            // 返回图片数据的Base64编码字符串
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }


    /**
     * 从Base64编码的图片字符串中识别文本
     */
    @Override
    public RecordDTO recognizeFromBase64(String base64Image) throws Exception {
        try {
            // 从缓存或获取新的AccessToken
            String accessToken = getAccessToken();

            OkHttpClient client = new OkHttpClient();

            RequestBody body = new FormBody.Builder()
                    .add("access_token", accessToken)
                    .add("image", base64Image)
                    .add("language_type", "CHN_ENG")
                    .build();

            // 构建OCR请求
            Request request = new Request.Builder()
                    .url(OCR_URL)
                    .post(body)
                    .build();

            // 执行OCR请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OCR请求失败: " + response);
                }

                String responseBody = response.body().string();
                System.out.println("百度OCR响应: " + responseBody);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                if (rootNode.has("error_code")) {
                    throw new RuntimeException("百度OCR错误: " + rootNode.get("error_msg").asText());
                }

                
                StringBuilder ocrText = new StringBuilder();
                JsonNode wordsResult = rootNode.get("words_result");
                if (wordsResult != null && wordsResult.isArray()) {
                    for (JsonNode wordNode : wordsResult) {
                        ocrText.append(wordNode.get("words").asText()).append("\n");
                    }
                }

                String text = ocrText.toString();
                System.out.println("OCR识别结果：\n" + text);

                // 解析OCR文本并返回RecordDTO
                return parseOcrText(text);
            }
        } catch (Exception e) {
            System.err.println("OCR识别异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 从缓存或获取新的AccessToken
     */
    private synchronized String getAccessToken() throws Exception {

        // 检查缓存的AccessToken是否有效
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        // 构建获取AccessToken的URL
        String authUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials" +
                "&client_id=" + API_KEY +
                "&client_secret=" + SECRET_KEY;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(authUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("获取百度AccessToken失败: " + response);
            }

            String responseBody = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            if (rootNode.has("error")) {
                throw new RuntimeException("获取AccessToken错误: " + rootNode.get("error_description").asText());
            }

            accessToken = rootNode.get("access_token").asText();
            int expiresIn = rootNode.get("expires_in").asInt();
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

            System.out.println("获取到百度AccessToken: " + accessToken);
            return accessToken;
        }
    }

        
    /**
     * 使用正则表达式解析OCR文本并返回RecordDTO
     */
    private RecordDTO parseOcrText(String text) {
        RecordDTO dto = new RecordDTO();
        RecordDTO.DeviceInfo deviceInfo = new RecordDTO.DeviceInfo();
        RecordDTO.TestResult testResult = new RecordDTO.TestResult();
        Map<String, RecordDTO.PhaseData> phaseResults = new HashMap<>();

        String[] lines = text.split("\n");

        // 将所有行合并为一个字符串，方便正则匹配
        String combinedText = text.replace("\n", " ");

        // 提取计量点编号
        Pattern productPattern = Pattern.compile("(计量点编号|产品编号)\\s*([0-9]+)");
        Matcher productMatcher = productPattern.matcher(combinedText);
        if (productMatcher.find()) {
            deviceInfo.productNo = productMatcher.group(2);
        }

        // 提取测试日期 - 支持7位或8位日期（如2023114或20231114）
        Pattern datePattern = Pattern.compile("(测试日期|日期)[\\s　]*(\\d{7,8})");
        Matcher dateMatcher = datePattern.matcher(combinedText);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(2);
            System.out.println("DEBUG 日期匹配到: " + dateStr);
            // 补齐为8位
            if (dateStr.length() == 7) {
                dateStr = "0" + dateStr;
            }
            try {
                LocalDate date = LocalDate.parse(dateStr, INPUT_DATE_FMT);
                deviceInfo.testDate = date.format(OUTPUT_DATE_FMT);
            } catch (Exception e) {
                deviceInfo.testDate = dateStr;
            }
        }

        // 提取二次电压 - 从"次电压"所在行提取数字
        Pattern voltagePattern = Pattern.compile("(?:次电压|二次电压)[^0-9]*([0-9]+\\.?[0-9]*)\\s*[Uu]?", Pattern.CASE_INSENSITIVE);
        Matcher voltageMatcher = voltagePattern.matcher(combinedText);
        if (voltageMatcher.find()) {
            testResult.secondaryVoltage = voltageMatcher.group(1);
        }

        // 提取温度 - 处理"温 度 27.4C"这种分开的情况，以及全角字符
        Pattern tempPattern = Pattern.compile("温[\\s　]*度[\\s　]*\\d+\\.?\\d*\\s*[°]?[CcC℃]?", Pattern.CASE_INSENSITIVE);
        Matcher tempMatcher = tempPattern.matcher(combinedText);
        if (tempMatcher.find()) {
            String matched = tempMatcher.group();
            Pattern numPattern = Pattern.compile("\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) {
                testResult.temperature = numMatcher.group();
            }
        }

        // 提取湿度 - 处理"湿 度 20.0%"这种分开的情况
        Pattern humidityPattern = Pattern.compile("湿[\\s　]*度[\\s　]*\\d+\\.?\\d*\\s*%?", Pattern.CASE_INSENSITIVE);
        Matcher humidityMatcher = humidityPattern.matcher(combinedText);
        if (humidityMatcher.find()) {
            String matched = humidityMatcher.group();
            Pattern numPattern = Pattern.compile("\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) {
                testResult.humidity = numMatcher.group();
            }
        }

        // 提取三相数据 - 使用更灵活的模式
        RecordDTO.PhaseData ao = new RecordDTO.PhaseData();
        RecordDTO.PhaseData bo = new RecordDTO.PhaseData();
        RecordDTO.PhaseData co = new RecordDTO.PhaseData();

        // 匹配 f(%) 数据行 - 支持多种OCR识别变体
        // 支持: f(%) / f(6) / f%) / f(% / f（%） 等所有变体
        // 核心思路：找到 f% 或 f6 模式，然后提取其后3个数值
        Pattern fPattern = Pattern.compile("f\\s*[（(]?\\s*[%％6６]", Pattern.CASE_INSENSITIVE);
        Matcher fMatcher = fPattern.matcher(combinedText);
        if (fMatcher.find()) {
            // 找到位置后，提取其后跟的三个数值
            int endPos = fMatcher.end();
            String afterMatch = combinedText.substring(endPos);
            // 提取3个数值
            Pattern numPattern = Pattern.compile("[－\\-]?\\s*\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(afterMatch);
            if (numMatcher.find()) ao.f = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) bo.f = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) co.f = normalizeNumber(numMatcher.group());
            System.out.println("DEBUG f匹配成功: ao.f=" + ao.f + ", bo.f=" + bo.f + ", co.f=" + co.f);
        }

        // 匹配 d(分) 数据行 - 多种可能格式
        // 支持: d分) / d分 / d( / d（分） 等变体
        Pattern dPattern = Pattern.compile("d[\\s　]*[(\\[（]?\\s*[分芬分钟fFenF]+[\\s　]*[)\\]）]?\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*", Pattern.CASE_INSENSITIVE);
        Matcher dMatcher = dPattern.matcher(combinedText);
        if (dMatcher.find()) {
            String matched = dMatcher.group();
            Pattern numPattern = Pattern.compile("[－\\-]?\\s*\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) ao.d = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) bo.d = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) co.d = normalizeNumber(numMatcher.group());
        }

        // 匹配 dU(%) 数据行 - 支持多种OCR识别变体
        Pattern duPattern = Pattern.compile("dU[\\s　]*[(\\[（]\\s*[%％]\\s*[)\\]）]\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*", Pattern.CASE_INSENSITIVE);
        Matcher duMatcher = duPattern.matcher(combinedText);
        if (duMatcher.find()) {
            String matched = duMatcher.group();
            Pattern numPattern = Pattern.compile("[－\\-]?\\s*\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) ao.dU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) bo.dU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) co.dU = normalizeNumber(numMatcher.group());
        }

        // 匹配 Upt:U 数据行 - 支持多种OCR识别变体
        Pattern uptuPattern = Pattern.compile("Upt[\\s　:：]*U\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*", Pattern.CASE_INSENSITIVE);
        Matcher uptuMatcher = uptuPattern.matcher(combinedText);
        if (uptuMatcher.find()) {
            String matched = uptuMatcher.group();
            Pattern numPattern = Pattern.compile("[－\\-]?\\s*\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) ao.uptU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) bo.uptU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) co.uptU = normalizeNumber(numMatcher.group());
        }

        // 匹配 Uyb:U 数据行 - 支持多种OCR识别变体
        Pattern uybuPattern = Pattern.compile("Uyb[\\s　:：]*U\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*\\s*[－\\-]?\\s*\\d+\\.?\\d*", Pattern.CASE_INSENSITIVE);
        Matcher uybuMatcher = uybuPattern.matcher(combinedText);
        if (uybuMatcher.find()) {
            String matched = uybuMatcher.group();
            Pattern numPattern = Pattern.compile("[－\\-]?\\s*\\d+\\.?\\d*");
            Matcher numMatcher = numPattern.matcher(matched);
            if (numMatcher.find()) ao.uybU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) bo.uybU = normalizeNumber(numMatcher.group());
            if (numMatcher.find()) co.uybU = normalizeNumber(numMatcher.group());
        }

        phaseResults.put("ao", ao);
        phaseResults.put("bo", bo);
        phaseResults.put("co", co);

        testResult.phaseResults = phaseResults;
        testResult.resultStatus = "合格";

        System.out.println("DEBUG 最终解析结果:");
        System.out.println("  deviceInfo.productNo: " + deviceInfo.productNo);
        System.out.println("  deviceInfo.testDate: " + deviceInfo.testDate);
        System.out.println("  secondaryVoltage: " + testResult.secondaryVoltage);
        System.out.println("  temperature: " + testResult.temperature);
        System.out.println("  humidity: " + testResult.humidity);
        System.out.println("  ao.f=" + ao.f + ", bo.f=" + bo.f + ", co.f=" + co.f);
        System.out.println("  ao.d=" + ao.d + ", bo.d=" + bo.d + ", co.d=" + co.d);
        System.out.println("  ao.dU=" + ao.dU + ", bo.dU=" + bo.dU + ", co.dU=" + co.dU);

        dto.setDeviceInfo(deviceInfo);
        dto.setTestResult(testResult);
        dto.setTestData(null);

        return dto;
    }

    // 标准化数字：将全角负号"－"转换为半角负号"-"
    private String normalizeNumber(String num) {
        if (num == null || num.isEmpty()) {
            return num;
        }
        return num.replace('－', '-');
    }
}
