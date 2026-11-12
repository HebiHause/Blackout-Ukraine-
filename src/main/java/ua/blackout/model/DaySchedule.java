package ua.blackout.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record DaySchedule(LocalDate date, List<Slot> slots) {
    public DaySchedule {
        Objects.requireNonNull(date, "date must be not null");
        Objects.requireNonNull(slots, "slots must not be null");
        slots = List.copyOf(slots);
    }

}