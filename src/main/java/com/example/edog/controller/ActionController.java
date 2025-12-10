package com.example.edog.controller;

import com.example.edog.utils.GetIotToken;
import com.example.edog.utils.IotHttpSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActionController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET /action
     * 请求体: {"action_content": 1}
     * 接收 action_content，如果为 1 则发送 "trot" 到 IoT 平台。
     */
    @GetMapping(value = "/action", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveAction(@RequestBody(required = false) String body) {
        try {
            if (body == null || body.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing body");
            }
            JsonNode node = objectMapper.readTree(body);
            if (node == null || !node.has("action_content")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing action_content");
            }
            
            int actionContent = node.get("action_content").asInt();
            System.out.println("收到消息: " + actionContent);

            String messageToSend = null;
            if (actionContent == 1) {
                messageToSend = "trot_coze";
            } else if (actionContent == 2) {
                messageToSend = "trot_back_coze";
            }
            else if(actionContent == 3){
                messageToSend = "turn_left_coze";
            }
            else if(actionContent == 4){
                messageToSend = "turn_right_coze";
            }
            else{
                messageToSend = "stop_coze";
            }

            if (messageToSend != null) {
                String token = GetIotToken.getToken();
                if (token != null) {
                    try {
                        IotHttpSender.sendMessage(token, messageToSend);
                        System.out.println("向设备发送: " + messageToSend);
                    } catch (Exception e) {
                        System.err.println("发送失败，尝试刷新 Token 重试...");
                        GetIotToken.refreshToken();
                        token = GetIotToken.getToken();
                        if (token != null) {
                            try {
                                IotHttpSender.sendMessage(token, messageToSend);
                                System.out.println("重试发送成功: " + messageToSend);
                            } catch (Exception ex) {
                                System.err.println("重试发送失败: " + ex.getMessage());
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message after retry");
                            }
                        } else {
                            System.err.println("刷新 Token 失败");
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to refresh IoT Token");
                        }
                    }
                } else {
                    System.err.println("获取 Token 失败");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get IoT Token");
                }
            }

            return ResponseEntity.ok("received: " + actionContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error: " + e.getMessage());
        }
    }
}
