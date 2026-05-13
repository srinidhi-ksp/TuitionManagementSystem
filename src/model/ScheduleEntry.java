package model;

/**
 * Represents one {day, timeslotId} entry in a batch's schedule array.
 * Used in the updated batch schema: schedule: [{day:"TUE", timeslotId:"TS1"}, ...]
 */
public class ScheduleEntry {
    private String day;        // "MON", "TUE", ... "SUN"
    private String timeslotId; // "TS1" ... "TS8"

    public ScheduleEntry() {}

    public ScheduleEntry(String day, String timeslotId) {
        this.day = day;
        this.timeslotId = timeslotId;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getTimeslotId() { return timeslotId; }
    public void setTimeslotId(String timeslotId) { this.timeslotId = timeslotId; }

    @Override
    public String toString() {
        return day + " @ " + timeslotId;
    }
}
