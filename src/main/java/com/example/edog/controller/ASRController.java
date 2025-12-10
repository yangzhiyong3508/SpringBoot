package com.example.edog.controller;

import com.example.edog.service.ASRService;
import com.example.edog.utils.BaiduAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class ASRController {

    @GetMapping("/asr/start")
    public String startASR() {
        try {
            // 1. 获取 AccessToken
            String token = BaiduAuthService.getAccessToken();

            // 2. 构造 WebSocket URL
            String url = "wss://vop.baidu.com/realtime_asr?sn=123456&token=" + token + "&dev_pid=15372";

            // 3. 建立 WebSocket 连接
            ASRService client = new ASRService(new URI(url));
            client.connectBlocking();  // 只连接一次

            // 4. 读取本地测试音频（16kHz 16bit PCM）
            byte[] audioData = Files.readAllBytes(Paths.get("D:/Web_Package/Backend/EDOG/src/pcm/16K16bit.pcm"));

            // 5. 发送音频
            client.sendAudioInChunks(audioData);

            return "音频已发送，等待识别结果...";
        } catch (Exception e) {
            e.printStackTrace();
            return "出错: " + e.getMessage();
        }
    }
}

