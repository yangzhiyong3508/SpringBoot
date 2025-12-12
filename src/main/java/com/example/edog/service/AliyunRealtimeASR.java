package com.example.edog.service;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.example.edog.utils.AliyunCredentials;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 阿里云实时语音识别工具类 (适配 NLS SDK 2.2.19)
 */
public class AliyunRealtimeASR {
    private static final Logger log = LoggerFactory.getLogger(AliyunRealtimeASR.class);

    // 静态单例 Client
    private static NlsClient nlsClient;
    // 记录当前 Client 使用的 Token，用于对比是否需要刷新
    private static String currentClientToken = "";
    // 锁对象
    private static final Object clientLock = new Object();

    private SpeechTranscriber transcriber;
    private OpusDecoder opusDecoder;
    private Consumer<String> textCallback;
    private volatile boolean isRunning = false;
    private final short[] pcmBuffer = new short[5760];

    public AliyunRealtimeASR() {
        try {
            this.opusDecoder = new OpusDecoder(16000, 1);
        } catch (OpusException e) {
            log.error("Opus解码器初始化失败", e);
        }
    }

    public void setOnResultCallback(Consumer<String> callback) {
        this.textCallback = callback;
    }

    /**
     * 启动识别
     * @param token 最新的有效 Token
     */
    public void start(String token) {
        // 1. 检查并刷新 NlsClient
        // 如果 Client 为空，或者 Token 变了（过期刷新了），则重新创建 Client
        synchronized (clientLock) {
            if (nlsClient == null || !token.equals(currentClientToken)) {
                log.info("检测到 Token 变更或 Client 未初始化，正在重置 NlsClient...");
                
                // 如果有旧的，先关闭
                if (nlsClient != null) {
                    try {
                        nlsClient.shutdown();
                    } catch (Exception e) {
                        log.warn("关闭旧 NlsClient 失败", e);
                    }
                }

                // 创建新的，绑定新 Token
                try {
                    nlsClient = new NlsClient(token);
                    currentClientToken = token; // 更新记录
                    log.info("NlsClient 初始化/刷新成功");
                } catch (Exception e) {
                    log.error("NlsClient 创建失败", e);
                    return;
                }
            }
        }

        try {
            // 2. 使用 NlsClient 创建 Transcriber
            // 注意：SDK 2.2.19 的 SpeechTranscriber 会自动使用 nlsClient 里的 Token
            transcriber = new SpeechTranscriber(nlsClient, getListener());
            transcriber.setAppKey(AliyunCredentials.APP_KEY);

            // ❌ 已删除报错代码: transcriber.setToken(token); 

            // 3. 参数设置
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnablePunctuation(true);
            transcriber.addCustomedParam("enable_inverse_text_normalization", true);
            transcriber.setEnableIntermediateResult(false);

            transcriber.start();
            isRunning = true;
            log.info("ASR 会话启动成功");

        } catch (Exception e) {
            log.error("ASR 启动异常", e);
            isRunning = false;
        }
    }

    public void sendOpusStream(byte[] opusBytes) {
        if (!isRunning || transcriber == null) return;
        try {
            int samples = opusDecoder.decode(opusBytes, 0, opusBytes.length, pcmBuffer, 0, pcmBuffer.length, false);
            if (samples > 0) {
                byte[] pcmData = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    short s = pcmBuffer[i];
                    pcmData[i * 2] = (byte) (s & 0xff);
                    pcmData[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                }
                transcriber.send(pcmData);
            }
        } catch (Exception e) {
            log.warn("音频发送失败: {}", e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        if (transcriber != null) {
            try { transcriber.stop(); } catch (Exception e) {}
            finally { transcriber.close(); transcriber = null; }
        }
        log.info("ASR 会话已停止");
    }

    private SpeechTranscriberListener getListener() {
        return new SpeechTranscriberListener() {
            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                log.info("任务开启: {}", response.getTaskId());
            }
            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                log.info("任务结束: {}", response.getStatus());
            }
            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                String text = response.getTransSentenceText();
                log.info("识别结果: {}", text);
                if (textCallback != null && text != null && !text.isEmpty()) {
                    textCallback.accept(text);
                }
            }
            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {}
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {}
            @Override
            public void onFail(SpeechTranscriberResponse response) {
                log.error("ASR Error: {}", response.getStatusText());
            }
        };
    }
}