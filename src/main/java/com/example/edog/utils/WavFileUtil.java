package com.example.edog.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavFileUtil {

    // PCM参数（根据您的ESP32配置调整这些参数）
    private static final int SAMPLE_RATE = 16000;    // 采样率
    private static final int BITS_PER_SAMPLE = 16;   // 位深
    private static final int CHANNELS = 1;           // 单声道

    /**
     * 将PCM数据保存为WAV文件
     * @param pcmData PCM音频数据
     * @param outputFile 输出文件
     * @throws IOException
     */
    public static void savePcmAsWav(byte[] pcmData, File outputFile) throws IOException {
        // 确保目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // 写入WAV文件头
            writeWavHeader(dos, pcmData.length);

            // 写入PCM数据
            dos.write(pcmData);

            System.out.println("📊 WAV文件信息: " +
                    "时长=" + calculateDuration(pcmData) + "s, " +
                    "大小=" + pcmData.length + " bytes");
        }
    }

    /**
     * 写入WAV文件头
     */
    private static void writeWavHeader(DataOutputStream dos, int pcmDataLength) throws IOException {
        // 计算总文件大小（44字节头 + PCM数据长度）
        int totalDataLen = pcmDataLength + 36;
        int byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;

        // RIFF头
        dos.writeBytes("RIFF");                                  // ChunkID
        writeIntLittleEndian(dos, totalDataLen);                 // ChunkSize
        dos.writeBytes("WAVE");                                  // Format

        // fmt子块
        dos.writeBytes("fmt ");                                  // Subchunk1ID
        writeIntLittleEndian(dos, 16);                           // Subchunk1Size (16 for PCM)
        writeShortLittleEndian(dos, (short) 1);                  // AudioFormat (1 for PCM)
        writeShortLittleEndian(dos, (short) CHANNELS);           // NumChannels
        writeIntLittleEndian(dos, SAMPLE_RATE);                  // SampleRate
        writeIntLittleEndian(dos, byteRate);                     // ByteRate
        writeShortLittleEndian(dos, (short) (CHANNELS * BITS_PER_SAMPLE / 8)); // BlockAlign
        writeShortLittleEndian(dos, (short) BITS_PER_SAMPLE);    // BitsPerSample

        // data子块
        dos.writeBytes("data");                                  // Subchunk2ID
        writeIntLittleEndian(dos, pcmDataLength);                // Subchunk2Size
    }

    /**
     * 以小端序写入int
     */
    private static void writeIntLittleEndian(DataOutputStream dos, int value) throws IOException {
        dos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    /**
     * 以小端序写入short
     */
    private static void writeShortLittleEndian(DataOutputStream dos, short value) throws IOException {
        dos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    /**
     * 计算音频时长（秒）
     */
    public static double calculateDuration(byte[] pcmData) {
        int bytesPerSecond = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;
        return (double) pcmData.length / bytesPerSecond;
    }

    /**
     * 获取音频配置信息
     */
    public static String getAudioConfig() {
        return String.format("采样率: %dHz, 位深: %dbit, 声道: %d",
                SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS);
    }
}