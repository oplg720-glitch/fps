package net.opl.fpsstabilizer;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Fully automatic client-side controller.
 * Applies a conservative profile when a world is entered, then adjusts view and
 * simulation distance with hysteresis to avoid constant setting changes.
 */
public final class AdaptiveController {
    private static final Logger LOGGER = Logger.getLogger("FPS Stabilizer");

    private static final int CHECK_EVERY_TICKS = 40;       // about every 2 seconds
    private static final int LOW_WINDOWS_TO_REDUCE = 2;   // require sustained slowdown
    private static final int GOOD_WINDOWS_TO_INCREASE = 9; // recover slowly, not immediately

    private static final int START_RENDER_DISTANCE_CAP = 10;
    private static final int MAX_RENDER_DISTANCE_CAP = 16;
    private static final int MIN_RENDER_DISTANCE = 5;
    private static final int START_SIMULATION_DISTANCE_CAP = 6;
    private static final int MIN_SIMULATION_DISTANCE = 4;

    private static Map<String, Object> originalSettings;
    private static boolean active;
    private static int ticks;
    private static int lowWindows;
    private static int goodWindows;
    private static int minRenderDistance;
    private static int maxRenderDistance;
    private static int minSimulationDistance;
    private static int maxSimulationDistance;

    private AdaptiveController() {}

    /** Called from END_CLIENT_TICK. */
    public static void tick(Object client) {
        boolean inWorld = OptionAccess.isInWorld(client);

        if (inWorld && !active) {
            start(client);
        } else if (!inWorld && active) {
            stop(client);
            return;
        }

        if (!active) return;

        ticks++;
        if (ticks % CHECK_EVERY_TICKS != 0) return;

        FrameTimeTracker.Stats stats = FrameTimeTracker.getStats();
        if (stats.samples() < 30 || stats.averageFps() <= 0.0 || stats.onePercentLow() <= 0.0) return;

        int currentRenderDistance = OptionAccess.getInt(client, "renderDistance", -1);
        int currentSimulationDistance = OptionAccess.getInt(client, "simulationDistance", -1);
        if (currentRenderDistance < 0) return;

        int targetFps = getAdaptiveTarget(client);
        boolean sustainedSlowdown = stats.averageFps() < targetFps * 0.76
            || stats.onePercentLow() < targetFps * 0.42;
        boolean comfortablyFast = stats.averageFps() >= targetFps * 0.96
            && stats.onePercentLow() >= targetFps * 0.66;

        if (sustainedSlowdown) {
            lowWindows++;
            goodWindows = 0;
        } else if (comfortablyFast) {
            goodWindows++;
            lowWindows = 0;
        } else {
            lowWindows = 0;
            goodWindows = 0;
        }

        if (lowWindows >= LOW_WINDOWS_TO_REDUCE) {
            if (currentRenderDistance > minRenderDistance) {
                OptionAccess.setRenderDistance(client, currentRenderDistance - 1);
                LOGGER.fine(() -> "Reduced render distance to " + (currentRenderDistance - 1));
            } else if (currentSimulationDistance > minSimulationDistance) {
                OptionAccess.setSimulationDistance(client, currentSimulationDistance - 1);
                LOGGER.fine(() -> "Reduced simulation distance to " + (currentSimulationDistance - 1));
            }
            lowWindows = 0;
            return;
        }

        if (goodWindows >= GOOD_WINDOWS_TO_INCREASE) {
            if (currentRenderDistance < maxRenderDistance) {
                OptionAccess.setRenderDistance(client, currentRenderDistance + 1);
                LOGGER.fine(() -> "Raised render distance to " + (currentRenderDistance + 1));
            } else if (currentSimulationDistance >= 0 && currentSimulationDistance < maxSimulationDistance) {
                OptionAccess.setSimulationDistance(client, currentSimulationDistance + 1);
                LOGGER.fine(() -> "Raised simulation distance to " + (currentSimulationDistance + 1));
            }
            goodWindows = 0;
        }
    }

    /** Restores the user's original video options when leaving the world or closing Minecraft. */
    public static void stop(Object client) {
        if (!active) return;
        OptionAccess.restore(client, originalSettings);
        originalSettings = null;
        active = false;
        resetCounters();
        LOGGER.info("Automatic profile stopped; original video settings restored.");
    }

    private static void start(Object client) {
        originalSettings = OptionAccess.capture(client);

        int originalRenderDistance = OptionAccess.getInt(client, "renderDistance", START_RENDER_DISTANCE_CAP);
        int originalSimulationDistance = OptionAccess.getInt(client, "simulationDistance", START_SIMULATION_DISTANCE_CAP);

        minRenderDistance = Math.min(MIN_RENDER_DISTANCE, Math.max(2, originalRenderDistance));
        maxRenderDistance = Math.max(minRenderDistance,
            Math.min(MAX_RENDER_DISTANCE_CAP, originalRenderDistance));
        maxSimulationDistance = Math.max(2, Math.min(8, originalSimulationDistance));
        minSimulationDistance = Math.min(MIN_SIMULATION_DISTANCE, maxSimulationDistance);

        // The initial cap is deliberately moderate. The controller can recover one step at a time.
        int initialRenderDistance = Math.min(originalRenderDistance, START_RENDER_DISTANCE_CAP);
        int initialSimulationDistance = Math.min(originalSimulationDistance, START_SIMULATION_DISTANCE_CAP);
        OptionAccess.applyAutomaticBaseline(client, initialRenderDistance, initialSimulationDistance);

        active = true;
        resetCounters();
        LOGGER.info("Automatic profile enabled; adaptive FPS and frame-time monitoring started.");
    }

    private static int getAdaptiveTarget(Object client) {
        // Respect FPS caps and VSync, otherwise use a practical 90 FPS target.
        int configuredLimit = OptionAccess.getInt(client, "framerateLimit", 0);
        boolean vsync = OptionAccess.getBoolean(client, "enableVsync", false);
        int monitorRate = OptionAccess.getWindowRefreshRate(client);

        if (vsync) {
            int effectiveRefreshRate = monitorRate > 0 ? monitorRate : 60;
            int syncTarget = Math.max(30, Math.min(120, effectiveRefreshRate));
            if (configuredLimit > 0 && configuredLimit < 240) {
                return Math.min(syncTarget, Math.max(30, configuredLimit));
            }
            return syncTarget;
        }

        if (configuredLimit <= 0 || configuredLimit >= 240) return 90;
        return Math.max(30, Math.min(120, configuredLimit));
    }

    private static void resetCounters() {
        ticks = 0;
        lowWindows = 0;
        goodWindows = 0;
    }
}
