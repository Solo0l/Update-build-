package com.example.PentoMaster_v1;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundManager {
    public static void playSound(String fileName) {
        new Thread(() -> {
            try {
                InputStream is = SoundManager.class.getResourceAsStream("/sounds/" + fileName);
                if (is == null) return;

                InputStream bufferedIn = new BufferedInputStream(is);
                AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bufferedIn);
                AudioFormat baseFormat = sourceStream.getFormat();

                // تحويل الملف يدوياً داخل الكود لنوع تدعمه الجافا (PCM_SIGNED)
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(), false);

                AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
                Clip clip = AudioSystem.getClip();
                clip.open(targetStream);
                clip.start();
            } catch (Exception e) {
                System.err.println("Error with sound: " + fileName);
                e.printStackTrace();
            }
        }).start();
    }
}