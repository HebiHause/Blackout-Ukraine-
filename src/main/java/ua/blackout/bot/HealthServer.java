package ua.blackout.bot;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

// GET /health -> 200 while the bot runs and the schedule is fresh, 503 otherwise.
// The server's restart policy uses it: a stuck bot or a schedule that has not
// updated for MAX_AGE means something is wrong.
public class HealthServer implements AutoCloseable {

    // Generous on purpose: a short YASNO outage should not restart the bot,
    // because a restart does not bring YASNO back.
    static final Duration MAX_AGE = Duration.ofHours(1);

    private final HttpServer server;
    private final Supplier<Instant> lastUpdated;
    private final Clock clock;

    // lastUpdated returns the time of the last good schedule (null if none yet).
    public HealthServer(int port, Supplier<Instant> lastUpdated, Clock clock) throws IOException {
        this.lastUpdated = lastUpdated;
        this.clock = clock;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handle);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        int status;
        String body;
        if (!exchange.getRequestMethod().equals("GET")) {
            status = 405;
            body = "method not allowed";
        } else {
            Instant updated = lastUpdated.get();
            if (updated == null) {
                status = 503;
                body = "no schedule yet";
            } else if (Duration.between(updated, clock.instant()).compareTo(MAX_AGE) > 0) {
                status = 503;
                body = "schedule is stale, updated " + updated;
            } else {
                status = 200;
                body = "OK, schedule updated " + updated;
            }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
