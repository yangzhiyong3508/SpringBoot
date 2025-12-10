package com.example.edog.utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class IotHttpSender {

    // 和你 ArkTS CommonConstant 完全对应
    private static final String ENDPOINT =
            "https://587a77885a.st1.iotda-app.cn-east-3.myhuaweicloud.com";

    private static final String PROJECT_ID =
            "1b4fbda3bbc2472f9c497507034f79f2";

    private static final String DEVICE_ID =
            "692bdc8d46c60374e3f8eadc_myedog";

    /**
     * 通过 x-auth-token 方式发送消息（等价 ArkTS）
     */
    public static String sendMessage(String token, String message) {

        // URL
        String url = ENDPOINT + "/v5/iot/" + PROJECT_ID +
                "/devices/" + DEVICE_ID + "/messages";

        // Header（完全等价 ArkTS）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-auth-token", token);

        // Body（完全等价 encoding: none）
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("encoding", "none");

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        // 发送
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        // IoTDA 成功返回码：201
        if (response.getStatusCode() == HttpStatus.CREATED) {
            return response.getBody();
        } else {
            throw new RuntimeException(
                "IoTDA 发送失败: " + response.getStatusCode() + " - " + response.getBody()
            );
        }
    }
}