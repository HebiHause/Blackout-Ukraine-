package ua.blackout.model;

public record Slot(int startMinute, int endMinute, SlotType type) {

    public Slot {
        if (startMinute < 0 || endMinute > 1440) {
            throw new IllegalArgumentException(
                    "Minutes must be within 0..1440: " + startMinute + ".." + endMinute);
        }
        if (startMinute >= endMinute) {
            throw new IllegalArgumentException(
                    "Start must be before end: " + startMinute + ".." + endMinute);
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }
    }

    public boolean isActive(int now) {
        return startMinute <= now && now < endMinute;
    }

    public int minutesUntilStart(int now){
        return startMinute - now;
    }

    public int duration(){
        return endMinute - startMinute;

    }

    public boolean isOutage() {
        return type == SlotType.DEFINITE;
    }
}
