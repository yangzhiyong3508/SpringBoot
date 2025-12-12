package com.example.edog.service;

import com.alibaba.nls.client.AccessToken;
import com.example.edog.utils.AliyunCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 阿里云 Token 管理服务
 * 功能：自动申请和刷新 Token，确保 Token 始终有效
 */
@Service
public class AliyunTokenService {

    private static final Logger log = LoggerFactory.getLogger(AliyunTokenService.class);

    private String token;
    private long expireTime = 0; // 过期时间戳（秒）

    /**
     * 获取有效的 Token
     * 如果缓存为空或即将过期（提前10分钟），则重新申请
     */
    public synchronized String getToken() {
        long now = System.currentTimeMillis() / 1000;
        
        // 如果没有 Token 或者 Token 还有 10 分钟就过期了，就刷新
        if (token == null || (expireTime - now) < 600) {
            log.info("Token 即将过期或不存在，正在刷新...");
            refreshToken();
        }
        return token;
    }

    private void refreshToken() {
        try {
            // 使用 AccessKey ID 和 Secret 申请新 Token
            AccessToken accessToken = new AccessToken(
                    AliyunCredentials.ACCESS_KEY_ID, 
                    AliyunCredentials.ACCESS_KEY_SECRET
            );
            accessToken.apply();

            this.token = accessToken.getToken();
            this.expireTime = accessToken.getExpireTime();
            
            log.info("Token 刷新成功, 有效期至: {} (时间戳: {})", 
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expireTime * 1000)),
                    expireTime);
            
        } catch (IOException e) {
            log.error("获取阿里云 Token 失败", e);
            throw new RuntimeException("无法获取语音识别 Token", e);
        }
    }
}