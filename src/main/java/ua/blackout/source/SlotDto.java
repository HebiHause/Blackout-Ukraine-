package ua.blackout.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlotDto(int start, int end, String type) {
}