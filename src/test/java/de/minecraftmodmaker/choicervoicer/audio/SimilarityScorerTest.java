package de.minecraftmodmaker.choicervoicer.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityScorerTest {
    private final SimilarityScorer scorer = new SimilarityScorer();

    @Test
    void identicalSignalsReceiveFiveVotes() {
        short[] signal = sine(440D, 2D);
        SimilarityScorer.Score score = scorer.score(signal, signal.clone(), 0.005D);
        assertEquals(5, score.judgeVotes());
        assertTrue(score.similarity() > 0.99D);
    }

    @Test
    void silenceIsRejected() {
        SimilarityScorer.Score score = scorer.score(sine(440D, 1D), new short[48_000], 0.005D);
        assertTrue(score.silent());
        assertEquals(0, score.judgeVotes());
    }

    @Test
    void veryDifferentDurationLosesVotes() {
        SimilarityScorer.Score score = scorer.score(sine(440D, 3D), sine(880D, 0.5D), 0.005D);
        assertTrue(score.judgeVotes() < 4);
    }

    private static short[] sine(double frequency, double seconds) {
        short[] samples = new short[(int) (48_000 * seconds)];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = (short) (Math.sin(2D * Math.PI * frequency * index / 48_000D) * 20_000D);
        }
        return samples;
    }
}
