package ua.blackout.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YasnoResponseTest {

    @Test
    void parsesAllSixtyQueues() throws Exception {
        Map<String, YasnoQueueDto> queues = loadSample();

        assertEquals(60, queues.size());
        assertTrue(queues.containsKey("1.1"));
        assertTrue(queues.containsKey("60.1"));
    }

    @Test
    void parsesTodaySlotsOfQueueOne() throws Exception {
        YasnoQueueDto queue = loadSample().get("1.1");

        assertEquals(5, queue.today().slots().size());
        assertEquals("Definite", queue.today().slots().get(1).type());
        assertEquals("ScheduleApplies", queue.today().status());
    }

    @Test
    void tomorrowIsEmptyInSample() throws Exception {
        YasnoQueueDto queue = loadSample().get("1.1");

        assertEquals(0, queue.tomorrow().slots().size());
        assertEquals("WaitingForSchedule", queue.tomorrow().status());
    }

    private Map<String, YasnoQueueDto> loadSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/yasno-sample.json")) {
            assertNotNull(in, "yasno-sample.json not found in src/test/resources");
            return new ObjectMapper().readValue(in,
                    new TypeReference<Map<String, YasnoQueueDto>>() {});
        }
    }
}