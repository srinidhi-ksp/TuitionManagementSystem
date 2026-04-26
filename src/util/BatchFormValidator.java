package util;

import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Batch Form Validation Utility
 * Provides strict validation for batch creation and editing
 */
public class BatchFormValidator {

    /**
     * Validate batch name
     */
    public static String validateBatchName(String batchName) {
        if (batchName == null || batchName.trim().isEmpty()) {
            return "Batch Name is required";
        }
        if (batchName.trim().length() < 3) {
            return "Batch Name must be at least 3 characters";
        }
        // No only spaces is already covered by trim().isEmpty()
        return null; // Valid
    }

    /**
     * Validate combo box selection (not default value)
     */
    public static String validateComboSelection(JComboBox<?> combo, String fieldName) {
        Object selected = combo.getSelectedItem();
        if (selected == null) {
            return "Please select a " + fieldName;
        }
        
        String selectedStr = selected.toString();
        
        // Check for default placeholder values
        if (selectedStr.equalsIgnoreCase("Select " + fieldName) || 
            selectedStr.equalsIgnoreCase("Select Class") ||
            selectedStr.equalsIgnoreCase("Select Subject") ||
            selectedStr.equalsIgnoreCase("Select Teacher") ||
            selectedStr.equalsIgnoreCase("Select Mode") ||
            combo.getSelectedIndex() <= 0) {
            return "Please select a " + fieldName;
        }
        
        return null; // Valid
    }

    /**
     * Validate time selection
     */
    public static String validateTimeSelection(Date startTime, Date endTime) {
        if (startTime == null) {
            return "Start Time is required";
        }
        if (endTime == null) {
            return "End Time is required";
        }

        if (!endTime.after(startTime)) {
            return "End Time must be after Start Time";
        }

        return null; // Valid
    }

    /**
     * Validate meeting link (for online mode)
     */
    public static String validateMeetingLink(String meetingLink, String classMode) {
        if (classMode != null && classMode.equalsIgnoreCase("Online")) {
            if (meetingLink == null || meetingLink.trim().isEmpty()) {
                return "Meeting link is required for Online classes";
            }
            
            String trimmed = meetingLink.trim();
            if (!trimmed.toLowerCase().startsWith("http://") && !trimmed.toLowerCase().startsWith("https://")) {
                return "Meeting link must be a valid URL starting with http/https";
            }
            
            // Simple regex for URL validation
            if (!trimmed.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")) {
                return "Invalid meeting link URL format";
            }
        }
        return null; // Valid
    }

    /**
     * Validate entire batch form
     */
    public static String validateBatchForm(
            String batchName,
            JComboBox<?> classLevelCombo,
            JComboBox<?> subjectCombo,
            JComboBox<?> teacherCombo,
            JComboBox<?> modeCombo,
            Date startTime,
            Date endTime,
            String meetingLink) {

        // 1. Validate Batch Name
        String nameError = validateBatchName(batchName);
        if (nameError != null) return nameError;

        // 2. Validate Class/Standard
        String classError = validateComboSelection(classLevelCombo, "Class");
        if (classError != null) return classError;

        // 3. Validate Subject
        String subjectError = validateComboSelection(subjectCombo, "Subject");
        if (subjectError != null) return subjectError;

        // 4. Validate Teacher
        String teacherError = validateComboSelection(teacherCombo, "Teacher");
        if (teacherError != null) return teacherError;

        // 5. Validate Class Mode
        String modeError = validateComboSelection(modeCombo, "Mode");
        if (modeError != null) return modeError;

        // 6. Validate Times
        String timeError = validateTimeSelection(startTime, endTime);
        if (timeError != null) return timeError;

        // 7. Validate Meeting Link (if online)
        String selectedMode = modeCombo.getSelectedItem() != null ? modeCombo.getSelectedItem().toString() : "";
        String linkError = validateMeetingLink(meetingLink, selectedMode);
        if (linkError != null) return linkError;

        return null; // All validations passed
    }

    /**
     * Show validation error dialog
     */
    public static void showError(java.awt.Component parentComponent, String errorMessage) {
        JOptionPane.showMessageDialog(
            parentComponent,
            errorMessage,
            "Validation Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}

