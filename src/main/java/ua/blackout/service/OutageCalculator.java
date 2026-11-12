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
}