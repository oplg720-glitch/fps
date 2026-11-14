package net.opl.fpsstabilizer;

import java.util.Arrays;

/** Collects real render-frame timings. Called once for each GameRenderer frame. */
public final class FrameTimeTracker {
    private static final int WINDOW_SIZE = 180;
    private static final long[] FRAME_TIMES_NS = new long[WINDOW_SIZE];
    private static final int PUBLISH_EVERY = 30;

    private static long previousFrameNs;
    private static int cursor;
    private static int sampleCount;
    private static int framesSincePublish;
    private static volatile Stats latest = new Stats(0.0, 0.0, 0);

    private FrameTimeTracker() {}

    public static void onFrame() {
        long now = System.nanoTime();
        if (previousFrameNs != 0L) {
            long delta = now - previousFrameNs;
            // Ignore pauses, loading screens and invalid timings so they do not poison the window.
            if (delta >= 1_000_000L && delta <= 1_000_000_000L) {
                FRAME_TIMES_NS[cursor] = delta;
                cursor = (cursor + 1) % WINDOW_SIZE;
                sampleCount = Math.min(sampleCount + 1, WINDOW_SIZE);
                framesSincePublish++;
                if (framesSincePublish >= PUBLISH_EVERY && sampleCount >= 30) {
                    publishStats();
                    framesSincePublish = 0;
                }
            }
        }
        previousFrameNs = now;
    }

    private static void publishStats() {
        long[] copy = new long[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            copy[i] = FRAME_TIMES_NS[i];
        }
        Arrays.sort(copy);

        double total = 0.0;
        for (long frameTime : copy) {
            total += frameTime;
        }

        double averageFrameMs = (total / copy.length) / 1_000_000.0;
        int p99Index = Math.min(copy.length - 1, (int) Math.ceil(copy.length * 0.99) - 1);
        double p99FrameMs = copy[p99Index] / 1_000_000.0;
        double averageFps = averageFrameMs <= 0.0 ? 0.0 : 1000.0 / averageFrameMs;
        double onePercentLow = p99FrameMs <= 0.0 ? 0.0 : 1000.0 / p99FrameMs;

        latest = new Stats(averageFps, onePercentLow, sampleCount);
    }

    public static Stats getStats() {
        return latest;
    }

    public record Stats(double averageFps, double onePercentLow, int samples) {}
}
