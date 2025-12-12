package com.example.edog.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class CozeAPI {

    // 请确保 Token 和 BotID 正确
    private static final String COZE_API_TOKEN = "sat_LeDY8iu23Ifcb2UwY7LXfZeL0HhoF4NTswQmlooFVJyRJNd7ExEk9gFogjnRPbPl";
    private static final String BOT_ID = "7534905232841785370";

    /**
     * 调用 Coze 接口
     */
    public String[] CozeRequest(String question, String voiceId, Double speedRatio, boolean stream) {
        
        try {
            RestTemplate restTemplate = createUtf8RestTemplate();
            String url = "https://api.coze.cn/v3/chat";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(COZE_API_TOKEN);

            ObjectMapper mapper = new ObjectMapper();

            // 1. 构造输入参数对象 (对应 JSON 结构)
            Map<String, Object> inputPayload = new HashMap<>();
            inputPayload.put("input", question); 
            inputPayload.put("voice_id", (voiceId != null && !voiceId.isEmpty()) ? voiceId : "7568423452617523254");
            inputPayload.put("speed_ratio", speedRatio != null ? speedRatio : 1.0);

            // 2. 将参数对象转为 JSON 字符串
            String contentJsonString = mapper.writeValueAsString(inputPayload);
            System.out.println("[CozeAPI] 发送 Content: " + contentJsonString);

            // 3. 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("bot_id", BOT_ID);
            requestBody.put("user_id", "user_123");
            requestBody.put("stream", stream);
            requestBody.put("auto_save_history", true);

            List<Map<String, Object>> additional_messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentJsonString); 
            userMessage.put("content_type", "text");
            additional_messages.add(userMessage);
            
            requestBody.put("additional_messages", additional_messages);

            // 序列化整个请求体
            String finalRequestBodyJson = mapper.writeValueAsString(requestBody);
            HttpEntity<String> requestEntity = new HttpEntity<>(finalRequestBodyJson, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            String responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK) {
                // 简单检查业务错误码
                if (responseBody != null && responseBody.contains("\"code\":") && !responseBody.contains("\"code\":0")) {
                    System.err.println("[CozeAPI] 业务报错: " + responseBody);
                    return new String[]{"", "Bot配置错误或参数解析失败"};
                }

                if (stream) {
                    return processStreamResponse(responseBody);
                } else {
                    return processNonStreamResponse(responseBody);
                }
            } else {
                System.err.println("[CozeAPI] HTTP 请求失败: " + response.getStatusCode());
                return new String[]{ "", "请求失败" };
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{ "", "系统异常" };
        }
    }

    private RestTemplate createUtf8RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().removeIf(converter ->
                converter instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    /**
     * 处理流式响应（SSE格式）
     * 🔥 核心修复：正确解析 SSE 协议的 event 行，过滤 completed 事件防止重复
     */
    private String[] processStreamResponse(String streamData) {
        if (streamData == null || streamData.isEmpty()) {
            return new String[]{ "", "" };
        }

        StringBuilder result = new StringBuilder();
        String audio_url = "";
        String[] lines = streamData.split("\n");
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 状态机变量：记录当前正在处理的事件类型
        String currentEvent = "";

        for (String line : lines) {
            line = line.trim(); // 去除回车符等空白
            
            // 1. 捕获 event 行
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
                continue;
            }
            
            // 2. 处理 data 行
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                
                if (data.equals("[DONE]") || data.isEmpty()) continue;

                // 🔥 核心逻辑：如果当前事件是 'completed' (全量消息)，直接忽略
                // 因为我们已经通过 'delta' (增量消息) 拼接出了完整内容
                // 如果不忽略，内容就会重复一遍
                if ("conversation.message.completed".equals(currentEvent) || 
                    "conversation.chat.completed".equals(currentEvent)) {
                    continue;
                }

                try {
                    JsonNode jsonNode = objectMapper.readTree(data);

                    // 1. 尝试从 message 结构解析 (标准 V3)
                    if (jsonNode.has("message")) {
                        JsonNode msg = jsonNode.get("message");
                        if (msg.has("content") && "assistant".equals(msg.path("role").asText()) && "answer".equals(msg.path("type").asText())) {
                            String content = msg.get("content").asText();
                            // 检查是否为音频链接
                            if (content.startsWith("https://") && (content.contains(".mp3") || content.contains(".wav"))) {
                                if (audio_url.isEmpty()) { // 防止 URL 重复赋值
                                    audio_url = content;
                                    System.out.println("[CozeAPI] 解析到音频URL: " + audio_url);
                                }
                            } else {
                                result.append(content);
                            }
                        }
                    }
                    // 2. 兼容旧版结构 (Content 直接在根节点)
                    else if (jsonNode.has("content")) {
                        // 额外检查：确保是 answer 类型
                        String type = jsonNode.path("type").asText("answer");
                        if ("answer".equals(type)) {
                            String content = jsonNode.get("content").asText();
                            if (content.startsWith("https://") && (content.contains(".mp3") || content.contains(".wav"))) {
                                if (audio_url.isEmpty()) {
                                    audio_url = content;
                                    System.out.println("[CozeAPI] 解析到音频URL: " + audio_url);
                                }
                            } else {
                                result.append(content);
                            }
                        }
                    }

                } catch (Exception e) {
                    // 忽略解析错误 (可能是心跳包等非 JSON 数据)
                }
            }
        }
        
        String finalContent = result.toString().trim();
        System.out.println("[CozeAPI] 最终解析文本: " + finalContent);
        return new String[]{ audio_url, finalContent };
    }

    /**
     * 处理非流式响应 (JSON格式)
     */
    private String[] processNonStreamResponse(String jsonBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonBody);
            
            StringBuilder text = new StringBuilder();
            String audio_url = "";

            if (root.has("data")) {
                for (JsonNode item : root.get("data")) {
                    if ("assistant".equals(item.path("role").asText()) && "answer".equals(item.path("type").asText())) {
                        String content = item.path("content").asText();
                        if (content.startsWith("https://") && (content.contains(".mp3") || content.contains(".wav"))) {
                            audio_url = content;
                        } else {
                            text.append(content);
                        }
                    }
                }
            }

            return new String[]{ audio_url, text.toString() };
        } catch (Exception e) {
            System.err.println("[CozeAPI] 非流式解析失败: " + e.getMessage());
            return new String[]{ "", "" };
        }
    }

    public String downloadAudio(String audioUrl, String saveDir) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return null;
        }

        try {
            File dir = new File(saveDir);
            if (!dir.exists()) dir.mkdirs();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "coze_audio_" + timestamp + ".mp3";
            File outputFile = new File(dir, fileName);

            URL url = new URL(audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    System.out.println("[CozeAPI] 音频下载完成: " + outputFile.getAbsolutePath());
                    return outputFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            System.err.println("[CozeAPI] 下载音频失败: " + e.getMessage());
        }
        return null;
    }
    
    public byte[] downloadAudioBytes(String audioUrl) {
        return null; 
    }
}