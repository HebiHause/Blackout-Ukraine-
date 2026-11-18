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

    @Test
    void returnsMinutesUntilPowerReturnsDuringOutage() {
        List<Slot> slots = List.of(new Slot(600, 720, SlotType.DEFINITE));

        assertEquals(70, calculator.minutesUntilPowerReturns(slots, 650));
    }

    @Test
    void powerReturnsIsMinusOneWhenNoOutageNow() {
        List<Slot> slots = List.of(new Slot(600, 720, SlotType.DEFINITE));

        assertEquals(-1, calculator.minutesUntilPowerReturns(slots, 720));
    }

    @Test
    void countdownShowsPowerReturnDuringOutage() {
        List<Slot> today = List.of(new Slot(600, 720, SlotType.DEFINITE));

        assertEquals(new OutageCalculator.Countdown(true, 70),
                calculator.countdown(today, List.of(), 650));
    }

    @Test
    void countdownShowsNextOutageToday() {
        List<Slot> today = List.of(new Slot(600, 720, SlotType.DEFINITE));

        assertEquals(new OutageCalculator.Countdown(false, 60),
                calculator.countdown(today, List.of(), 540));
    }

    @Test
    void countdownLooksAtTomorrowWhenTodayIsOver() {
        List<Slot> today = List.of(new Slot(600, 720, SlotType.DEFINITE));
        List<Slot> tomorrow = List.of(new Slot(120, 240, SlotType.DEFINITE));

        // now = 23:00 (1380): 60 min to midnight + 120 min after it
        assertEquals(new OutageCalculator.Countdown(false, 180),
                calculator.countdown(today, tomorrow, 1380));
    }

    @Test
    void countdownCountsOutageStartingAtMidnight() {
        List<Slot> tomorrow = List.of(new Slot(0, 240, SlotType.DEFINITE));

        assertEquals(new OutageCalculator.Countdown(false, 60),
                calculator.countdown(List.of(), tomorrow, 1380));
    }

    @Test
    void countdownJoinsOutageAcrossMidnight() {
        List<Slot> today = List.of(new Slot(1320, 1440, SlotType.DEFINITE));
        List<Slot> tomorrow = List.of(
                new Slot(0, 120, SlotType.DEFINITE),
                new Slot(120, 180, SlotType.DEFINITE),
                new Slot(300, 400, SlotType.DEFINITE));

        // now = 23:00: 40 min today + 180 min tomorrow
        assertEquals(new OutageCalculator.Countdown(true, 220),
                calculator.countdown(today, tomorrow, 1400));
    }

    @Test
    void countdownIsMinusOneWhenNothingPlanned() {
        assertEquals(new OutageCalculator.Countdown(false, -1),
                calculator.countdown(List.of(), List.of(), 540));
    }
}
