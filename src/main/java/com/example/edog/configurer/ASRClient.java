package com.example.edog.configurer;

import lombok.Setter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 百度实时语音识别客户端（始终保持心跳）
 */
public class ASRClient extends WebSocketClient {

    @Setter
    private Consumer<String> recognitionCallback; // 识别结果回调

    private boolean finished = false;
    private Timer heartbeatTimer;

    public ASRClient(String token) throws Exception {
        super(new URI(buildUrl(token)));
    }

    /** 构造百度 ASR WebSocket URL */
    private static String buildUrl(String token) throws Exception {
        String sn = UUID.randomUUID().toString();
        return "wss://vop.baidu.com/realtime_asr?sn=" + sn + "&token=" + URLEncoder.encode(token, "UTF-8");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("（ASR）已连接百度ASR服务器");

        // 构建 START 消息
        String startMsg = "{ \"type\":\"START\", \"data\":{" +
                "\"appid\":120108667," +
                "\"appkey\":\"sov5I4aEwzZldEDgSyTmvbs6\"," +
                "\"dev_pid\":15372," +
                "\"cuid\":\"edog-device\"," +
                "\"format\":\"pcm\"," +
                "\"sample\":16000" +
                "} }";
        this.send(startMsg);
        System.out.println("（ASR）已发送 START 消息");

        startHeartbeat(); // 启动心跳
    }

    @Override
    public void onMessage(String message) {
        if (recognitionCallback != null) {
            recognitionCallback.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("ASR 连接关闭: " + reason);
        stopHeartbeat();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("ASR 错误: " + ex.getMessage());
        stopHeartbeat();
    }

    /**
     * 始终保持心跳（不依赖 finished）
     */
    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isOpen()) {
                    try {
                        send("{\"type\":\"HEARTBEAT\"}");
                    } catch (Exception e) {
                        // 心跳发送失败，静默处理
                    }
                }
            }
        }, 3000, 3000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    /** 发送音频（PCM数据） */
    public void sendAudio(byte[] audioData) {
        if (this.isOpen() && !finished && audioData != null && audioData.length > 0) {
            try {
                this.send(ByteBuffer.wrap(audioData));
            } catch (Exception e) {
                System.err.println("发送音频失败: " + e.getMessage());
            }
        }
    }

    /** 发送 FINISH 消息 */
    public void finish() {
        if (this.isOpen() && !finished) {
            finished = true;
            this.send("{\"type\":\"FINISH\"}");
            System.out.println("（ASR）已发送 FINISH 消息");

            new Timer(true).schedule(new TimerTask() {
                @Override
                public void run() {
                    stopHeartbeat();
                }
            }, 5000);
        }
    }
}