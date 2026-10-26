package ua.blackout.model;

public class Blackout {
    private final int start;
    private final int end;
    private final int group;

    public Blackout(int start, int end, int group) {
        if (start < 0 || end > 1440) {
            throw new IllegalArgumentException("Minutes must be within 0..1440: " + start + ".." + end);
        }
        if (start >= end) {
            throw new IllegalArgumentException("Start must be before end: " + start + ".." + end);
        }
        this.start = start;
        this.end = end;
        this.group = group;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getGroup(){
        return group;
    }

    public boolean isActive(int now) {
        return start <= now && now < end;
    }

    public int minutesUntilStart(int now){
        return start - now;
    }

    public int duration(){
        return end - start;
    }


}


