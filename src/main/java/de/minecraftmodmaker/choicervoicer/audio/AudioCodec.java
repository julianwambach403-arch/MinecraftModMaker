package de.minecraftmodmaker.choicervoicer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class AudioCodec {
    public static final float SAMPLE_RATE = 48_000F;
    public static final int MAX_SECONDS = 60;
    private static final AudioFormat PCM_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 16, 1, 2, SAMPLE_RATE, false
    );

    private AudioCodec() {
    }

    public static short[] decode(Path path) throws IOException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile());
             AudioInputStream converted = AudioSystem.getAudioInputStream(PCM_FORMAT, source);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16_384];
            int read;
            int maxBytes = (int) SAMPLE_RATE * 2 * MAX_SECONDS;
            while ((read = converted.read(buffer)) >= 0) {
                if (output.size() + read > maxBytes) {
                    throw new IOException("Audio clip exceeds " + MAX_SECONDS + " seconds: " + path);
                }
                output.write(buffer, 0, read);
            }
            byte[] bytes = output.toByteArray();
            short[] samples = new short[bytes.length / 2];
            for (int index = 0; index < samples.length; index++) {
                int low = bytes[index * 2] & 0xFF;
                int high = bytes[index * 2 + 1];
                samples[index] = (short) ((high << 8) | low);
            }
            return normalize(samples);
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw new IOException("Unsupported or corrupt audio file: " + path, exception);
        }
    }

    public static short[] opusToPcm(byte[] opus, de.maxhenkel.voicechat.api.opus.OpusDecoder decoder)
            throws IOException {
        try {
            return decoder.decode(opus);
        } catch (RuntimeException exception) {
            throw new IOException("Could not decode voice packet", exception);
        }
    }

    static short[] normalize(short[] input) {
        int peak = 0;
        for (short sample : input) {
            peak = Math.max(peak, Math.abs((int) sample));
        }
        if (peak < 1 || peak >= 29_490) {
            return input;
        }
        double gain = 29_490D / peak;
        short[] output = new short[input.length];
        for (int index = 0; index < input.length; index++) {
            output[index] = (short) Math.clamp(Math.round(input[index] * gain), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return output;
    }

    public static double durationSeconds(short[] samples) {
        return samples.length / SAMPLE_RATE;
    }
}
