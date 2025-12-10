package com.example.edog.utils;

import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CozeAPI {

    public String[] CozeRequest(String question, Boolean stream) {
        List<Map<String, Object>> additional_messages = new ArrayList<>();
        Map<String, Object> additional_messages_body = new HashMap<>();
        additional_messages_body.put("content", question);
        additional_messages_body.put("content_type", "text");
        additional_messages_body.put("role", "user");
        additional_messages_body.put("type", "question");
        additional_messages.add(additional_messages_body);

        try {
            // 1. 创建自定义配置的 RestTemplate
            RestTemplate restTemplate = createUtf8RestTemplate();

            // 2. 设置请求 URL
            String url = "https://api.coze.cn/v3/chat";

            // 3. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth("sat_LeDY8iu23Ifcb2UwY7LXfZeL0HhoF4NTswQmlooFVJyRJNd7ExEk9gFogjnRPbPl");

            // 4. 设置请求体参数
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("bot_id", "7534905232841785370");
            requestBody.put("user_id", "123456789");
            requestBody.put("stream", stream);
            requestBody.put("additional_messages", additional_messages);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 5. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                System.out.println("请求成功！");

                // 处理流式响应（如果是stream模式）
                if (stream != null && stream) {
                    return processStreamResponse(responseBody);
                } else {
                    return new String[]{ responseBody };
                }
            } else {
                return new String[]{ "请求失败，状态码: " + response.getStatusCode() };
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new String[]{ "请求异常: " + e.getMessage() };
        }
    }

    /**
     * 创建支持UTF-8编码的RestTemplate
     */
    private RestTemplate createUtf8RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 移除默认的String转换器，添加UTF-8编码的转换器
        restTemplate.getMessageConverters().removeIf(converter ->
                converter instanceof StringHttpMessageConverter);

        // 添加UTF-8编码的String转换器
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
    }

    /**
     * 处理流式响应（SSE格式）
     */
    private String[] processStreamResponse(String streamData) throws Exception {
        if (streamData == null || streamData.isEmpty()) {
            return new String[]{ "无响应数据" };
        }

        StringBuilder result = new StringBuilder();
        String[] lines = streamData.split("\n");
        String audio_url = "";//返回的音频的url

        // 处理每条消息
        for (String line : lines) {
            // 获取data内容
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();//这里的trim是为了删除空格
                if (!data.equals("[DONE]") && !data.isEmpty()) {
                    try {
                        // 检查数据中是否包含"answer"类型且有created_at（完整消息）
                        if (data.contains("\"type\":\"answer\"") && data.contains("\"created_at\"")) {
                            // 使用Jackson解析JSON
                            ObjectMapper objectMapper = new ObjectMapper();
                            JsonNode jsonNode = objectMapper.readTree(data);

                            // 提取content字段
                            if (jsonNode.has("content")) {
                                String content = jsonNode.get("content").asText();
                                
                                // 检查 content 是否是 URL (音频链接)
                                if (content.startsWith("https://") && content.contains(".mp3")) { 
                                    audio_url = content;
                                    System.out.println("解析到音频URL: " + audio_url);
                                }
                                // 普通文本回复
                                else if (!content.startsWith("https://")) {
                                    result.append(content);
                                    System.out.println("解析到文本: " + content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // JSON解析失败，跳过
                        System.err.println("JSON解析失败: " + e.getMessage());
                    }
                }
            }
        }

        /*
        * 返回一个字符串数组，第一个元素是音频的url，第二个元素是文本结果
        */
        return new String[]{ audio_url, result.toString() };
    }

    /**
     * 从URL下载音频文件
     * @param audioUrl 音频URL
     * @param saveDir 保存目录
     * @return 保存的文件路径，失败返回null
     */
    public String downloadAudio(String audioUrl, String saveDir) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            System.err.println("音频URL为空");
            return null;
        }

        try {
            // 创建保存目录
            File dir = new File(saveDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "coze_audio_" + timestamp + ".mp3";
            File outputFile = new File(dir, fileName);

            // 下载文件
            URL url = new URL(audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    
                    System.out.println("音频下载完成: " + outputFile.getAbsolutePath() + " (" + totalBytes / 1024 + " KB)");
                    return outputFile.getAbsolutePath();
                }
            } else {
                System.err.println("下载失败，HTTP状态码: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            System.err.println("下载音频失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从URL下载音频并返回字节数组（用于直接发送给设备）
     * @param audioUrl 音频URL
     * @return 音频字节数组，失败返回null
     */
    public byte[] downloadAudioBytes(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return null;
        }

        try {
            URL url = new URL(audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    byte[] audioData = outputStream.toByteArray();
                    System.out.println("音频下载完成: " + audioData.length / 1024 + " KB");
                    return audioData;
                }
            }
        } catch (Exception e) {
            System.err.println("下载音频失败: " + e.getMessage());
        }
        return null;
    }
}