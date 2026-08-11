package de.minecraftmodmaker.choicervoicer.audio;

import java.util.Arrays;

public final class SimilarityScorer {
    private static final int FEATURE_BINS = 64;
    private static final int FRAME_SIZE = 1024;

    public Score score(short[] reference, short[] performance, double silenceThreshold) {
        short[] trimmedReference = trimSilence(reference, silenceThreshold);
        short[] trimmedPerformance = trimSilence(performance, silenceThreshold);
        if (trimmedReference.length < 480 || trimmedPerformance.length < 480) {
            return new Score(0D, 0, true);
        }

        double[] referenceEnvelope = envelope(trimmedReference);
        double[] performanceEnvelope = envelope(trimmedPerformance);
        double envelopeSimilarity = cosine(referenceEnvelope, performanceEnvelope);
        double durationRatio = Math.min(trimmedReference.length, trimmedPerformance.length)
                / (double) Math.max(trimmedReference.length, trimmedPerformance.length);
        double pitchShape = 1D - Math.min(1D,
                Math.abs(zeroCrossingRate(trimmedReference) - zeroCrossingRate(trimmedPerformance)) * 4D);
        double spectralShape = 1D - Math.min(1D,
                Math.abs(spectralCentroid(trimmedReference) - spectralCentroid(trimmedPerformance)));

        double similarity = Math.clamp(
                envelopeSimilarity * 0.45D + durationRatio * 0.25D
                        + pitchShape * 0.15D + spectralShape * 0.15D,
                0D, 1D
        );
        int votes = similarity >= 0.90D ? 5
                : similarity >= 0.78D ? 4
                : similarity >= 0.64D ? 3
                : similarity >= 0.50D ? 2
                : similarity >= 0.36D ? 1 : 0;
        return new Score(similarity, votes, false);
    }

    private static short[] trimSilence(short[] samples, double threshold) {
        int absoluteThreshold = (int) (Short.MAX_VALUE * threshold);
        int start = 0;
        while (start < samples.length && Math.abs((int) samples[start]) < absoluteThreshold) {
            start++;
        }
        int end = samples.length;
        while (end > start && Math.abs((int) samples[end - 1]) < absoluteThreshold) {
            end--;
        }
        return Arrays.copyOfRange(samples, start, end);
    }

    private static double[] envelope(short[] samples) {
        double[] result = new double[FEATURE_BINS];
        for (int bin = 0; bin < FEATURE_BINS; bin++) {
            int from = bin * samples.length / FEATURE_BINS;
            int to = Math.max(from + 1, (bin + 1) * samples.length / FEATURE_BINS);
            double energy = 0D;
            for (int index = from; index < Math.min(to, samples.length); index++) {
                double value = samples[index] / (double) Short.MAX_VALUE;
                energy += value * value;
            }
            result[bin] = Math.sqrt(energy / Math.max(1, to - from));
        }
        return result;
    }

    private static double cosine(double[] first, double[] second) {
        double dot = 0D;
        double firstLength = 0D;
        double secondLength = 0D;
        for (int index = 0; index < first.length; index++) {
            dot += first[index] * second[index];
            firstLength += first[index] * first[index];
            secondLength += second[index] * second[index];
        }
        if (firstLength == 0D || secondLength == 0D) {
            return 0D;
        }
        return Math.clamp(dot / Math.sqrt(firstLength * secondLength), 0D, 1D);
    }

    private static double zeroCrossingRate(short[] samples) {
        int crossings = 0;
        for (int index = 1; index < samples.length; index++) {
            if ((samples[index - 1] < 0) != (samples[index] < 0)) {
                crossings++;
            }
        }
        return crossings / (double) Math.max(1, samples.length - 1);
    }

    private static double spectralCentroid(short[] samples) {
        int frames = Math.max(1, samples.length / FRAME_SIZE);
        double weighted = 0D;
        double magnitudeTotal = 0D;
        for (int frame = 0; frame < frames; frame++) {
            int offset = frame * samples.length / frames;
            int available = Math.min(FRAME_SIZE, samples.length - offset);
            for (int bin = 1; bin < 64; bin++) {
                double real = 0D;
                double imaginary = 0D;
                for (int index = 0; index < available; index += 4) {
                    double angle = 2D * Math.PI * bin * index / FRAME_SIZE;
                    double sample = samples[offset + index] / (double) Short.MAX_VALUE;
                    real += sample * Math.cos(angle);
                    imaginary -= sample * Math.sin(angle);
                }
                double magnitude = Math.hypot(real, imaginary);
                weighted += (bin / 63D) * magnitude;
                magnitudeTotal += magnitude;
            }
        }
        return magnitudeTotal == 0D ? 0D : weighted / magnitudeTotal;
    }

    public record Score(double similarity, int judgeVotes, boolean silent) {
        public int points() {
            return judgeVotes * 100 + (int) Math.round(similarity * 100D);
        }
    }
}
