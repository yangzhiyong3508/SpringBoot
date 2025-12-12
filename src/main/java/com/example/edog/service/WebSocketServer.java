package com.example.edog.service;

import com.example.edog.entity.Account;
import com.example.edog.utils.AudioConverter;
import com.example.edog.utils.CozeAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WebSocketServer extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    @Autowired
    private AliyunTokenService tokenService;

    private final Map<String, AliyunRealtimeASR> asrServices = new ConcurrentHashMap<>();
    private final Map<String, Timer> sessionTimers = new ConcurrentHashMap<>(); 
    private final Map<String, AtomicBoolean> sessionBusyState = new ConcurrentHashMap<>();
    
    // 记录最后一次发送给 ASR 数据的时间戳
    private final Map<String, Long> lastAsrSendTime = new ConcurrentHashMap<>();

    private final CozeAPI cozeAPI = new CozeAPI();

    private static volatile String currentVoiceId = "7568423452617523254";
    private static volatile Double currentSpeedRatio = 1.0;

    // Opus 静音帧
    private static final byte[] OPUS_SILENCE_FRAME = new byte[]{(byte) 0xF8, (byte) 0xFF, (byte) 0xFE};

    public static void setVoiceParams(Account account) {
        if (account != null) {
            if (account.getVoiceId() != null && !account.getVoiceId().isEmpty()) {
                currentVoiceId = account.getVoiceId();
            }
            if (account.getSpeedRatio() != null) {
                currentSpeedRatio = account.getSpeedRatio();
            }
            log.info("语音参数已全局更新: voiceId={}, speed={}", currentVoiceId, currentSpeedRatio);
        }
    }

    public static void setVoiceParams(String voiceId, Double speedRatio) {
        if (voiceId != null && !voiceId.isEmpty()) currentVoiceId = voiceId;
        if (speedRatio != null) currentSpeedRatio = speedRatio;
        log.info("语音参数已全局更新: voiceId={}, speed={}", currentVoiceId, currentSpeedRatio);
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        String id = session.getId();
        log.info("ESP32 Connected: {}", id);

        sessionBusyState.put(id, new AtomicBoolean(false));
        lastAsrSendTime.put(id, System.currentTimeMillis()); 

        AliyunRealtimeASR asr = new AliyunRealtimeASR();
        asr.setOnResultCallback(text -> {
            if (isSessionBusy(id)) return;
            handleUserQuestion(session, text);
        });

        try {
            String token = tokenService.getToken();
            asr.start(token);
            asrServices.put(id, asr);
        } catch (Exception e) {
            log.error("ASR 启动失败", e);
            session.close();
            return;
        }

        // 启动定时任务
        Timer timer = new Timer(true);
        
        // 1. WebSocket 心跳
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (session.isOpen()) session.sendMessage(new PingMessage());
                } catch (Exception e) {}
            }
        }, 5000, 5000);

        // 2. ASR 保活任务
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    AliyunRealtimeASR currentAsr = asrServices.get(id);
                    if (currentAsr == null || !session.isOpen()) return;

                    long lastTime = lastAsrSendTime.getOrDefault(id, 0L);
                    long now = System.currentTimeMillis();

                    if (now - lastTime > 800) {
                        currentAsr.sendOpusStream(OPUS_SILENCE_FRAME);
                        lastAsrSendTime.put(id, now);
                    }
                } catch (Exception e) {
                    log.error("保活帧发送失败", e);
                }
            }
        }, 1000, 500); 

        sessionTimers.put(id, timer);
    }

    @Override
    protected void handleBinaryMessage(@NotNull WebSocketSession session, @NotNull BinaryMessage message) {
        String id = session.getId();
        AliyunRealtimeASR asr = asrServices.get(id);

        if (asr != null) {
            try {
                lastAsrSendTime.put(id, System.currentTimeMillis());

                if (isSessionBusy(id)) {
                    asr.sendOpusStream(OPUS_SILENCE_FRAME);
                } else {
                    ByteBuffer payload = message.getPayload();
                    byte[] opusData = new byte[payload.remaining()];
                    payload.get(opusData);
                    asr.sendOpusStream(opusData);
                }
            } catch (Exception e) {
                log.error("ASR 发送异常, 尝试重置", e);
                resetAsr(session, id, asr);
            }
        }
    }

    private void resetAsr(WebSocketSession session, String id, AliyunRealtimeASR oldAsr) {
        if (oldAsr != null) oldAsr.stop();
        AliyunRealtimeASR newAsr = new AliyunRealtimeASR();
        newAsr.setOnResultCallback(text -> {
            if (!isSessionBusy(id)) handleUserQuestion(session, text);
        });
        try {
            newAsr.start(tokenService.getToken());
            asrServices.put(id, newAsr);
            log.info("ASR 引擎已自动恢复");
        } catch (Exception ex) {
            log.error("ASR 恢复失败", ex);
        }
    }

    private void handleUserQuestion(WebSocketSession session, String question) {
        String id = session.getId();
        setSessionBusy(id, true);

        new Thread(() -> {
            try {
                String shouldUseVoiceId = currentVoiceId;
                Double shouldUseSpeed = currentSpeedRatio;

                log.info("请求智能体: '{}' (Locking session)", question);

                String[] response = cozeAPI.CozeRequest(question, shouldUseVoiceId, shouldUseSpeed, true);

                if (response != null && response.length >= 2) {
                    String audioUrl = response[0];
                    String replyText = response[1];

                    if (!session.isOpen()) {
                        setSessionBusy(id, false);
                        return;
                    }

                    // 1. 发送文本
                    String startJson = String.format("{\"type\":\"tts\",\"state\":\"start\",\"text\":\"%s\"}",
                            replyText.replace("\"", "\\\"").replace("\n", ""));
                    session.sendMessage(new TextMessage(startJson));

                    // 2. 发送音频
                    long estimatedPlaybackDuration = 0;
                    long timeSpentSending = 0; 

                    if (audioUrl != null && !audioUrl.isEmpty()) {
                        String mp3Path = cozeAPI.downloadAudio(audioUrl, "coze_audio");
                        if (mp3Path != null) {
                            List<byte[]> opusFrames = AudioConverter.convertMp3ToOpusFrames(mp3Path);
                            if (opusFrames != null && !opusFrames.isEmpty()) {
                                int frameDurationMs = 60;
                                estimatedPlaybackDuration = (long) opusFrames.size() * frameDurationMs;
                                timeSpentSending = (long) opusFrames.size() * 50;

                                log.info("音频帧数: {}, 预估播放时长: {} ms", opusFrames.size(), estimatedPlaybackDuration);

                                for (byte[] frame : opusFrames) {
                                    if (!session.isOpen()) break;
                                    session.sendMessage(new BinaryMessage(frame));
                                    
                                    // 发送数据也要更新时间戳，避免ASR保活逻辑冲突
                                    lastAsrSendTime.put(id, System.currentTimeMillis());
                                    Thread.sleep(50); 
                                }
                            }
                        }
                    } else {
                        estimatedPlaybackDuration = 2000;
                        timeSpentSending = 0;
                    }

                    // 3. 发送结束标志
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("{\"type\":\"tts\",\"state\":\"end\"}"));
                    }

                    long bufferTime = 0; 
                    long remainingWait = estimatedPlaybackDuration - timeSpentSending;
                    if (remainingWait < 0) remainingWait = 0;
                    long unlockDelay = remainingWait + bufferTime;

                    log.info("发送完毕。设备预计还需播放 {} ms，将在 {} ms 后解锁输入", remainingWait, unlockDelay);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            setSessionBusy(id, false);
                            log.info("会话已解锁，准备接收新语音");
                        }
                    }, unlockDelay);

                } else {
                    setSessionBusy(id, false);
                }
            } catch (Exception e) {
                log.error("处理失败", e);
                setSessionBusy(id, false);
            }
        }).start();
    }

    private boolean isSessionBusy(String sessionId) {
        AtomicBoolean state = sessionBusyState.get(sessionId);
        return state != null && state.get();
    }

    private void setSessionBusy(String sessionId, boolean busy) {
        AtomicBoolean state = sessionBusyState.get(sessionId);
        if (state != null) {
            state.set(busy);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        String id = session.getId();
        log.info("ESP32 Disconnected: {}", id);

        AliyunRealtimeASR asr = asrServices.remove(id);
        if (asr != null) asr.stop();

        Timer timer = sessionTimers.remove(id);
        if (timer != null) timer.cancel();

        sessionBusyState.remove(id);
        lastAsrSendTime.remove(id);
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) {
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}