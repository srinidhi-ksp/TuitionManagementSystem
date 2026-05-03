# 🔷 ENROLLMENT VALIDATION SYSTEM GUIDE

## Overview
This document explains the **strict validation rules** implemented for student enrollment in the Tuition Management System. The system prevents:
- ❌ Duplicate enrollments (same student in same batch)
- ❌ Schedule conflicts (student already has class at that time)

---

## 📋 VALIDATION RULES

### RULE 1: PREVENT DUPLICATE ENROLLMENT

**When:** Admin clicks "Save Enrollment" OR selects a batch in the dropdown

**Check:**
```mongodb
db.enrollments.findOne({
  student_id: selectedStudentId,
  batch_id: selectedBatchId,
  status: "ACTIVE"
})
```

**If Duplicate Found:**
- ❌ Shows error message: "Duplicate Enrollment!"
- ❌ Displays student name and batch name
- ✋ Blocks enrollment
- 🔴 Save button is **disabled**

**Java Method:**
```java
// In EnrollmentDAO
public boolean isDuplicateEnrollment(String studentId, int batchId)

// In ScheduleConflictValidator  
public static String checkDuplicateEnrollment(String studentId, int batchId)
```

---

### RULE 2: PREVENT SCHEDULE CONFLICT

**When:** Admin selects a batch OR clicks "Save Enrollment"

**Steps:**

1. Get selected batch schedule (day, start_time, end_time)
2. Get student's ACTIVE enrollments from DB
3. For each enrollment, fetch corresponding batch schedule
4. Compare schedules using overlap logic

**Time Overlap Condition:**
```java
// Conflict if: (start1 < end2) AND (end1 > start2)
return s1.isBefore(e2) && e1.isAfter(s2);
```

**If Conflict Found:**
- ⚠️ Shows warning: "Schedule Conflict Detected!"
- 📍 Displays conflicting batch name, day, and time
- ✋ Blocks enrollment
- 🔴 Save button is **disabled**

**Java Method:**
```java
// In ScheduleConflictValidator
public static ConflictInfo checkStudentConflict(String studentId, int selectedBatchId)

// In ScheduleConflictValidator
public static boolean isTimeConflict(String start1, String end1, String start2, String end2)
```

---

## 🎯 RULE 3: REAL-TIME VALIDATION (IMPORTANT)

**Triggers:** When batch is selected in dropdown

**Validation Checks:**
1. ✔ Duplicate enrollment check
2. ✔ Schedule conflict check

**Instant Feedback Display (Below Batch Field):**

| Status | Display | Color |
|--------|---------|-------|
| ❌ Duplicate | "❌ Duplicate Enrollment!" + Details | 🔴 Red |
| ⚠️ Conflict | "⚠️ Schedule Conflict Detected!" + Details | 🟡 Orange |
| ✅ Valid | "✅ No conflicts" | 🟢 Green |
| ℹ️ Incomplete | "ℹ️ Select student and batch" | 🔵 Gray |

**Button State:**
- 🔴 **Disabled** → Duplicate or conflict found
- 🟢 **Enabled** → All validations pass + both student and batch selected

---

## 🔷 RULE 4: DISABLE SAVE BUTTON

**Save Button State:**

```
✋ DISABLED IF:
  → No student selected
  → No batch selected
  → Duplicate enrollment found
  → Schedule conflict found

✅ ENABLED ONLY IF:
  → Student selected
  → Batch selected
  → No duplicate
  → No schedule conflict
```

---

## 🛠️ JAVA CODE IMPLEMENTATION

### File: `src/dao/EnrollmentDAO.java`

**Added Method:**
```java
// ✅ DUPLICATE ENROLLMENT CHECK
public boolean isDuplicateEnrollment(String studentId, int batchId) {
    if (enrollmentCollection == null || studentId == null) return false;
    try {
        String sid = studentId.trim();
        // Check if student already enrolled in this batch with ACTIVE status
        Document existing = enrollmentCollection.find(
            Filters.and(
                Filters.or(
                    Filters.eq("student_user_id", sid),
                    Filters.eq("student_id", sid),
                    Filters.eq("user_id", sid)
                ),
                Filters.eq("batch_id", batchId),
                Filters.eq("status", "ACTIVE")
            )
        ).first();
        
        return existing != null;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
```

---

### File: `src/util/ScheduleConflictValidator.java`

**Added Method - Duplicate Check:**
```java
public static String checkDuplicateEnrollment(String studentId, int batchId) {
    if (studentId == null) {
        return null;
    }

    EnrollmentDAO enrollmentDao = new EnrollmentDAO();
    
    // Check if duplicate exists
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
        return studentName;  // Returns student name if duplicate
    }
    
    return null; // No duplicate
}
```

