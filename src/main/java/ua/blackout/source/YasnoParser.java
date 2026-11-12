package ua.blackout.source;

import ua.blackout.model.DaySchedule;
import ua.blackout.model.Slot;
import ua.blackout.model.SlotType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class YasnoParser {

    private YasnoParser() {
    }

    public static List<DaySchedule> toSchedules(YasnoQueueDto queue) {
        List<DaySchedule> result = new ArrayList<>();
        result.add(toDay(queue.today()));
        result.add(toDay(queue.tomorrow()));
        return result;
    }

    private static DaySchedule toDay(DayDto day) {
        List<Slot> slots = new ArrayList<>();
        for (SlotDto dto : day.slots()) {
            slots.add(new Slot(dto.start(), dto.end(), SlotType.fromApi(dto.type())));
        }
        return new DaySchedule(
                OffsetDateTime.parse(day.date()).toLocalDate(),
                day.status(),
                slots);
    }
}