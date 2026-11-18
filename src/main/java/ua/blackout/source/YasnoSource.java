package ua.blackout.source;

import ua.blackout.model.DaySchedule;
import ua.blackout.service.OutageSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class YasnoSource implements OutageSource {

    static final String URL =
            "https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/25/dsos/902/planned-outages";

    // YASNO updates the schedule about every 5 minutes (docs/SPEC.md), so the cache
    // lives as long: one request to YASNO per 5 minutes at most, however many users ask.
    static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, YasnoQueueDto> lastGood;
    private Instant lastGoodAt;
    private Instant lastAttemptAt;


    @Override
    public synchronized List<DaySchedule> getSchedules(String queue) {
        Map<String, YasnoQueueDto> data = refresh();
        YasnoQueueDto dto = data.get(queue);
        if (dto == null) {
            return List.of();
        }
        return YasnoParser.toSchedules(dto);
    }

    // Time of the last successful fetch, for the "as of ..." text in the bot.
    public synchronized Instant lastUpdated() {
        return lastGoodAt;
    }

    // Returns cached data. Calls YASNO only when the cache is older than CACHE_TTL.
    private Map<String, YasnoQueueDto> refresh() {
        if (lastGood != null && !isFetchDue()) {
            return lastGood;
        }
        fetch();
        if (lastGood == null) {
            throw new IllegalStateException("no YASNO data yet");
        }
        return lastGood;
    }

    // True if the cache is empty or the last attempt, successful or not, is older than CACHE_TTL.
    // Counting failed attempts too keeps us from hammering YASNO while it is down.
    private boolean isFetchDue() {
        return isFetchDue(lastAttemptAt, Instant.now());
    }

    static boolean isFetchDue(Instant lastAttempt, Instant now) {
        if (lastAttempt == null) {
            return true;
        }
        return !now.isBefore(lastAttempt.plus(CACHE_TTL));
    }

    // Fetches when the cache is stale and returns the time of the last good data (null if none).
    // Used by /health, so a running bot keeps the schedule fresh even when nobody asks.
    public synchronized Instant refreshIfDue() {
        if (isFetchDue()) {
            fetch();
        }
        return lastGoodAt;
    }

    private void fetch() {
        lastAttemptAt = Instant.now();
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                lastGood = mapper.readValue(response.body(),
                        new TypeReference<Map<String, YasnoQueueDto>>() {});
                lastGoodAt = Instant.now();
            } else {
                System.err.println("YASNO HTTP " + response.statusCode());
            }
        } catch (IOException e) {
            System.err.println("YASNO request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}