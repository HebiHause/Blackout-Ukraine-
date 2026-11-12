package ua.blackout.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record DaySchedule(LocalDate date, String status, List<Slot> slots) {

    public DaySchedule {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(slots, "slots must not be null");
        slots = List.copyOf(slots);
    }

    public int totalOutageMinutes() {
        int total = 0;
        for (Slot slot : slots) {
            if (slot.isOutage()) {
                total += slot.duration();
            }
        }
        return total;
    }
}