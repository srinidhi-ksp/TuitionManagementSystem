# 📋 Testing & Verification Guide

## Test Case 1: Single Enrollment

### Scenario
Student `U21` is enrolled in 1 batch (ID: 205)

### Expected Console Output
```
[StudentDAO] 🔍 Attempting to map User ID: 'U21'
[StudentDAO]   🔄 Trying email-based lookup...
[StudentDAO]   Searching by email: student1@example.com
[StudentDAO]   ✅ Found via email field
[StudentDAO] ✅ Mapped U21 → Student S001

[MyBatchesPanel] 🔄 Starting batch load...
[MyBatchesPanel] User ID: U21
[MyBatchesPanel] ✅ Mapped Student ID: S001
[MyBatchesPanel] 📊 Enrollments Found: 1
[MyBatchesPanel] Processing enrollment #1 (Batch ID: 205, Status: ACTIVE)
[MyBatchesPanel]   ✅ Added: English Foundation Evening (English)
[MyBatchesPanel] ✅ Total rows to display: 1
```

### UI Verification
- ✅ Dashboard shows "Active Batches: 1"
- ✅ My Batches shows 1 row
- ✅ Fees & Payments shows 1 fee entry

---

## Test Case 2: Multiple Enrollments (KEY TEST)

### Scenario
Student `U21` is enrolled in 3 batches:
- Batch 205: English (PAID)
- Batch 206: Physics (UNPAID)
- Batch 207: Maths (PAID)

### Expected Console Output
```
[StudentDAO] 🔍 Attempting to map User ID: 'U21'
[StudentDAO]   🔄 Trying email-based lookup...
[StudentDAO]   Searching by email: student1@example.com
[StudentDAO]   ✅ Found via email field
[StudentDAO] ✅ Mapped U21 → Student S001

[MyBatchesPanel] 📊 Enrollments Found: 3
[MyBatchesPanel] Processing enrollment #1 (Batch ID: 205, Status: ACTIVE)
[MyBatchesPanel]   ✅ Added: English Foundation Evening (English)
[MyBatchesPanel] Processing enrollment #2 (Batch ID: 206, Status: ACTIVE)
[MyBatchesPanel]   ✅ Added: Physics Advanced Morning (Physics)
[MyBatchesPanel] Processing enrollment #3 (Batch ID: 207, Status: ACTIVE)
[MyBatchesPanel]   ✅ Added: Maths Basics Afternoon (Maths)
[MyBatchesPanel] ✅ Total rows to display: 3

[FeeService] 🔄 Resolving user_id -> student_id for: U21
[FeeService] ✅ Mapped U21 → S001

[FeeService] Fetching fee details for student: S001
[EnrollmentDAO] Querying enrollments for ID: 'S001'
[EnrollmentDAO]   ✔ Match Found: Enrollment #301 (Batch 205)
[EnrollmentDAO]   ✔ Match Found: Enrollment #302 (Batch 206)
[EnrollmentDAO]   ✔ Match Found: Enrollment #303 (Batch 207)
[EnrollmentDAO] ✅ Total Enrollments Found: 3

[FeeService]   -> Added: English (English Foundation Evening) | Status: PAID
[FeeService]   -> Added: Physics (Physics Advanced Morning) | Status: UNPAID
[FeeService]   -> Added: Maths (Maths Basics Afternoon) | Status: PAID
[FeeService] ✅ Final fee details size: 3

[FeeService] Summary - Total: Rs. 3300.0 | Paid: Rs. 2200.0 | Pending: Rs. 1100.0 | Status: PARTIAL
```

### UI Verification - Dashboard
- ✅ Shows "Active Batches: 3"
- ✅ Shows "Subjects: 3"

### UI Verification - My Batches
- ✅ Shows 3 rows in table
- ✅ Each row shows: Batch Name, Subject, Teacher, Schedule, Mode, Status
- ✅ All 3 batches visible without scrolling issues

### UI Verification - Fees & Payments
- ✅ Summary shows:
  - Total Fees: ₹3,300.00
  - Paid Amount: ₹2,200.00
  - Pending Balance: ₹1,100.00
  - Overall Status: PARTIAL
- ✅ Table shows 3 rows:
  - English: ₹1,100 | PAID
  - Physics: ₹1,100 | UNPAID
  - Maths: ₹1,100 | PAID

---

## Test Case 3: No Enrollments

### Scenario
Student `U22` has NO enrollments

### Expected Console Output
```
[StudentDAO] 🔍 Attempting to map User ID: 'U22'
[StudentDAO]   🔄 Trying email-based lookup...
[StudentDAO]   Searching by email: student2@example.com
[StudentDAO]   ✅ Found via email field
[StudentDAO] ✅ Mapped U22 → Student S002

[MyBatchesPanel] 📊 Enrollments Found: 0
[MyBatchesPanel] ⚠️  No active enrollments for student: S002
```

### UI Verification
- ✅ Dashboard shows "Active Batches: 0"
- ✅ My Batches shows message: "No Data Available"
- ✅ Fees & Payments shows:
  - All amounts: ₹0.00
  - Status: "NO_ENROLLMENT"

---

## Test Case 4: ID Mapping Failure Scenario

### Scenario
Email mismatch between users and students collections

