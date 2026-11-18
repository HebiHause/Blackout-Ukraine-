package ua.blackout.service;

import ua.blackout.model.Slot;
import ua.blackout.model.SlotType;

import java.util.List;

public class OutageCalculator {

    public int minutesUntilNextOutage(List<Slot> slots, int now){
        int best = -1;
        for (Slot slot : slots){
            if (slot.type() != SlotType.DEFINITE){
                continue;
            }
            if (slot.startMinute() > now) {
                int left = slot.startMinute() - now;
                if (best == -1 || left < best){
                    best = left;
                }
            }
        }
        return best;
    }

    public int minutesUntilPowerReturns(List<Slot> slots, int now){
        for (Slot slot : slots){
            if (slot.isOutage() && slot.isActive(now)){
                return  slot.endMinute() - now;
            }
        }
        return -1;
    }

    // Countdown for /next. Looks at today first, then tomorrow, so the bot does not say
    // "nothing planned" when the next outage starts right after midnight.
    // tomorrow may be empty when YASNO has not published it yet.
    public Countdown countdown(List<Slot> today, List<Slot> tomorrow, int now) {
        int back = minutesUntilPowerReturns(today, now);
        if (back >= 0) {
            if (now + back == MINUTES_PER_DAY) {
                // The outage runs to midnight: YASNO splits it into two days, so add
                // the part that continues from 00:00 tomorrow.
                back += outageFromMidnight(tomorrow);
            }
            return new Countdown(true, back);
        }
        int next = minutesUntilNextOutage(today, now);
        if (next >= 0) {
            return new Countdown(false, next);
        }
        // -1 instead of 0 so that an outage starting exactly at 00:00 is counted too.
        int nextTomorrow = minutesUntilNextOutage(tomorrow, -1);
        if (nextTomorrow >= 0) {
            return new Countdown(false, MINUTES_PER_DAY - now + nextTomorrow - 1);
        }
        return new Countdown(false, -1);
    }

    // Length of the outage that starts at 00:00, following slots that touch each other.
    int outageFromMidnight(List<Slot> slots) {
        int end = 0;
        boolean moved = true;
        while (moved) {
            moved = false;
            for (Slot slot : slots) {
                if (slot.isOutage() && slot.startMinute() == end) {
                    end = slot.endMinute();
                    moved = true;
                }
            }
        }
        return end;
    }

    private static final int MINUTES_PER_DAY = 1440;

    // outageNow = true: minutes until power returns; false: minutes until the next outage.
    // minutes = -1 when there is nothing to count down to.
    public record Countdown(boolean outageNow, int minutes) {
    }
}
