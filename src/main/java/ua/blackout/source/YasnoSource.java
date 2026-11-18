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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class YasnoSource implements OutageSource {

    static final String URL =
            "https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/25/dsos/902/planned-outages";

    // Fixed moments in Kyiv time when the source is allowed to hit YASNO.
    static final List<LocalTime> FETCH_TIMES = List.of(
            LocalTime.of(6, 0),
            LocalTime.of(14, 0),
            LocalTime.of(20, 0));

    static final Duration PUBLISH_DELAY = Duration.ofMinutes(10);

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

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

    // Returns cached data. Calls YASNO only if a fetch moment (plus delay)
    // has passed since the last attempt, successful or not.
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

    // True if at least one fetch moment (plus PUBLISH_DELAY) has passed since the last attempt.
    private boolean isFetchDue() {
        if (lastAttemptAt == null) {
            return true;
        }
        ZonedDateTime last = lastAttemptAt.atZone(KYIV);
        ZonedDateTime now = ZonedDateTime.now(KYIV);
        for (LocalTime time : FETCH_TIMES) {
            ZonedDateTime moment = now.toLocalDate().atTime(time).atZone(KYIV)
                    .plus(PUBLISH_DELAY);
            if (moment.isAfter(last) && !moment.isAfter(now)) {
                return true;
            }
        }
        return false;
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