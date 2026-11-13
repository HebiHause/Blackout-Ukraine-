package ua.blackout.service;

import ua.blackout.model.DaySchedule;
import ua.blackout.service.OutageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FakeOutageSource implements OutageSource {

    private final Map<String, List<DaySchedule>> schedules = new HashMap<>();

    public void addSchedule(String queue, DaySchedule schedule) {
        schedules.computeIfAbsent(queue, key -> new ArrayList<>()).add(schedule);
    }

    @Override
    public List<DaySchedule> getSchedules(String queue) {
        return List.copyOf(schedules.getOrDefault(queue, List.of()));
    }
}