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

    private AudioCodec() {
    }

    public static short[] decode(Path path) throws IOException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat sourceFormat = source.getFormat();
            float sourceRate = sourceFormat.getSampleRate() > 0 ? sourceFormat.getSampleRate() : SAMPLE_RATE;
            int channels = Math.max(1, sourceFormat.getChannels());
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sourceRate, 16, channels, channels * 2, sourceRate, false);
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(decodedFormat, source);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16_384];
                int read;
                long maxBytes = (long) sourceRate * channels * 2L * MAX_SECONDS;
                while ((read = converted.read(buffer)) >= 0) {
                    if (output.size() + read > maxBytes) {
                        throw new IOException("Audio clip exceeds " + MAX_SECONDS + " seconds: " + path);
                    }
                    output.write(buffer, 0, read);
                }
                short[] mono = downmix(output.toByteArray(), channels);
                return normalize(resample(mono, sourceRate, SAMPLE_RATE));
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw new IOException("Unsupported or corrupt audio file: " + path, exception);
        }
    }

    private static short[] downmix(byte[] bytes, int channels) {
        int frames = bytes.length / (channels * 2);
        short[] mono = new short[frames];
        for (int frame = 0; frame < frames; frame++) {
            int sum = 0;
            for (int channel = 0; channel < channels; channel++) {
                int offset = (frame * channels + channel) * 2;
                int low = bytes[offset] & 0xFF;
                int high = bytes[offset + 1];
                sum += (short) ((high << 8) | low);
            }
            mono[frame] = (short) (sum / channels);
        }
        return mono;
    }

    private static short[] resample(short[] input, float sourceRate, float targetRate) {
        if (input.length == 0 || Math.abs(sourceRate - targetRate) < 1F) {
            return input;
        }
        int outputLength = Math.max(1, Math.round(input.length * targetRate / sourceRate));
        short[] output = new short[outputLength];
        double ratio = sourceRate / targetRate;
        for (int index = 0; index < outputLength; index++) {
            double sourcePosition = index * ratio;
            int lower = Math.min(input.length - 1, (int) sourcePosition);
            int upper = Math.min(input.length - 1, lower + 1);
            double fraction = sourcePosition - lower;
            output[index] = (short) Math.round(input[lower] * (1D - fraction) + input[upper] * fraction);
        }
        return output;
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
