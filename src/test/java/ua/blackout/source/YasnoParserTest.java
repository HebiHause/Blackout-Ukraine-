package ua.blackout.source;

import ua.blackout.model.DaySchedule;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YasnoParserTest {

    @Test
    void todayOfQueueOneHasExpectedDateAndOutageMinutes() throws Exception {
        List<DaySchedule> days = YasnoParser.toSchedules(loadSample().get("1.1"));

        DaySchedule today = days.get(0);
        assertEquals(LocalDate.of(2026, 11, 13), today.date());
        assertEquals(720, today.totalOutageMinutes());
    }

    @Test
    void tomorrowHasNoSlotsInSample() throws Exception {
        List<DaySchedule> days = YasnoParser.toSchedules(loadSample().get("1.1"));

        DaySchedule tomorrow = days.get(1);
        assertEquals(LocalDate.of(2026, 11, 14), tomorrow.date());
        assertTrue(tomorrow.slots().isEmpty());
    }

    private Map<String, YasnoQueueDto> loadSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/yasno-sample.json")) {
            return new ObjectMapper().readValue(in,
                    new TypeReference<Map<String, YasnoQueueDto>>() {});
        }
    }
}