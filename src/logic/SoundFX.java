package logic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Lightweight synthesized retro audio cues using javax.sound.sampled.
 * Runs asynchronously on daemon threads so the 60 Hz game loop never blocks.
 */
public class SoundFX {

    public static void play(boolean enabled, String type) {
        if (!enabled) return;
        Thread t = new Thread(() -> {
            switch (type) {
                case "COIN":
                    tone(880, 55, 0.12);
                    break;
                case "KITTEN":
                    tone(660, 70, 0.16);
                    tone(990, 90, 0.16);
                    break;
                case "PORTAL":
                    tone(523, 80, 0.18);
                    tone(659, 80, 0.18);
                    tone(1046, 160, 0.20);
                    break;
                case "RECALL":
                    tone(740, 50, 0.14);
                    tone(520, 70, 0.14);
                    break;
                case "STUN":
                    tone(440, 70, 0.15);
                    tone(330, 90, 0.15);
                    break;
                case "HIT":
                    tone(180, 140, 0.22);
                    break;
            }
        }, "sfx-" + type);
        t.setDaemon(true);
        t.start();
    }

    private static void tone(int hz, int msecs, double vol) {
        try {
            float sampleRate = 16000f;
            byte[] buf = new byte[(int) (msecs * sampleRate / 1000)];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                double env = 1.0 - ((double) i / buf.length);
                buf[i] = (byte) (Math.sin(angle) * 127.0 * vol * env);
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            try (SourceDataLine sdl = AudioSystem.getSourceDataLine(af)) {
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
            }
        } catch (Exception ignored) {
            // Audio device may be unavailable in headless/CI environments
        }
    }
}
