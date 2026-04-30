package dev.rabauer.racing_telemetry;

/**
 * Immutable snapshot of telemetry signals at a single point in time.
 *
 * @param timestamp epoch millis
 * @param speed     km/h, range 0–350
 * @param throttle  percent, range 0–100
 * @param brake     percent, range 0–100
 */
public record TelemetryPoint(long timestamp, double speed, double throttle, double brake) {
}
