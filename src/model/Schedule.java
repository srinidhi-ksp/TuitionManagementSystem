package model;

/**
 * Schedule model for Batch timings.
 * Supports both legacy (day+start+end) and new (day+timeslotId) formats.
 */
public class Schedule {
    private String day;
    private String start;
    private String end;
    /** Optional: links this schedule entry to a Timeslot document. */
    private String timeslotId;

    public Schedule() {}

    public Schedule(String day, String start, String end) {
        this.day = day;
        this.start = start;
        this.end = end;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getTimeslotId() { return timeslotId; }
    public void setTimeslotId(String timeslotId) { this.timeslotId = timeslotId; }

    @Override
    public String toString() {
        return day + " " + start + " - " + end;
    }
}
