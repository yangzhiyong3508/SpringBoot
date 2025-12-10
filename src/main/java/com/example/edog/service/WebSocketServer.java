package com.example.edog.service;

import com.example.edog.configurer.ASRClient;
import com.example.edog.utils.AudioConverter;
import com.example.edog.utils.BaiduAuthService;
import com.example.edog.utils.CozeAPI;
import com.example.edog.utils.WavFileUtil;
import io.github.jaredmdobson.concentus.OpusDecoder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketServer extends AbstractWebSocketHandler {

    private static final String RECORD_DIR = "recordings";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;

    /** 每个 session 一个 Opus 解码器 */
    private final Map<String, OpusDecoder> decoders = new ConcurrentHashMap<>();

    /** 解码后的 PCM 缓冲 */
    private final Map<String, ByteArrayOutputStream> pcmBuffers = new ConcurrentHashMap<>();

    /** 心跳 */
    private final Map<String, Timer> heartbeats = new ConcurrentHashMap<>();

    /** 每个 session 对应的百度 ASR 客户端 */
    private final Map<String, ASRClient> asrClients = new ConcurrentHashMap<>();

    /** Coze API 客户端 */
    private final CozeAPI cozeAPI = new CozeAPI();

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        String id = session.getId();
        System.out.println("ESP32 连接: " + id);

        decoders.put(id, new OpusDecoder(SAMPLE_RATE, CHANNELS));
        pcmBuffers.put(id, new ByteArrayOutputStream());

        // 初始化百度 ASR 客户端
        initASRClient(id, session);

        Timer hb = new Timer(true);
        hb.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new PingMessage());
                    } else {
                        cancelSession(id);
                    }
                } catch (Exception e) {
                    cancelSession(id);
                }
            }
        }, 5000, 5000);

        heartbeats.put(id, hb);
    }

    /**
     * 初始化百度 ASR 客户端
     */
    private void initASRClient(String sessionId, WebSocketSession espSession) {
        try {
            String token = BaiduAuthService.getAccessToken();
            ASRClient asrClient = new ASRClient(token);

            // 设置识别结果回调 - 将结果发送回 ESP32
            asrClient.setRecognitionCallback(result -> {
                // 过滤心跳消息
                if (result.contains("\"type\":\"HEARTBEAT\"")) {
                    return;
                }
                
                // 只处理最终识别结果 (FIN_TEXT)
                if (result.contains("\"type\":\"FIN_TEXT\"")) {
                    // 提取识别文本
                    int resultStart = result.indexOf("\"result\":\"");
                    if (resultStart != -1) {
                        resultStart += 10;
                        int resultEnd = result.indexOf("\"", resultStart);
                        if (resultEnd != -1) {
                            String text = result.substring(resultStart, resultEnd);
                            if (!text.isEmpty()) {
                                System.out.println("🎤 ASR识别结果: " + text);
                                
                                // 调用 Coze API 获取回复
                                sendToCozeAPI(text, espSession);
                            }
                        }
                    }
                } else if (result.contains("\"err_no\"") && !result.contains("\"err_no\":0")) {
                    // 输出错误信息（排除正常的 err_no:0）
                    System.err.println("ASR错误: " + result);
                }
            });

            // 连接百度 ASR
            asrClient.connectBlocking();
            asrClients.put(sessionId, asrClient);
            System.out.println("百度ASR客户端已连接 (session: " + sessionId + ")");

        } catch (Exception e) {
            System.err.println("初始化百度ASR客户端失败: " + e.getMessage());
        }
    }

    /**
     * 调用 Coze API 并将结果发送给 ESP32
     */
    private void sendToCozeAPI(String question, WebSocketSession espSession) {
        // 异步调用 Coze API，避免阻塞
        new Thread(() -> {
            try {
                System.out.println("发送到Coze: " + question);
                
                // 调用 Coze API (stream=true 获取流式响应)
                String[] response = cozeAPI.CozeRequest(question, true);
                
                if (response != null && response.length >= 2) {
                    String audioUrl = response[0];
                    String textResult = response[1];
                    
                    // 打印结果
                    System.out.println("音频URL: " + (audioUrl.isEmpty() ? "无" : audioUrl));
                    System.out.println("文本结果: " + textResult);
                    
                    // 下载并转换音频
                    String oggFilePath = null;
                    if (!audioUrl.isEmpty()) {
                        // 1. 下载 MP3
                        String mp3FilePath = cozeAPI.downloadAudio(audioUrl, "coze_audio");
                        
                        // 2. 转换为 OGG/Opus 格式 (设备端可接受的格式)
                        if (mp3FilePath != null) {
                            oggFilePath = AudioConverter.convertMp3ToOpusOgg(mp3FilePath);
                            if (oggFilePath != null) {
                                System.out.println("音频已转换为设备格式: " + oggFilePath);
                            }
                        }
                    }
                    
                    // 发送给 ESP32
                    if (espSession.isOpen()) {
                        if (oggFilePath != null) {
                            // 读取 OGG 文件并发送二进制数据给 ESP32
                            File oggFile = new File(oggFilePath);
                            if (oggFile.exists()) {
                                byte[] oggData = Files.readAllBytes(oggFile.toPath());
                                
                                // 先发送文本信息
                                String jsonResponse = String.format(
                                    "{\"type\":\"tts\",\"text\":\"%s\",\"audio_size\":%d}",
                                    textResult.replace("\"", "\\\"").replace("\n", "\\n"),
                                    oggData.length
                                );
                                espSession.sendMessage(new TextMessage(jsonResponse));
                                
                                // 再发送音频二进制数据
                                espSession.sendMessage(new BinaryMessage(oggData));
                                System.out.println("已发送音频到ESP32: " + oggData.length + " bytes");
                            }
                        } else {
                            // 没有音频，只发送文本
                            String jsonResponse = String.format(
                                "{\"type\":\"text\",\"text\":\"%s\"}",
                                textResult.replace("\"", "\\\"").replace("\n", "\\n")
                            );
                            espSession.sendMessage(new TextMessage(jsonResponse));
                        }
                        System.out.println("已发送Coze回复到ESP32");
                    }
                } else if (response != null && response.length == 1) {
                    System.err.println("Coze API 返回: " + response[0]);
                }
                
            } catch (Exception e) {
                System.err.println("Coze API 调用失败: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void handleBinaryMessage(@NotNull WebSocketSession session,
                                    @NotNull BinaryMessage message) {

        String id = session.getId();
        OpusDecoder decoder = decoders.get(id);
        ByteArrayOutputStream pcm = pcmBuffers.get(id);
        ASRClient asrClient = asrClients.get(id);

        if (decoder == null || pcm == null) {
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] opusData = new byte[payload.remaining()];
        payload.get(opusData);

        // 单帧最大 60 ms（Opus 规范）
        short[] pcmFrame = new short[960 * CHANNELS];

        try {
            int samples = decoder.decode(
                    opusData, 0, opusData.length,
                    pcmFrame, 0, pcmFrame.length, false
            );

            if (samples > 0) {
                // short → little-endian PCM16
                byte[] pcmBytes = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    short s = pcmFrame[i];
                    pcmBytes[i * 2] = (byte) (s & 0xff);
                    pcmBytes[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                }

                // 保存到缓冲区（用于生成WAV文件）
                pcm.write(pcmBytes);

                // 实时发送给百度 ASR
                if (asrClient != null && asrClient.isOpen()) {
                    asrClient.sendAudio(pcmBytes);
                }
            }

        } catch (Exception e) {
            System.err.println("Opus 解码失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        cancelSession(session.getId());
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) {
        cancelSession(session.getId());
    }

    private synchronized void cancelSession(String id) {
        Timer hb = heartbeats.remove(id);
        if (hb != null) hb.cancel();

        decoders.remove(id);
        ByteArrayOutputStream pcm = pcmBuffers.remove(id);

        // 关闭百度 ASR 客户端
        ASRClient asrClient = asrClients.remove(id);
        if (asrClient != null) {
            try {
                asrClient.finish();  // 发送 FINISH 消息
                // 延迟关闭，等待最后的识别结果
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (asrClient.isOpen()) {
                            asrClient.close();
                        }
                    }
                }, 3000);
            } catch (Exception e) {
                System.err.println("关闭ASR客户端失败: " + e.getMessage());
            }
        }

        if (pcm != null && pcm.size() > 0) {
            try {
                File dir = new File(RECORD_DIR);
                if (!dir.exists()) dir.mkdirs();

                String ts = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

                File wav = new File(
                        RECORD_DIR,
                        "opus_" + id.substring(0, 8) + "_" + ts + ".wav"
                );

                WavFileUtil.savePcmAsWav(pcm.toByteArray(), wav);

                System.out.println("WAV 已生成: " + wav.getAbsolutePath());

            } catch (Exception e) {
                System.err.println("保存 WAV 失败: " + e.getMessage());
            }
        }

        System.out.println("ESP32 断开: " + id);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
