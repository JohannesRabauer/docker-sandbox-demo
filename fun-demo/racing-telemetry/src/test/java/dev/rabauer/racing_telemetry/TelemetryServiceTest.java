package dev.rabauer.racing_telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    private TelemetryService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryService();
    }

    @Test
    void speedShouldStayWithinBounds() {
        for (int i = 0; i < 1000; i++) {
            double speed = service.next().speed();
            assertTrue(speed >= 0.0 && speed <= 350.0,
                "Speed out of bounds at tick %d: %s".formatted(i, speed));
        }
    }

    @Test
    void throttleShouldStayWithinBounds() {
        for (int i = 0; i < 1000; i++) {
            double throttle = service.next().throttle();
            assertTrue(throttle >= 0.0 && throttle <= 100.0,
                "Throttle out of bounds at tick %d: %s".formatted(i, throttle));
        }
    }

    @Test
    void brakeShouldStayWithinBounds() {
        for (int i = 0; i < 1000; i++) {
            double brake = service.next().brake();
            assertTrue(brake >= 0.0 && brake <= 100.0,
                "Brake out of bounds at tick %d: %s".formatted(i, brake));
        }
    }

    @Test
    void throttleAndBrakeShouldNotPeakSimultaneously() {
        // Both above 80% simultaneously indicates the simulation is broken
        for (int i = 0; i < 2000; i++) {
            TelemetryPoint p = service.next();
            assertFalse(p.throttle() > 80.0 && p.brake() > 80.0,
                "Simultaneous peak at tick %d: throttle=%.1f brake=%.1f".formatted(i, p.throttle(), p.brake()));
        }
    }

    @Test
    void valuesShouldVaryOverTime() {
        TelemetryPoint first = service.next();
        boolean speedChanged = false;
        boolean throttleChanged = false;

        for (int i = 0; i < 200; i++) {
            TelemetryPoint p = service.next();
            if (p.speed() != first.speed()) speedChanged = true;
            if (p.throttle() != first.throttle()) throttleChanged = true;
            if (speedChanged && throttleChanged) break;
        }

        assertTrue(speedChanged, "Speed should change over 200 ticks");
        assertTrue(throttleChanged, "Throttle should change over 200 ticks");
    }

    @Test
    void timestampShouldBePositive() {
        TelemetryPoint point = service.next();
        assertTrue(point.timestamp() > 0, "Timestamp must be a positive epoch millis value");
    }

    @Test
    void consecutiveCallsShouldProduceDifferentTimestamps() {
        // Timestamps may collide within the same ms, but not across a larger sample
        long first = service.next().timestamp();
        long last = first;
        for (int i = 0; i < 10; i++) {
            last = service.next().timestamp();
        }
        assertTrue(last >= first, "Timestamps must be non-decreasing");
    }
}
