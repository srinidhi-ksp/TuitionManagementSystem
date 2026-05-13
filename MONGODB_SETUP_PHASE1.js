/**
 * MongoDB Setup Script — Phase 1 (run once)
 * Timetable Overhaul + Batch Management + Smart Salary Engine
 *
 * Usage: mongosh tuitionManagementSystem MONGODB_SETUP_PHASE1.js
 */

// ============================================================
// 1. INSERT TIMESLOTS COLLECTION (8 fixed slots)
// ============================================================
print("=== 1. Seeding timeslots collection ===");

const timeslots = [
  { _id: "TS1", label: "06:00 \u2013 07:30", startHour: 6,  startMin: 0,  endHour: 7,  endMin: 30, durationMins: 90  },
  { _id: "TS2", label: "06:30 \u2013 08:00", startHour: 6,  startMin: 30, endHour: 8,  endMin: 0,  durationMins: 90  },
  { _id: "TS3", label: "08:30 \u2013 10:30", startHour: 8,  startMin: 30, endHour: 10, endMin: 30, durationMins: 120 },
  { _id: "TS4", label: "10:00 \u2013 12:00", startHour: 10, startMin: 0,  endHour: 12, endMin: 0,  durationMins: 120 },
  { _id: "TS5", label: "13:00 \u2013 15:00", startHour: 13, startMin: 0,  endHour: 15, endMin: 0,  durationMins: 120 },
  { _id: "TS6", label: "16:00 \u2013 17:30", startHour: 16, startMin: 0,  endHour: 17, endMin: 30, durationMins: 90  },
  { _id: "TS7", label: "18:00 \u2013 19:30", startHour: 18, startMin: 0,  endHour: 19, endMin: 30, durationMins: 90  },
  { _id: "TS8", label: "19:30 \u2013 21:00", startHour: 19, startMin: 30, endHour: 21, endMin: 0,  durationMins: 90  }
];

timeslots.forEach(ts => {
  const result = db.timeslots.updateOne(
    { _id: ts._id },
    { $setOnInsert: ts },
    { upsert: true }
  );
  print("  " + ts._id + ": " + (result.upsertedCount > 0 ? "inserted" : "already exists"));
});

// ============================================================
// 2. MIGRATE BATCHES: old timing fields → schedule[] array
// ============================================================
print("\n=== 2. Migrating batches to new schedule[] schema ===");

const batchCursor = db.batches.find({});
let migrated = 0, skipped = 0, failed = 0;

batchCursor.forEach(batch => {
  // Skip if already has new schema (schedule with timeslotId)
  if (batch.schedule && batch.schedule.length > 0 && batch.schedule[0].timeslotId) {
    skipped++;
    return;
  }

  // Try to build schedule from timing string (e.g. "TUE 09:00 - 11:00")
  let day = null;
  let startH = null, startM = null;

  if (batch.timing) {
    // Format: "MON 09:00 - 11:00" or "09:00 - 11:00"
    const parts = batch.timing.split(" ");
    const dayNames = ["MON","TUE","WED","THU","FRI","SAT","SUN"];
    if (dayNames.includes(parts[0])) {
      day = parts[0];
      const timePart = parts[1] || "";
      const [h, m] = timePart.split(":").map(Number);
      startH = h; startM = m;
    } else {
      const timePart = parts[0] || "";
      const [h, m] = timePart.split(":").map(Number);
      startH = h; startM = m;
    }
  } else if (batch.schedule && batch.schedule.length > 0 && batch.schedule[0].day) {
    // Already has legacy schedule with day/start/end
    const sch = batch.schedule[0];
    day = sch.day;
    if (sch.start) {
      const [h, m] = sch.start.split(":").map(Number);
      startH = h; startM = m;
    }
  }

  if (startH == null) { failed++; print("  SKIP (no parseable time): " + (batch.batch_name || batch._id)); return; }

  // Find closest timeslot by start time (within 30 min tolerance)
  const allSlots = db.timeslots.find().toArray();
  let bestSlot = null, bestDiff = Infinity;
  allSlots.forEach(ts => {
    const diff = Math.abs(ts.startHour * 60 + ts.startMin - (startH * 60 + startM));
    if (diff < bestDiff) { bestDiff = diff; bestSlot = ts; }
  });

  if (!bestSlot || bestDiff > 60) {
    failed++;
    print("  FAIL (no matching slot within 60 min): " + (batch.batch_name || batch._id)
        + " start=" + startH + ":" + startM);
    return;
  }

  // Build new schedule array
  const days = day ? [day] : ["MON"];
  const newSchedule = days.map(d => ({ day: d, timeslotId: bestSlot._id }));

  db.batches.updateOne(
    { _id: batch._id },
    {
      $set:   { schedule: newSchedule, timeslotId: bestSlot._id, status: batch.status || "ACTIVE" },
      $unset: { start_time: "", end_time: "" }
    }
  );
  migrated++;
  print("  Migrated: " + (batch.batch_name || batch._id) + " → " + bestSlot._id + " (" + bestSlot.label + ")" + (day ? " day=" + day : ""));
});

print("  Done: " + migrated + " migrated, " + skipped + " skipped, " + failed + " failed.");

// ============================================================
// 3. CREATE COMPOUND INDEX ON BATCHES
// ============================================================
print("\n=== 3. Creating compound index idx_teacher_day_slot ===");
try {
  db.batches.createIndex(
    { "teacherId": 1, "schedule.day": 1, "schedule.timeslotId": 1 },
    { name: "idx_teacher_day_slot", background: true }
  );
  // Also index on teacher_id (the field name actually used in DB)
  db.batches.createIndex(
    { "teacher_id": 1, "schedule.day": 1, "schedule.timeslotId": 1 },
    { name: "idx_teacher_id_day_slot", background: true }
  );
  print("  Indexes created.");
} catch(e) { print("  Index already exists or error: " + e.message); }

// ============================================================
// 4. INSERT DEFAULT SALARY RULES
// ============================================================
print("\n=== 4. Seeding salary_rules collection ===");
const defaultRules = {
  _id: "DEFAULT",
  freeDaysAllowed: 1,
  baseDeductionPerAbsentDay: 300,
  deductionIncrementPerDay: 100,
  bonusPerExtraSlot: 200,
  lastUpdatedAt: new Date()
};
const rulesResult = db.salary_rules.updateOne(
  { _id: "DEFAULT" },
  { $setOnInsert: defaultRules },
  { upsert: true }
);
print("  salary_rules DEFAULT: " + (rulesResult.upsertedCount > 0 ? "inserted" : "already exists"));

// ============================================================
// 5. CREATE teacher_extra_slots COLLECTION (touch to create)
// ============================================================
print("\n=== 5. Ensuring teacher_extra_slots collection exists ===");
try {
  db.createCollection("teacher_extra_slots");
  print("  Created teacher_extra_slots collection.");
} catch(e) { print("  Collection already exists."); }

// Index for fast lookups
db.teacher_extra_slots.createIndex({ teacher_id: 1, month: 1, year: 1 }, { name: "idx_teacher_month_year" });
print("  Index idx_teacher_month_year created.");

// ============================================================
// 6. PATCH EXISTING salary_records: add extra_slots=0, extra_bonus=0
// ============================================================
print("\n=== 6. Patching salary_records: adding missing extra_slots / extra_bonus fields ===");
const patchResult = db.salary_records.updateMany(
  { extra_slots: { $exists: false } },
  { $set: { extra_slots: 0, extra_bonus: 0.0 } }
);
print("  Patched " + patchResult.modifiedCount + " salary records.");

print("\n=== ✅ Phase 1 setup complete ===");
