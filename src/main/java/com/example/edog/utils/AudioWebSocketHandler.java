package com.example.edog.utils;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Service
public class AudioWebSocketHandler extends TextWebSocketHandler {

    // 单个 OHOS 客户端 session
    private static WebSocketSession ohosSession;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("AUDIO 已连接: " + session.getId());
        ohosSession = session;
        session.sendMessage(new TextMessage("服务器：AUDIO 连接成功"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("AUDIO 断开: " + session.getId());
        if (ohosSession != null && ohosSession.getId().equals(session.getId())) {
            ohosSession = null;
        }
    }

    // =================== 文本消息发送 ===================
    public static void sendToOHOS(String message) {
        try {
            if (ohosSession != null && ohosSession.isOpen()) {
                ohosSession.sendMessage(new TextMessage(message));
            } else {
                System.err.println("AUDIO 未连接，无法发送文本消息");
            }
        } catch (Exception e) {
            System.err.println("向 AUDIO 发送文本消息失败: " + e.getMessage());
        }
    }

    // =================== 二进制音频发送 ===================
    public static void sendBinaryToOHOS(byte[] audioData) {
        try {
            if (ohosSession != null && ohosSession.isOpen()) {
                ohosSession.sendMessage(new BinaryMessage(audioData));
            } else {
                System.err.println("AUDIO 未连接，无法发送二进制音频");
            }
        } catch (Exception e) {
            System.err.println("向 AUDIO 发送二进制音频失败: " + e.getMessage());
        }
    }
}