**Existing Method - Time Conflict:**
```java
public static ConflictInfo checkStudentConflict(String studentId, int selectedBatchId)

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
        return false;
    }
}
```

---

### File: `src/ui/admin/EnrollmentManagementFrame.java`

**Enhanced `openEnrollModal()` Method Features:**

#### Real-Time Validation
```java
// Real-time validation on batch selection
batchCombo.addActionListener(e -> {
    String selS = studentCombo.getSelectedItem().toString();
    Batch selB = (Batch) batchCombo.getSelectedItem();
    
    if (!selS.startsWith("Select") && selB != null) {
        String studentId = selS.split(" – ")[0].trim();
        
        // CHECK 1: DUPLICATE ENROLLMENT
        String duplicateStudentName = 
            util.ScheduleConflictValidator.checkDuplicateEnrollment(studentId, selB.getBatchId());
        if (duplicateStudentName != null) {
            validationLabel.setText("❌ Duplicate Enrollment!");
            statusDetailsLabel.setText("This student is already enrolled in this batch.");
            isValid[0] = false;
        } else {
            // CHECK 2: SCHEDULE CONFLICT
            util.ScheduleConflictValidator.ConflictInfo conflict = 
                util.ScheduleConflictValidator.checkStudentConflict(studentId, selB.getBatchId());
            
            if (conflict != null) {
                validationLabel.setText("⚠️ Schedule Conflict Detected!");
                statusDetailsLabel.setText("Conflicts with: " + conflict.getFormattedMessage());
                isValid[0] = false;
            } else {
                validationLabel.setText("✅ No conflicts");
                isValid[0] = true;
            }
        }
    }
    
    // Enable/disable save button
    saveBtn.setEnabled(isValid[0] && !selS.startsWith("Select") && selB != null);
});
```

#### Final Validation Before Save
```java
// FINAL VALIDATION BEFORE SAVE
String duplicateCheck = 
    util.ScheduleConflictValidator.checkDuplicateEnrollment(studentId, selB.getBatchId());
if (duplicateCheck != null && !isEditMode) {
    JOptionPane.showMessageDialog(dialog,
        "❌ DUPLICATE ENROLLMENT!\n\n" +
        "This student is already enrolled in this batch.\n\n" +
        "Student: " + duplicateCheck + "\n" +
        "Batch: " + selB.getBatchName() + "\n\n" +
        "You cannot enroll the same student twice.",
        "Duplicate Enrollment Error", JOptionPane.ERROR_MESSAGE);
    return; // Stop enrollment
}

// Check schedule conflict
util.ScheduleConflictValidator.ConflictInfo conflict = 
    util.ScheduleConflictValidator.checkStudentConflict(studentId, selB.getBatchId());

if (conflict != null && !isEditMode) {
    JOptionPane.showMessageDialog(dialog,
        "⚠️ SCHEDULE CONFLICT DETECTED!\n\n" +
        "This student already has another class at this time.\n\n" +
        "Conflicting Batch:\n" +
        "  Name: " + conflict.batchName + "\n" +
        "  Day: " + conflict.day + "\n" +
        "  Time: " + conflict.startTime + " – " + conflict.endTime + "\n\n" +
        "Please choose a different batch.",
        "Schedule Conflict", JOptionPane.WARNING_MESSAGE);
    return; // Stop enrollment
}
```

---

## 🧪 TESTING SCENARIOS

### Test Case 1: Duplicate Enrollment
1. Open "Enroll Student" form
2. Select a student who is already enrolled in a batch (e.g., John → Physics Batch)
3. Select the **same batch** (Physics Batch) again
4. **Expected:** 
   - ❌ "Duplicate Enrollment!" warning appears
   - 🔴 Save button is disabled
   - ✋ Cannot proceed with enrollment

### Test Case 2: Schedule Conflict
1. Open "Enroll Student" form
2. Select a student enrolled in batch on **Monday 5:00 PM - 6:00 PM**
3. Try to enroll in another batch at **Monday 5:30 PM - 6:30 PM**
4. **Expected:**
   - ⚠️ "Schedule Conflict Detected!" warning appears
   - Details show conflicting batch and time
   - 🔴 Save button is disabled
   - ✋ Cannot proceed with enrollment

### Test Case 3: Valid Enrollment
1. Open "Enroll Student" form
2. Select a student with no conflicts
3. Select a batch at a different time than their other enrollments
4. **Expected:**
   - ✅ "No conflicts" message appears in green
   - 🟢 Save button is enabled
   - ✅ Can save enrollment successfully

### Test Case 4: Edit Mode (Bypass Validation)
1. Click "Edit" on an existing enrollment
2. Keep the same student and batch
3. **Expected:**
   - Validation checks are **skipped** (isEditMode = true)
   - Can update enrollment without warnings
   - Prevents editing the same enrollment from being flagged as duplicate

