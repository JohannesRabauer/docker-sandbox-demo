package dev.rabauer.racing_telemetry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RacingTelemetryApplicationTests {

    @Autowired
    private TelemetryService telemetryService;

    @Test
    void contextLoads() {
    }

    @Test
    void telemetryServiceBeanIsAvailableAndFunctional() {
        assertThat(telemetryService).isNotNull();

        TelemetryPoint point = telemetryService.next();

        assertThat(point).isNotNull();
        assertThat(point.speed()).isBetween(0.0, 350.0);
        assertThat(point.throttle()).isBetween(0.0, 100.0);
        assertThat(point.brake()).isBetween(0.0, 100.0);
        assertThat(point.timestamp()).isPositive();
    }
}
