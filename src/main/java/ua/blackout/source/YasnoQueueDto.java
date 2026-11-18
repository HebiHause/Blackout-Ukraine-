package ua.blackout.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YasnoQueueDto(DayDto today, DayDto tomorrow, String updatedOn) {
}