---

## 🎨 UI COLORS & INDICATORS

| Element | Color | Meaning |
|---------|-------|---------|
| ✅ Valid Status | 🟢 Green (22, 163, 74) | No issues detected |
| ⚠️ Warning | 🟡 Orange (234, 179, 8) | Schedule conflict warning |
| ❌ Error | 🔴 Red (220, 38, 38) | Duplicate enrollment error |
| ℹ️ Info | 🔵 Gray (107, 122, 153) | Incomplete selection |

---

## 📊 FLOW DIAGRAM

```
┌─────────────────────────┐
│ Admin Opens Enroll Form │
└────────────┬────────────┘
             │
             ▼
    ┌────────────────────┐
    │ Select Student     │
    │ Select Batch       │
    └────────┬───────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ Real-Time Validation       │
    │ (Batch Selection)          │
    └────────┬───────────────────┘
             │
       ┌─────┴─────┐
       │            │
       ▼            ▼
   ┌────────┐  ┌──────────┐
   │ Check  │  │ Check    │
   │Duplicate│ │ Conflict │
   └────┬───┘  └────┬─────┘
        │           │
    ┌───┴───────────┴──┐
    │                  │
    ▼                  ▼
┌─────────┐      ┌─────────┐
│ Found ✖  │      │ Found ✖  │
└────┬────┘      └────┬────┘
     │                │
     ▼                ▼
  Red ❌           Orange ⚠️
  DISABLED        DISABLED
  
  If BOTH PASS:
     ▼
  Green ✅
  ENABLED
     │
     ▼
┌─────────────────┐
│ Click Save      │
└────────┬────────┘
         │
         ▼
    ┌────────────────────────┐
    │ Final Validation Check │
    │ (Before DB Insert)     │
    └────────┬───────────────┘
             │
       ┌─────┴──────┐
       │             │
    PASS         FAIL
       │             │
       ▼             ▼
   ✅ INSERT    ❌ SHOW ERROR
   Success      Try Again
```

---

## 🚀 EXPECTED BEHAVIOR SUMMARY

| Action | Before | After |
|--------|--------|-------|
| Select Student + Batch | Form opens, no feedback | Immediate validation feedback |
| No Conflicts | Save button enabled | Save button enabled ✅ |
| Duplicate Found | Can save (bug) | Cannot save, button disabled ✋ |
| Schedule Conflict | Can save (bug) | Cannot save, button disabled ✋ |
| Click Save | Inserts immediately | Final check → Insert or Error |
| Edit Mode | Same validation | Validation bypassed (can edit same enrollment) |

---

## ✅ IMPLEMENTATION CHECKLIST

- ✅ Added `isDuplicateEnrollment()` method to EnrollmentDAO
- ✅ Added `checkDuplicateEnrollment()` method to ScheduleConflictValidator
- ✅ Enhanced `openEnrollModal()` with real-time validation
- ✅ Added two validation label fields for feedback
- ✅ Implemented save button enable/disable logic
- ✅ Added detailed error popups with batch information
- ✅ Color-coded validation feedback (red, orange, green)
- ✅ Edit mode validation bypass
- ✅ Final validation check before database insert

---

## 🔧 USAGE EXAMPLES

### Example 1: Preventing Duplicate
```
Admin: "I want to enroll John in Physics Batch"
System: John is already enrolled in Physics Batch
Result: ❌ Duplicate error shown, enrollment blocked
```

### Example 2: Preventing Schedule Conflict
```
Admin: "Enroll Jane in Math Batch (Mon 5-6 PM)"
System: Jane is already in Physics Batch (Mon 5:30-6:30 PM)
Result: ⚠️ Conflict warning shown, enrollment blocked
```

### Example 3: Valid Enrollment
```
Admin: "Enroll Bob in Chemistry Batch (Tue 4-5 PM)"
System: No conflicts found
Result: ✅ Success, Bob enrolled in Chemistry
```

---

## 📝 NOTES

- **Edit Mode:** When editing existing enrollments, duplicate checks are skipped
- **Active Status Only:** Checks only consider "ACTIVE" enrollments
- **Time Format:** Uses 24-hour format (HH:mm)
- **Day Comparison:** Case-insensitive (MON, mon, Mon all work)
- **Student ID Lookup:** Checks multiple ID fields for flexibility

---

## 🎯 BENEFITS

✅ **Prevents Data Corruption** - No duplicate enrollments in database  
✅ **Better UX** - Real-time feedback prevents frustration  
✅ **Professional UI** - Color-coded indicators for quick understanding  
✅ **Disabled Actions** - Button disabled prevents accidental clicks  
✅ **Clear Messages** - Detailed error messages with specific batch info  
✅ **Flexible Editing** - Edit mode allows corrections without duplication errors  

---

Generated: May 3, 2026
