package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import model.Batch;
import model.Enrollment;
import model.Student;

/**
 * ScheduleConflictValidator: Detects schedule conflicts for teachers and students
 * 
 * KEY FEATURES:
 * ✅ Teacher Conflict Detection: Check if teacher has batch at overlapping time
 * ✅ Student Conflict Detection: Check if student enrolled in batch at overlapping time
 * ✅ Time Comparison Logic: Compare day and time ranges properly
 * ✅ Batch Details: Return conflicting batch info for UI display
 */
public class ScheduleConflictValidator {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────
    // 🔷 TEACHER CONFLICT DETECTION
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Check if teacher has schedule conflict with new batch
     * 
     * @param teacherId Teacher's ID
     * @param newDay Day of week (MON, TUE, WED, THU, FRI, SAT, SUN)
     * @param newStartTime Start time (HH:mm format)
     * @param newEndTime End time (HH:mm format)
     * @param excludeBatchId Exclude this batch ID from comparison (for editing)
     * @return ConflictInfo object with conflict details, or null if no conflict
     */
    public static ConflictInfo checkTeacherConflict(
            String teacherId, 
            String newDay, 
            String newStartTime, 
            String newEndTime,
            Integer excludeBatchId) {
        
        if (teacherId == null || newDay == null || newStartTime == null || newEndTime == null) {
            return null;
        }

        BatchDAO batchDao = new BatchDAO();
        List<Batch> teacherBatches = batchDao.getBatchesByTeacherId(teacherId);

        for (Batch batch : teacherBatches) {
            // Skip the batch being edited
            if (excludeBatchId != null && batch.getBatchId() == excludeBatchId) {
                continue;
            }

            // Skip inactive batches
            if (batch.getStatus() != null && batch.getStatus().equals("INACTIVE")) {
                continue;
            }

            // Parse batch schedule
            String batchDay = extractDayFromTiming(batch.getTiming());
            String batchStartTime = extractStartTimeFromTiming(batch.getTiming());
            String batchEndTime = extractEndTimeFromTiming(batch.getTiming());

            // Check for day match
            if (batchDay != null && batchDay.equalsIgnoreCase(newDay)) {
                // Check for time overlap
                if (isTimeConflict(newStartTime, newEndTime, batchStartTime, batchEndTime)) {
                    return new ConflictInfo(
                        batch.getBatchName(),
                        batchDay,
                        batchStartTime,
                        batchEndTime,
                        batch.getBatchId()
                    );
                }
            }
        }
        return null; // No conflict
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 STUDENT CONFLICT DETECTION
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Check if student is already enrolled in the same batch (DUPLICATE)
     * 
     * @param studentId Student's ID
     * @param batchId Batch ID to check
     * @return Student name if duplicate found, null otherwise
     */
    public static String checkDuplicateEnrollment(String studentId, int batchId) {
        if (studentId == null) {
            return null;
        }

        EnrollmentDAO enrollmentDao = new EnrollmentDAO();
        
        // Check if duplicate exists (student already enrolled in this batch)
        if (enrollmentDao.isDuplicateEnrollment(studentId, batchId)) {
            // Get student name for display
            StudentDAO studentDao = new StudentDAO();
            Student student = studentDao.getStudentById(studentId);
            if (student == null) {
                student = studentDao.getStudentByUserId(studentId);
            }
            
            String studentName = (student != null && student.getName() != null) 
                ? student.getName() 
                : studentId;
            return studentName;
        }
        
        return null; // No duplicate
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 STUDENT CONFLICT DETECTION
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Check if student has schedule conflict with new batch
     * 
     * @param studentId Student's ID
     * @param selectedBatchId Batch to enroll into
     * @return ConflictInfo object with conflict details, or null if no conflict
     */
    public static ConflictInfo checkStudentConflict(String studentId, int selectedBatchId) {
        if (studentId == null) return null;

        BatchDAO batchDao = new BatchDAO();
        Batch selectedBatch = batchDao.getBatchById(selectedBatchId);
        if (selectedBatch == null || selectedBatch.getScheduleEntries() == null) return null;

        // Get student's active enrollments
        EnrollmentDAO enrollmentDao = new EnrollmentDAO();
        List<Enrollment> activeEnrollments = enrollmentDao.getEnrollmentsByStudentId(studentId);

        for (Enrollment enrollment : activeEnrollments) {
            // Don't check the same batch
            if (enrollment.getBatchId() == selectedBatchId) continue;

            Batch existingBatch = batchDao.getBatchById(enrollment.getBatchId());
            if (existingBatch == null || existingBatch.getScheduleEntries() == null) continue;

            // Check for overlap in schedule entries (Day + Timeslot)
            for (model.ScheduleEntry newEntry : selectedBatch.getScheduleEntries()) {
                for (model.ScheduleEntry oldEntry : existingBatch.getScheduleEntries()) {
                    if (newEntry.getDay().equalsIgnoreCase(oldEntry.getDay()) && 
                        newEntry.getTimeslotId().equals(oldEntry.getTimeslotId())) {
                        
                        return new ConflictInfo(
                            existingBatch.getBatchName(),
                            oldEntry.getDay(),
                            "Timeslot: " + oldEntry.getTimeslotId(),
                            "",
                            existingBatch.getBatchId()
                        );
                    }
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 TIME CONFLICT LOGIC
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Check if two time ranges overlap
     * Times in HH:mm format (24-hour)
     * 
     * Conflict condition: (start1 < end2) AND (end1 > start2)
     * 
     * @return true if times overlap, false otherwise
     */
    public static boolean isTimeConflict(
            String start1, String end1,
            String start2, String end2) {
        
        try {
            LocalTime s1 = LocalTime.parse(start1, TIME_FMT);
            LocalTime e1 = LocalTime.parse(end1, TIME_FMT);
            LocalTime s2 = LocalTime.parse(start2, TIME_FMT);
            LocalTime e2 = LocalTime.parse(end2, TIME_FMT);

            // Overlap condition: (s1 < e2) AND (e1 > s2)
            return s1.isBefore(e2) && e1.isAfter(s2);
        } catch (Exception e) {
            System.err.println("[ScheduleConflictValidator] Error parsing times: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 TIMING STRING PARSING
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Extract day from timing string
     * Format: "MON 09:00 - 11:00" or "MON 9:00 AM - 11:00 AM"
     */
    public static String extractDayFromTiming(String timing) {
        if (timing == null || timing.isEmpty()) return null;
        String[] parts = timing.split(" ");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Extract start time from timing string
     * Format: "MON 09:00 - 11:00"
     * Returns: "09:00"
     */
    public static String extractStartTimeFromTiming(String timing) {
        if (timing == null || timing.isEmpty()) return null;
        String[] parts = timing.split(" ");
        if (parts.length < 2) return null;
        
        String timeStr = parts[1];
        // Remove any AM/PM suffix
        timeStr = timeStr.replaceAll("\\s*(AM|PM|am|pm)$", "");
        
        return timeStr;
    }

    /**
     * Extract end time from timing string
     * Format: "MON 09:00 - 11:00"
     * Returns: "11:00" (after the "-" separator)
     */
    public static String extractEndTimeFromTiming(String timing) {
        if (timing == null || timing.isEmpty()) return null;
        
        // Look for pattern: - TIME
        int dashIndex = timing.indexOf('-');
        if (dashIndex == -1) return null;
        
        String afterDash = timing.substring(dashIndex + 1).trim();
        String[] parts = afterDash.split(" ");
        if (parts.length > 0) {
            String timeStr = parts[0];
            // Remove any AM/PM suffix
            timeStr = timeStr.replaceAll("\\s*(AM|PM|am|pm)$", "");
            return timeStr;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 DATA CLASS: CONFLICT INFO
    // ─────────────────────────────────────────────────────────────
    
    /**
     * Container for conflict information
     * Used to display detailed conflict messages in UI
     */
    public static class ConflictInfo {
        public String batchName;
        public String day;
        public String startTime;
        public String endTime;
        public int batchId;

        public ConflictInfo(String batchName, String day, String startTime, String endTime, int batchId) {
            this.batchName = batchName;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.batchId = batchId;
        }

        /**
         * Formatted message for UI display
         * Format: "Batch Name (Day HH:mm - HH:mm)"
         */
        public String getFormattedMessage() {
            return String.format("%s (%s %s – %s)", batchName, day, startTime, endTime);
        }

        public String toString() {
            return getFormattedMessage();
        }
    }
}
