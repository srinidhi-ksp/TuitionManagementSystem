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
        if (batchName.trim().matches("^\\s*$")) {
            return "Batch Name cannot be only spaces";
        }
        return null; // Valid
    }

    /**
     * Validate combo box selection (not default value)
     */
    public static String validateComboSelection(JComboBox<?> combo, String fieldName) {
        int selectedIndex = combo.getSelectedIndex();
        
        // First item (index 0) is typically "Select..."
        if (selectedIndex <= 0) {
            return "Please select a " + fieldName;
        }
        
        String selected = combo.getSelectedItem().toString();
        if (selected.contains("Select") || selected.isEmpty()) {
            return "Please select a valid " + fieldName;
        }
        
        return null; // Valid
    }

    /**
     * Validate time selection (handles Date objects from TimeChooser)
     */
    public static String validateTimeSelection(Date startTime, Date endTime) {
        if (startTime == null) {
            return "Start Time is required";
        }
        if (endTime == null) {
            return "End Time is required";
        }

        if (endTime.before(startTime) || endTime.equals(startTime)) {
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
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return "Meeting link must start with http:// or https://";
            }
            
            try {
                new java.net.URL(trimmed);
            } catch (java.net.MalformedURLException e) {
                return "Invalid meeting link URL";
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
        String classError = validateComboSelection(classLevelCombo, "Class/Standard");
        if (classError != null) return classError;

        // 3. Validate Subject
        String subjectError = validateComboSelection(subjectCombo, "Subject");
        if (subjectError != null) return subjectError;

        // 4. Validate Teacher
        String teacherError = validateComboSelection(teacherCombo, "Teacher");
        if (teacherError != null) return teacherError;

        // 5. Validate Class Mode
        String modeError = validateComboSelection(modeCombo, "Class Mode");
        if (modeError != null) return modeError;

        // 6. Validate Times
        String timeError = validateTimeSelection(startTime, endTime);
        if (timeError != null) return timeError;

        // 7. Validate Meeting Link (if online)
        String linkError = validateMeetingLink(meetingLink, modeCombo.getSelectedItem().toString());
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

    /**
     * Highlight invalid field (optional UI enhancement)
     */
    public static void highlightField(JComponent component, boolean isInvalid) {
        if (component instanceof JTextField) {
            JTextField field = (JTextField) component;
            if (isInvalid) {
                field.setBorder(BorderFactory.createLineBorder(new java.awt.Color(244, 67, 54), 2, true));
                field.setBackground(new java.awt.Color(255, 245, 245));
            } else {
                field.setBorder(null);
                field.setBackground(java.awt.Color.WHITE);
            }
        } else if (component instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) component;
            if (isInvalid) {
                combo.setBorder(BorderFactory.createLineBorder(new java.awt.Color(244, 67, 54), 2, true));
                combo.setBackground(new java.awt.Color(255, 245, 245));
            } else {
                combo.setBorder(null);
                combo.setBackground(java.awt.Color.WHITE);
            }
        }
    }
}
