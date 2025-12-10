package com.example.edog.utils;

import java.io.*;
import java.nio.file.*;

/**
 * 音频格式转换工具
 * 将 MP3 转换为设备可接受的 OGG/Opus 格式
 * 
 * 设备端要求：
 * - 格式：OGG 容器 + Opus 编码
 * - 采样率：16000 Hz
 * - 声道：单声道 (mono)
 * - 帧时长：60ms
 */
public class AudioConverter {

    // FFmpeg 可执行文件路径
    private static final String FFMPEG_PATH = "D:\\Ffmepg\\bin\\ffmpeg.exe";

    /**
     * 将 MP3 文件转换为 OGG/Opus 格式
     * @param inputMp3Path 输入的 MP3 文件路径
     * @return 转换后的 OGG 文件路径，失败返回 null
     */
    public static String convertMp3ToOpusOgg(String inputMp3Path) {
        if (inputMp3Path == null || inputMp3Path.isEmpty()) {
            return null;
        }

        File inputFile = new File(inputMp3Path);
        if (!inputFile.exists()) {
            System.err.println("输入文件不存在: " + inputMp3Path);
            return null;
        }

        // 生成输出文件路径 (将 .mp3 替换为 .ogg)
        String outputPath = inputMp3Path.replaceAll("\\.[^.]+$", ".ogg");

        try {
            // 使用 FFmpeg 转换
            // 参数说明：
            // -y: 覆盖输出文件
            // -i: 输入文件
            // -c:a libopus: 使用 Opus 编码器
            // -ar 16000: 采样率 16kHz
            // -ac 1: 单声道
            // -b:a 24k: 比特率 24kbps (适合语音)
            // -application voip: 优化语音
            // -frame_duration 60: 帧时长 60ms (与设备端匹配)
            ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_PATH,
                "-y",
                "-i", inputMp3Path,
                "-c:a", "libopus",
                "-ar", "16000",
                "-ac", "1",
                "-b:a", "24k",
                "-application", "voip",
                "-frame_duration", "60",
                outputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 可以打印 FFmpeg 输出用于调试
                    // System.out.println("[FFmpeg] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                File outputFile = new File(outputPath);
                if (outputFile.exists()) {
                    System.out.println("音频转换成功: " + outputPath + " (" + outputFile.length() / 1024 + " KB)");
                    return outputPath;
                }
            } else {
                System.err.println("FFmpeg 转换失败，退出码: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("音频转换异常: " + e.getMessage());
            // 如果 FFmpeg 不可用，尝试使用备用方案
            return tryFallbackConversion(inputMp3Path, outputPath);
        }

        return null;
    }

    /**
     * 将 MP3 字节数据转换为 OGG/Opus 格式
     * @param mp3Data MP3 音频数据
     * @param tempDir 临时文件目录
     * @return 转换后的 OGG 文件字节数据，失败返回 null
     */
    public static byte[] convertMp3BytesToOpusOgg(byte[] mp3Data, String tempDir) {
        if (mp3Data == null || mp3Data.length == 0) {
            return null;
        }

        try {
            // 创建临时目录
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 写入临时 MP3 文件
            String timestamp = String.valueOf(System.currentTimeMillis());
            File tempMp3 = new File(dir, "temp_" + timestamp + ".mp3");
            Files.write(tempMp3.toPath(), mp3Data);

            // 转换
            String oggPath = convertMp3ToOpusOgg(tempMp3.getAbsolutePath());

            // 读取 OGG 文件
            byte[] oggData = null;
            if (oggPath != null) {
                File oggFile = new File(oggPath);
                if (oggFile.exists()) {
                    oggData = Files.readAllBytes(oggFile.toPath());
                    // 删除临时 OGG 文件
                    oggFile.delete();
                }
            }

            // 删除临时 MP3 文件
            tempMp3.delete();

            return oggData;

        } catch (Exception e) {
            System.err.println("转换字节数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查 FFmpeg 是否可用
     */
    public static boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(FFMPEG_PATH, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 备用转换方案（当 FFmpeg 不可用时）
     */
    private static String tryFallbackConversion(String inputPath, String outputPath) {
        System.err.println("FFmpeg 不可用，请确认路径: " + FFMPEG_PATH);
        return null;
    }

    /**
     * 获取音频文件信息
     */
    public static void printAudioInfo(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                filePath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                System.out.println("音频信息:\n" + output);
            }

            process.waitFor();
        } catch (Exception e) {
            System.err.println("获取音频信息失败: " + e.getMessage());
        }
    }
}
