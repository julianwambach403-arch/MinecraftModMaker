package de.minecraftmodmaker.choicervoicer.audio;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavWriterTest {
    @Test
    void writesValidWavHeader() {
        short[] samples = {0, 1000, -1000, 0};
        byte[] wav = WavWriter.toWav(samples, AudioCodec.SAMPLE_RATE);
        ByteBuffer buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);

        byte[] riff = new byte[4];
        buffer.get(riff);
        assertEquals("RIFF", new String(riff));
        buffer.position(8);
        byte[] wave = new byte[4];
        buffer.get(wave);
        assertEquals("WAVE", new String(wave));
        assertEquals(44 + samples.length * 2, wav.length);
        assertTrue(wav.length > 44);
    }
}
