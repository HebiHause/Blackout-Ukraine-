package ua.blackout.service;

import ua.blackout.model.Slot;
import ua.blackout.model.SlotType;
import org.junit.jupiter.api.Test;
import ua.blackout.service.OutageCalculator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutageCalculatorTest {

    private final OutageCalculator calculator = new OutageCalculator();

    @Test
    void returnsMinutesUntilNextDefiniteOutage() {
        List<Slot> slots = List.of(
                new Slot(600, 720, SlotType.DEFINITE),
                new Slot(900, 1020, SlotType.DEFINITE));

        // now = 09:00 (540), next outage starts at 600
        assertEquals(60, calculator.minutesUntilNextOutage(slots, 540));
    }

    @Test
    void ignoresSlotsThatAreNotDefinite() {
        List<Slot> slots = List.of(
                new Slot(600, 720, SlotType.NOT_PLANNED),
                new Slot(900, 1020, SlotType.DEFINITE));

        assertEquals(360, calculator.minutesUntilNextOutage(slots, 540));
    }

    @Test
    void returnsMinusOneWhenNothingLeftToday() {
        List<Slot> slots = List.of(
                new Slot(600, 720, SlotType.DEFINITE));

        assertEquals(-1, calculator.minutesUntilNextOutage(slots, 800));
    }

    @Test
    void returnsMinusOneForEmptyDay() {
        assertEquals(-1, calculator.minutesUntilNextOutage(List.of(), 540));
    }
}