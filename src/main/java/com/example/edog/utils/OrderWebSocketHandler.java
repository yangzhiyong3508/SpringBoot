package com.example.edog.utils;

import com.example.edog.entity.Command;
import com.example.edog.service.CommandService;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private CommandService commandService;

    // 单设备模式
    private static WebSocketSession appSession = null; // 手机端
    private static WebSocketSession dogSession = null; // 机械狗端

    /**
     * -- GETTER --
     *  对外公开：获取当前登录账号（供 WebSocketService 使用）
     */
    // 当前手机账号
    @Getter
    private static String currentAccount = null;

    // 当前账号的命令列表（content-message 二维数组）
    @Getter
    private static List<String[]> commandList = new ArrayList<>();

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        System.out.println("（order）新连接建立: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload().trim();
        System.out.println("（order） 收到消息: " + payload);

        // 判断是否为身份绑定消息
        if (payload.startsWith("role=")) {
            handleBindMessage(session, payload);
            return;
        }

        // 如果消息来自手机端，转发给机械狗
        if (session.equals(appSession)) {
            if (dogSession != null && dogSession.isOpen()) {
                dogSession.sendMessage(new TextMessage(payload));
                System.out.println("（order） 从手机 -> 机械狗: " + payload);
            } else {
                System.err.println("（order） 没有机械狗在线");
            }
        }

        // 如果消息来自机械狗，转发给手机端
        else if (session.equals(dogSession)) {
            if (appSession != null && appSession.isOpen()) {
                appSession.sendMessage(new TextMessage(payload));
                System.out.println("（order） 从机械狗 -> 手机: " + payload);
            } else {
                System.err.println("（order） 没有手机在线");
            }
        }
    }

    /**
     * 处理绑定消息（app需要账号，dog不需要）
     */
    private void handleBindMessage(WebSocketSession session, String payload) {
        // 示例: role=app&account=123456 或 role=dog
        String[] parts = payload.split("&");
        String role = null;
        String account = null;

        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                String key = kv[0];
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                if ("role".equals(key)) role = value;
                if ("account".equals(key)) account = value;
            }
        }

        if (role == null) {
            System.err.println("（order） 无效的绑定消息: " + payload);
            return;
        }

        switch (role) {
            case "app":
                appSession = session;
                System.out.println("（order） 手机端已绑定");
                if (account != null && !account.isEmpty()) {
                    currentAccount = account;
                    loadCommandList(account);
                } else {
                    System.err.println("（order） 手机端未提供账号，部分功能将受限");
                }
                break;

            case "dog":
                dogSession = session;
                System.out.println("（order） 机械狗端已绑定（无需账号）");
                break;

            default:
                System.err.println("（order） 未知角色: " + role);
                break;
        }
    }

    /**
     * 从数据库加载命令列表
     */
    private void loadCommandList(String account) {
        commandList.clear();
        List<Command> commands = commandService.getAllCommandByAccount(account);
        for (Command cmd : commands) {
            commandList.add(new String[]{cmd.getContent(), cmd.getMessage()});
        }
        System.out.println("（order） 已加载账号 " + account + " 的命令: " + commandList.size() + " 条");
    }

    /**
     * 服务器主动发送消息
     */
    public static void sendMessage(String role, String message) {
        try {
            WebSocketSession targetSession = null;

            if ("phone".equalsIgnoreCase(role) || "app".equalsIgnoreCase(role)) {
                targetSession = appSession;
            } else if ("device".equalsIgnoreCase(role) || "dog".equalsIgnoreCase(role)) {
                targetSession = dogSession;
            } else {
                System.err.println("（order）无效的发送目标角色: " + role);
                return;
            }

            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(message));
                System.out.println("（order）服务器 -> " + role + ": " + message);
            } else {
                System.err.println("（order）目标设备未在线");
            }
        } catch (IOException e) {
            System.err.println("（order）发送消息失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        if (session.equals(appSession)) {
            appSession = null;
            System.out.println("（order）手机端断开连接");
            currentAccount = null;
            commandList.clear();
        } else if (session.equals(dogSession)) {
            dogSession = null;
            System.out.println("（order） 机械狗端断开连接");
        }
    }

    /**
     * 对外公开刷新当前命令列表的静态方法
     */
    public static void refreshCommandList(String account) {
        if (account == null || account.isEmpty()) {
            System.err.println("（order）无法刷新命令列表：账号为空");
            return;
        }

        try {
            CommandService service = SpringContextUtil.getBean(CommandService.class);
            commandList.clear();

            List<Command> commands = service.getAllCommandByAccount(account);
            for (Command cmd : commands) {
                commandList.add(new String[]{cmd.getContent(), cmd.getMessage()});
            }

            System.out.println("（order）命令列表已刷新，共 " + commandList.size() + " 条");
        } catch (Exception e) {
            System.err.println("（order）刷新命令列表失败: " + e.getMessage());
        }
    }

}
