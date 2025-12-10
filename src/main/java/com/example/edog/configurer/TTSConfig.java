package com.example.edog.configurer;

import lombok.Getter;

/** 全局 TTS 参数缓存（无管理器版本） */
public class TTSConfig {
    @Getter
    private static volatile int per = 4;
    @Getter
    private static volatile int spd = 6;
    @Getter
    private static volatile int pid = 10;
    @Getter
    private static volatile int vol = 8;

    public static synchronized void setVoiceParams(int p, int s, int pi, int v) {
        per = p; spd = s; pid = pi; vol = v;
    }

}