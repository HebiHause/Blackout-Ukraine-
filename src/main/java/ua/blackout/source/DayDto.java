package ua.blackout.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DayDto(List<SlotDto> slots, String date, String status) {
}