package model;

/**
 * Represents a fixed time slot — the single source of truth for all batch schedules.
 * Loaded from the 'timeslots' MongoDB collection.
 */
public class Timeslot {
    private String id;          // e.g. "TS1"
    private String label;       // e.g. "06:00 – 07:30"
    private int startHour;
    private int startMin;
    private int endHour;
    private int endMin;
    private int durationMins;

    public Timeslot() {}

    public Timeslot(String id, String label, int startHour, int startMin,
                    int endHour, int endMin, int durationMins) {
        this.id = id;
        this.label = label;
        this.startHour = startHour;
        this.startMin = startMin;
        this.endHour = endHour;
        this.endMin = endMin;
        this.durationMins = durationMins;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getStartHour() { return startHour; }
    public void setStartHour(int startHour) { this.startHour = startHour; }

    public int getStartMin() { return startMin; }
    public void setStartMin(int startMin) { this.startMin = startMin; }

    public int getEndHour() { return endHour; }
    public void setEndHour(int endHour) { this.endHour = endHour; }

    public int getEndMin() { return endMin; }
    public void setEndMin(int endMin) { this.endMin = endMin; }

    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }

    /** Sort key: minutes from midnight */
    public int getStartTotalMins() {
        return startHour * 60 + startMin;
    }

    @Override
    public String toString() {
        return label != null ? label : id;
    }
}
