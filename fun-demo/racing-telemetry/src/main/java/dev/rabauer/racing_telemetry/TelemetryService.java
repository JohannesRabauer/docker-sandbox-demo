package dev.rabauer.racing_telemetry;

import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Generates a continuous stream of fake racing telemetry via sine-wave simulation.
 *
 * <p>Calling {@link #next()} advances the internal tick counter and returns the next point.
 * The generator is thread-safe so multiple UI sessions can share a single bean.
 */
@Service
public class TelemetryService {

    // Seeded for reproducible noise across restarts
    private final Random random = new Random(42);
    private long tick = 0;

    /**
     * Returns the next simulated telemetry point.
     *
     * <p>Throttle and brake use opposing sine phases so they do not peak simultaneously.
     * Speed follows the throttle/brake balance with inertia applied via a slower sine.
     */
    public synchronized TelemetryPoint next() {
        tick++;
        double t = tick * 0.05;

        // Throttle: primary driving wave + harmonic for variation
        double throttleBase = 0.5 + 0.4 * Math.sin(t * 0.4) + 0.1 * Math.sin(t * 1.7);
        throttleBase = clamp(throttleBase, 0.0, 1.0);
        double throttle = clamp(throttleBase * 100.0 + random.nextGaussian() * 2.0, 0.0, 100.0);

        // Brake: offset by PI and suppressed when throttle is dominant
        double brakeBase = 0.2 + 0.3 * Math.sin(t * 0.4 + Math.PI) * (1.0 - throttleBase);
        brakeBase = clamp(brakeBase, 0.0, 1.0);
        double brake = clamp(brakeBase * 100.0 + random.nextGaussian() * 1.5, 0.0, 100.0);

        // Speed: slower sine representing track layout + throttle/brake influence
        double speedBase = 0.5 + 0.35 * Math.sin(t * 0.2) + (throttleBase - brakeBase * 1.5) * 0.15;
        speedBase = clamp(speedBase, 0.0, 1.0);
        double speed = clamp(speedBase * 350.0 + random.nextGaussian() * 3.0, 0.0, 350.0);

        return new TelemetryPoint(
            System.currentTimeMillis(),
            round1(speed),
            round1(throttle),
            round1(brake)
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
