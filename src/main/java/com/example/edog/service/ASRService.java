package com.example.edog.service;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class ASRService extends WebSocketClient {

    public ASRService(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ WebSocket 连接成功");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("🎤 识别结果: " + message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        System.out.println("收到二进制消息: " + bytes.remaining() + " 字节");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ WebSocket 关闭: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("⚠️ WebSocket 错误: " + ex.getMessage());
    }

    /**
     * 分片发送音频
     * @param audioData PCM 16kHz 16bit 单声道原始音频
     */
    public void sendAudioInChunks(byte[] audioData) {
        int frameSize = 3200; // 100ms
        try {
            for (int i = 0; i < audioData.length; i += frameSize) {
                int end = Math.min(audioData.length, i + frameSize);
                byte[] chunk = new byte[end - i];
                System.arraycopy(audioData, i, chunk, 0, chunk.length);

                if (isOpen()) {  // 检查连接是否可用
                    send(chunk);
                } else {
                    System.err.println("⚠️ WebSocket 已关闭，无法发送");
                    break;
                }

                Thread.sleep(100); // 模拟实时发送
            }

            if (isOpen()) {
                send("{\"type\":\"FINISH\"}");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}