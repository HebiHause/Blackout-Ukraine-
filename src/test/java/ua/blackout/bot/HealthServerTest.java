package ua.blackout.bot;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthServerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void freshScheduleGives200() throws Exception {
        assertEquals(200, statusFor(NOW.minus(Duration.ofMinutes(5))));
    }

    @Test
    void staleScheduleGives503() throws Exception {
        assertEquals(503, statusFor(NOW.minus(Duration.ofHours(2))));
    }

    @Test
    void noScheduleYetGives503() throws Exception {
        assertEquals(503, statusFor(null));
    }

    // Port 0 lets the OS pick a free port, so tests never clash.
    private int statusFor(Instant lastUpdated) throws Exception {
        try (HealthServer server = new HealthServer(0, () -> lastUpdated, CLOCK)) {
            server.start();
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + server.port() + "/health")).build();
            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        }
    }
}