### Expected Console Output
```
[StudentDAO] 🔍 Attempting to map User ID: 'U99'
[StudentDAO]   ⚠️  No match on user_id field
[StudentDAO]   🔄 Trying email-based lookup...
[StudentDAO]   Searching by email: user99@example.com
[StudentDAO]   ⚠️  No match on email field
[StudentDAO]   🔄 ID starts with 'S', trying as student_id...
[StudentDAO] ❌ FAILED to map User ID: U99 (all strategies exhausted)

[StudentService] ❌ Failed to map user_id U99 to student

[MyBatchesPanel] ❌ Mapped Student: NULL - Cannot proceed
```

### UI Verification
- ✅ Error message shows: "Unable to load data"
- ✅ No crash, graceful failure

### Resolution
1. Check MongoDB:
   ```
   db.users.findOne({_id: "U99"})        → Get email
   db.students.findOne({email: email})   → Verify student exists
   ```
2. Update student record with matching email if needed

---

## Performance Benchmarks

### Expected Times (with MongoDB running locally)
- ID mapping: < 50ms (email lookup)
- Fetch 1 enrollment: < 100ms
- Fetch 3 enrollments: < 300ms
- Full dashboard load: < 1 second
- Full fees load: < 1 second

### Slow Query Diagnosis
If taking > 3 seconds:
1. Check MongoDB is running
2. Check network latency
3. Check database indexes on email field
4. Check for slow queries in MongoDB logs

---

## Console Output Legend

### Color-Coded Messages (in actual IDE)

**Success Messages:**
```
✅ = Green    Operation succeeded
```

**Error Messages:**
```
❌ = Red      Operation failed, needs attention
```

**Warning Messages:**
```
⚠️  = Yellow   Partial success or non-critical issue
```

**Info Messages:**
```
🔄 = Cyan     Processing/intermediate step
📊 = Blue     Data/statistics
🔍 = Purple   Searching/querying
```

---

## Log File Location

Console logs are output to:
```
Console/Terminal where application started
(Or IDE console if running from IDE)
```

To save logs to file (optional):
```bash
java -jar app.jar > application.log 2>&1
```

---

## How to Read Logs for Debugging

### Step 1: Find the Entry Point
```
Look for: [ComponentName] 🔄 Starting process...
         [Component] Starting load...
```

### Step 2: Follow the Chain
```
[StudentDAO] 🔍 Attempting to map...
    ↓
[StudentDAO] ✅ Mapped...
    ↓
[EnrollmentDAO] Querying enrollments...
    ↓
[Service] Final results...
```

### Step 3: Check for Errors
```
If you see ❌, trace back to find:
- Null values
- Missing database records
- Field mismatches
```

---

## Automated Test Scenarios

### Batch 1: Basic Functionality
- [ ] Login with student account
- [ ] Dashboard loads
- [ ] Batch count is correct
- [ ] No console errors

### Batch 2: Multi-Enrollment
- [ ] Enroll in 2+ batches
- [ ] All batches visible in My Batches
- [ ] All fees visible in Fees & Payments
- [ ] Dashboard count accurate

### Batch 3: Edge Cases
- [ ] Student with 0 enrollments
- [ ] Student with 10 enrollments
- [ ] Batch with missing teacher
- [ ] Subject with missing data

### Batch 4: Error Handling
- [ ] Invalid user_id
- [ ] Email mismatch
- [ ] Missing student record
- [ ] Missing batch record

---

## Regression Test

After deployment, verify:

1. **Existing Single-Batch Students**
   - [ ] Still see their 1 batch
   - [ ] Fees still calculated correctly
   - [ ] Dashboard stats unchanged

2. **Multi-Batch Students (New)**
   - [ ] See all enrolled batches
   - [ ] Fees show all subjects
   - [ ] Dashboard reflects total count

3. **Admin Functions**
   - [ ] Can still create enrollments
   - [ ] Can still view all students
   - [ ] Can still manage batches

4. **Teacher Views**
   - [ ] See correct students in batch
   - [ ] Mark attendance correctly
   - [ ] Record marks correctly

---

## Troubleshooting Checklist

| Issue | Check | Fix |
|-------|-------|-----|
| Single batch shown | Look for "Total rows: 1" in logs | Verify enrollments in DB |
| No fees shown | Look for "Final fee details size: 0" | Check fee records exist |
| "NO_ENROLLMENT" message | Look for "Enrollments Found: 0" | Add enrollments |
| Dashboard shows 0 batches | Look for "Found 0 active batches" | Check enrollment status |
| Mapping fails | Look for "❌ FAILED to map" | Check email mismatch |
| Slow loading | Check console for timeouts | Optimize queries/indexes |

---

## Success Indicators

When fix is working correctly, you should see:

```
✅ All enrollments mapped successfully
✅ Student ID resolution shows user_id → student_id
✅ Multiple batches displayed in UI
✅ Fees calculated for all subjects
✅ Dashboard stats accurate
✅ No null pointer exceptions
✅ Console shows info/debug logs, no errors
```

---

## Documentation References

For detailed information, see:
- `MULTI_BATCH_ENROLLMENT_FIX.md` - Comprehensive technical details
- `CODE_CHANGES_DETAILED.md` - Exact code changes made
- `QUICK_REFERENCE.md` - High-level overview

---

**Test Status:** READY FOR QA  
**Date:** April 24, 2026  
**Version:** 1.0
