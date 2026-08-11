package de.minecraftmodmaker.choicervoicer.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WavWriter {
    private WavWriter() {
    }

    public static byte[] toWav(short[] samples, float sampleRate) {
        int dataBytes = samples.length * 2;
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + dataBytes);
        buffer.put("WAVE".getBytes());
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(Math.round(sampleRate));
        buffer.putInt(Math.round(sampleRate) * 2);
        buffer.putShort((short) 2);
        buffer.putShort((short) 16);
        buffer.put("data".getBytes());
        buffer.putInt(dataBytes);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    public static void write(Path path, short[] samples, float sampleRate) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, toWav(samples, sampleRate));
    }
}
