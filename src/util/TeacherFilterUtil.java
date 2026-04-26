package util;

import java.util.Date;
import java.util.List;

import dao.TeacherDAO;
import model.Teacher;

/**
 * Teacher Filtering Utility
 * Filters teachers based on their assigned classes
 */
public class TeacherFilterUtil {

    private static TeacherDAO teacherDAO = new TeacherDAO();

    /**
     * Get teachers for a specific class
     */
    public static List<Teacher> getTeachersForClass(String className) {
        if (className == null || className.isEmpty()) {
            return teacherDAO.getAllTeachers();
        }
        return teacherDAO.getTeachersByClass(className);
    }

    /**
     * Format teacher display as "ID - Name (Subject)"
     */
    public static String formatTeacherForDisplay(Teacher teacher) {
        if (teacher == null) return "Unknown";
        
        String name = teacher.getName() != null ? teacher.getName() : "No Name";
        String spec = teacher.getSpecialization() != null ? teacher.getSpecialization() : "";
        
        if (!spec.isEmpty()) {
            return String.format("%s – %s (%s)", teacher.getUserId(), name, spec);
        } else {
            return String.format("%s – %s", teacher.getUserId(), name);
        }
    }
}
