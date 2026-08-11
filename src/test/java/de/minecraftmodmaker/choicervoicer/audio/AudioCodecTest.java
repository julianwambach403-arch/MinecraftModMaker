package de.minecraftmodmaker.choicervoicer.audio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioCodecTest {
    @ParameterizedTest
    @ValueSource(strings = {"/audio/tone.mp3", "/audio/tone.ogg"})
    void decodesChoicerVoicerCompressedFormats(String resource) throws Exception {
        Path path = resource(resource);
        short[] samples = AudioCodec.decode(path);
        assertTrue(samples.length >= 1_000);
        assertTrue(java.util.stream.IntStream.range(0, samples.length)
                .anyMatch(index -> samples[index] != 0));
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(AudioCodecTest.class.getResource(name).toURI());
    }
}
