package ua.blackout.service;

import ua.blackout.model.DaySchedule;

import java.util.List;

public interface OutageSource {

    List<DaySchedule> getSchedules(String queue);
}