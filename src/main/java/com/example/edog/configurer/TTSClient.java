package com.example.edog.configurer;

import com.alibaba.fastjson2.JSONObject;
import com.example.edog.utils.AudioWebSocketHandler;
import com.example.edog.utils.BaiduAuthService;
import lombok.Getter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * ✅ 百度流式文本在线合成（2025 官方协议版）
 * - 支持边输入边播放
 * - 自动空文本保护
 * - 自动等待 system.started 后发送文本
 */
public class TTSClient extends WebSocketClient {

    private final Consumer<byte[]> audioCallback;
    private Timer idleTimer;
    private static final long IDLE_TIMEOUT = 3000L;

    @Getter
    private boolean finished = false;
    private boolean systemReady = false;

    // 实时语音参数
    private int per;  // 发音人
    private int spd;  // 语速
    private int pid;  // 音色
    private int vol;  // 音量

    @Getter
    private long totalBytesSent = 0;

    // ==================== 构造函数 ====================

    public TTSClient(Consumer<byte[]> audioCallback) throws Exception {
        // ✅ 官方要求仅保留 access_token 与 per
        super(new URI("wss://aip.baidubce.com/ws/2.0/speech/publiccloudspeech/v1/tts"
                + "?access_token=" + BaiduAuthService.getAccessToken()
                + "&per=" + TTSConfig.getPer()));

        this.audioCallback = audioCallback;
        this.per = TTSConfig.getPer();
        this.spd = TTSConfig.getSpd();
        this.pid = TTSConfig.getPid();
        this.vol = TTSConfig.getVol();
    }

    // ==================== 生命周期回调 ====================

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ 已连接百度 TTS WebSocket");

        // 发送启动帧
        JSONObject start = new JSONObject();
        start.put("type", "system.start");

        JSONObject payload = new JSONObject();
        payload.put("spd", spd);
        payload.put("pid", pid);
        payload.put("vol", vol);
        payload.put("aue", 4); // PCM 16K
        payload.put("audio_ctrl", "{\"sampling_rate\":16000}");
        start.put("payload", payload);

        send(start.toJSONString());
        System.out.println("📤 （TTS）已发送 system.start 参数: " + payload);

        startIdleTimer();
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📩 （TTS）收到文本消息: " + message);
        resetIdleTimer();

        try {
            JSONObject json = JSONObject.parseObject(message);
            String type = json.getString("type");

            switch (type) {
                case "system.started":
                    systemReady = true;
                    System.out.println("✅ （TTS）系统初始化完成，可以发送文本");
                    break;

                case "system.error":
                case "error":
                    System.err.println("❌ TTS 错误消息: " + json);
                    break;

                case "system.finished":
                    finished = true;
                    AudioWebSocketHandler.sendToOHOS("PCM_FINISHED");
                    System.out.println("🏁 （TTS）所有文本合成完毕");
                    close();
                    break;

                default:
                    System.out.println("（TTS）收到其他类型消息: " + type);
                    break;
            }
        } catch (Exception e) {
            System.err.println("（TTS）消息解析失败: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        byte[] data = new byte[bytes.remaining()];
        bytes.get(data);
        totalBytesSent += data.length;

        if (audioCallback != null && data.length > 0) {
            audioCallback.accept(data);
        }

        System.out.println("🎵 （TTS）收到音频帧, 大小: " + data.length + "，累计: " + totalBytesSent + " 字节");
        resetIdleTimer();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("🔌 TTS 连接关闭: code=" + code + ", reason=" + reason);
        stopIdleTimer();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ TTS 出错: " + ex.getMessage());
        stopIdleTimer();
    }

    // ==================== 主逻辑 ====================

    /** ✅ 发送文本进行合成 */
    public void sendText(String text) {
        // 空值保护
        if (text == null || text.trim().isEmpty()) {
            System.err.println("⚠️ sendText 调用时文本为空，将使用默认提示文本。");
            text = "抱歉，我没有听清楚。";
        }

        if (!systemReady) {
            System.err.println("⚠️ 系统未准备好，无法发送文本");
            return;
        }

        JSONObject msg = new JSONObject();
        msg.put("type", "text");

        JSONObject payload = new JSONObject();
        payload.put("text", text);
        msg.put("payload", payload);

        send(msg.toJSONString());
        System.out.println("📤 （TTS）已发送文本: " + text);
    }

    /** ✅ 结束合成 */
    public void finish() {
        JSONObject finishMsg = new JSONObject();
        finishMsg.put("type", "system.finish");
        send(finishMsg.toJSONString());
        System.out.println("📤 （TTS）已发送 system.finish");
    }

    // ==================== 定时器逻辑 ====================

    private void startIdleTimer() {
        if (idleTimer != null) idleTimer.cancel();
        idleTimer = new Timer();
        idleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("⏳ 超过 3 秒无响应，自动关闭连接");
                close();
            }
        }, IDLE_TIMEOUT);
    }

    private void resetIdleTimer() {
        if (idleTimer != null) {
            idleTimer.cancel();
            startIdleTimer();
        }
    }

    private void stopIdleTimer() {
        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }
    }
}