package com.indianmusictiles.game.engine;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class BeatAnalyzer {
    private static final String TAG = "BeatAnalyzer";
    private static final int FFT_SIZE = 1024;
    private static final int HOP_SIZE = 512;
    private static final float ONSET_THRESHOLD_MULTIPLIER = 1.5f;
    private static final int ONSET_MEDIAN_WINDOW = 10;

    public interface AnalysisCallback {
        void onProgress(int percent);
        void onComplete(BeatMap beatMap);
        void onError(String error);
    }

    private final Context context;

    public BeatAnalyzer(Context context) {
        this.context = context;
    }

    public void analyze(Uri audioUri, AnalysisCallback callback) {
        new Thread(() -> {
            try {
                float[] samples = decodeToPCM(audioUri, callback);
                if (samples == null || samples.length == 0) {
                    callback.onError("Failed to decode audio");
                    return;
                }

                callback.onProgress(40);

                List<Float> onsets = detectOnsets(samples, 44100);
                callback.onProgress(70);

                float bpm = estimateBPM(onsets);
                callback.onProgress(85);

                List<Float> beats = quantizeBeats(onsets, bpm);
                callback.onProgress(95);

                BeatMap beatMap = new BeatMap(bpm, beats);
                callback.onProgress(100);
                callback.onComplete(beatMap);

            } catch (Exception e) {
                Log.e(TAG, "Analysis failed", e);
                callback.onError("Analysis failed: " + e.getMessage());
            }
        }).start();
    }

    private float[] decodeToPCM(Uri uri, AnalysisCallback callback) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, uri, null);

        int audioTrack = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrack = i;
                format = trackFormat;
                break;
            }
        }

        if (audioTrack < 0 || format == null) {
            throw new IOException("No audio track found");
        }

        extractor.selectTrack(audioTrack);
        String mime = format.getString(MediaFormat.KEY_MIME);
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        List<Float> pcmSamples = new ArrayList<>();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean outputDone = false;
        long totalDuration = format.containsKey(MediaFormat.KEY_DURATION)
                ? format.getLong(MediaFormat.KEY_DURATION) : 0;

        while (!outputDone) {
            if (!inputDone) {
                int inputIndex = codec.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTime = extractor.getSampleTime();
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTime, 0);
                        extractor.advance();

                        if (totalDuration > 0) {
                            int progress = (int) (presentationTime * 30 / totalDuration);
                            callback.onProgress(Math.min(progress, 30));
                        }
                    }
                }
            }

            int outputIndex = codec.dequeueOutputBuffer(info, 10000);
            if (outputIndex >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                outputBuffer.order(ByteOrder.LITTLE_ENDIAN);

                while (outputBuffer.remaining() >= 2) {
                    short sample = outputBuffer.getShort();
                    pcmSamples.add(sample / 32768.0f);

                    // Skip extra channels (convert to mono)
                    for (int c = 1; c < channels; c++) {
                        if (outputBuffer.remaining() >= 2) {
                            outputBuffer.getShort();
                        }
                    }
                }

                codec.releaseOutputBuffer(outputIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }

        codec.stop();
        codec.release();
        extractor.release();

        float[] result = new float[pcmSamples.size()];
        for (int i = 0; i < pcmSamples.size(); i++) {
            result[i] = pcmSamples.get(i);
        }
        return result;
    }

    private List<Float> detectOnsets(float[] samples, int sampleRate) {
        FFT fft = new FFT(FFT_SIZE);
        List<Float> spectralFlux = new ArrayList<>();
        float[] previousMagnitude = null;

        int totalFrames = (samples.length - FFT_SIZE) / HOP_SIZE;

        for (int i = 0; i + FFT_SIZE <= samples.length; i += HOP_SIZE) {
            float[] re = new float[FFT_SIZE];
            float[] im = new float[FFT_SIZE];

            // Apply Hanning window
            for (int j = 0; j < FFT_SIZE; j++) {
                float window = 0.5f * (1 - (float) Math.cos(2 * Math.PI * j / (FFT_SIZE - 1)));
                re[j] = samples[i + j] * window;
                im[j] = 0;
            }

            fft.fft(re, im);
            float[] magnitude = fft.getMagnitude(re, im);

            if (previousMagnitude != null) {
                float flux = 0;
                for (int j = 0; j < magnitude.length; j++) {
                    float diff = magnitude[j] - previousMagnitude[j];
                    if (diff > 0) {
                        flux += diff;
                    }
                }
                spectralFlux.add(flux);
            }

            previousMagnitude = magnitude;
        }

        // Peak picking with adaptive threshold
        List<Float> onsets = new ArrayList<>();
        for (int i = ONSET_MEDIAN_WINDOW; i < spectralFlux.size() - ONSET_MEDIAN_WINDOW; i++) {
            float median = getMedian(spectralFlux, i - ONSET_MEDIAN_WINDOW, i + ONSET_MEDIAN_WINDOW);
            float threshold = median * ONSET_THRESHOLD_MULTIPLIER;

            if (spectralFlux.get(i) > threshold &&
                spectralFlux.get(i) > spectralFlux.get(i - 1) &&
                spectralFlux.get(i) > spectralFlux.get(i + 1)) {

                float timeSeconds = (float) i * HOP_SIZE / sampleRate;
                // Minimum onset spacing: 100ms
                if (onsets.isEmpty() || timeSeconds - onsets.get(onsets.size() - 1) > 0.1f) {
                    onsets.add(timeSeconds);
                }
            }
        }

        return onsets;
    }

    private float getMedian(List<Float> data, int start, int end) {
        List<Float> window = new ArrayList<>();
        for (int i = start; i <= end && i < data.size(); i++) {
            window.add(data.get(i));
        }
        window.sort(Float::compare);
        return window.get(window.size() / 2);
    }

    private float estimateBPM(List<Float> onsets) {
        if (onsets.size() < 2) return 120f;

        // Calculate inter-onset intervals
        List<Float> intervals = new ArrayList<>();
        for (int i = 1; i < onsets.size(); i++) {
            float interval = onsets.get(i) - onsets.get(i - 1);
            if (interval > 0.2f && interval < 2.0f) {
                intervals.add(interval);
            }
        }

        if (intervals.isEmpty()) return 120f;

        // Find most common interval using histogram
        float minInterval = 60f / 200f; // 200 BPM max
        float maxInterval = 60f / 60f;  // 60 BPM min
        int bins = 100;
        int[] histogram = new int[bins];

        for (float interval : intervals) {
            if (interval >= minInterval && interval <= maxInterval) {
                int bin = (int) ((interval - minInterval) / (maxInterval - minInterval) * (bins - 1));
                bin = Math.max(0, Math.min(bins - 1, bin));
                histogram[bin]++;
            }
        }

        int bestBin = 0;
        for (int i = 1; i < bins; i++) {
            if (histogram[i] > histogram[bestBin]) {
                bestBin = i;
            }
        }

        float bestInterval = minInterval + (bestBin * (maxInterval - minInterval) / (bins - 1));
        float bpm = 60f / bestInterval;

        // Snap to common BPM ranges
        bpm = Math.round(bpm);
        return Math.max(60, Math.min(200, bpm));
    }

    private List<Float> quantizeBeats(List<Float> onsets, float bpm) {
        if (onsets.isEmpty()) return onsets;

        float beatInterval = 60f / bpm;
        List<Float> quantized = new ArrayList<>();

        for (float onset : onsets) {
            float nearestBeat = Math.round(onset / beatInterval) * beatInterval;
            // Only add if not too close to previous
            if (quantized.isEmpty() || nearestBeat - quantized.get(quantized.size() - 1) > beatInterval * 0.4f) {
                quantized.add(nearestBeat);
            }
        }

        // Fill in any gaps (add beats where none detected)
        List<Float> filled = new ArrayList<>();
        for (int i = 0; i < quantized.size() - 1; i++) {
            filled.add(quantized.get(i));
            float gap = quantized.get(i + 1) - quantized.get(i);
            if (gap > beatInterval * 2.5f) {
                int missingBeats = Math.round(gap / beatInterval) - 1;
                for (int j = 1; j <= missingBeats; j++) {
                    filled.add(quantized.get(i) + j * beatInterval);
                }
            }
        }
        filled.add(quantized.get(quantized.size() - 1));

        return filled;
    }
}
