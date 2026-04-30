// MongoDB Setup & Migration Script
// For Tuition Management System - Teacher Collection Enhancement

// ============================================================
// STEP 1: Create Indexes for Performance Optimization
// ============================================================

// Run these in MongoDB Shell or Atlas Query Editor

// Student Collection Indexes
db.students.createIndex({ "standard": 1 })
db.students.createIndex({ "board": 1 })
db.students.createIndex({ "city": 1 })
db.students.createIndex({ "full_name": "text" })

// Enrollments Collection Indexes (CRITICAL for active student filtering)
db.enrollments.createIndex({ "status": 1 })
db.enrollments.createIndex({ "student_id": 1, "status": 1 })

// Teachers Collection Indexes (CRITICAL for teacher filtering)
db.teachers.createIndex({ "specialization": 1 })
db.teachers.createIndex({ "city": 1 })
db.teachers.createIndex({ "experience_years": 1 })
db.teachers.createIndex({ "salary": 1 })
db.teachers.createIndex({ "full_name": "text" })

// ============================================================
// STEP 2: Initialize Existing Teacher Documents
// ============================================================

// Add missing fields to all existing teachers with default values
db.teachers.updateMany(
  {},
  {
    $set: {
      experience_years: 0,
      salary: 0,
      highest_degree: "Not Specified",
      status: "ACTIVE"
    }
  }
)

// ============================================================
// STEP 3: Bulk Update Sample Teacher Data
// ============================================================

// Option A: Individual Updates
db.teachers.updateOne(
  { _id: "T001" },
  {
    $set: {
      experience_years: 8,
      salary: 50000,
      highest_degree: "M.Sc Mathematics",
      status: "ACTIVE"
    }
  }
)

// Option B: Bulk Write (Multiple teachers at once)
db.teachers.bulkWrite([
  {
    updateOne: {
      filter: { _id: "T001" },
      update: {
        $set: {
          experience_years: 8,
          salary: 50000,
          highest_degree: "M.Sc Mathematics",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T002" },
      update: {
        $set: {
          experience_years: 5,
          salary: 42000,
          highest_degree: "M.Sc Physics",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T003" },
      update: {
        $set: {
          experience_years: 6,
          salary: 45000,
          highest_degree: "M.Sc Chemistry",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T004" },
      update: {
        $set: {
          experience_years: 7,
          salary: 47000,
          highest_degree: "M.Sc Biology",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T005" },
      update: {
        $set: {
          experience_years: 4,
          salary: 40000,
          highest_degree: "M.Tech Computer Science",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T006" },
      update: {
        $set: {
          experience_years: 3,
          salary: 35000,
          highest_degree: "M.A English",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T007" },
      update: {
        $set: {
          experience_years: 9,
          salary: 55000,
          highest_degree: "M.Sc Mathematics",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T008" },
      update: {
        $set: {
          experience_years: 5,
          salary: 43000,
          highest_degree: "M.Sc Physics",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T009" },
      update: {
        $set: {
          experience_years: 6,
          salary: 46000,
          highest_degree: "M.Sc Chemistry",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T010" },
      update: {
        $set: {
          experience_years: 7,
          salary: 48000,
          highest_degree: "M.Sc Biology",
          status: "ACTIVE"
        }
      }
    }
  },
  {
    updateOne: {
      filter: { _id: "T011" },
      update: {
        $set: {
          experience_years: 10,
          salary: 52000,
          highest_degree: "M.A History",
          status: "ACTIVE"
        }
      }
    }
  }
])

// ============================================================
// STEP 4: Verify Updates
// ============================================================

// Check if all teachers have new fields
db.teachers.find({ experience_years: { $exists: true } }).count()

// View updated teacher document
db.teachers.findOne({ _id: "T001" })

// Find teachers with specific experience range
db.teachers.find({ 
  experience_years: { $gte: 5, $lte: 10 } 
})

// Find teachers in salary range
db.teachers.find({ 
  salary: { $gte: 40000, $lte: 50000 } 
})

// ============================================================
// STEP 5: Sample Queries for Testing Filter Functionality
// ============================================================

// Get all specializations
db.teachers.distinct("specialization")

// Get all cities
db.teachers.distinct("city")

// Filter: Specialization + Experience Range
db.teachers.find({
  specialization: "Mathematics",
  experience_years: { $gte: 5, $lte: 10 }
})

// Filter: City + Salary Range
db.teachers.find({
  city: "Delhi",
  salary: { $gte: 40000, $lte: 60000 }
})

// Complex filter with multiple criteria
db.teachers.find({
  specialization: "Mathematics",
  city: "Mumbai",
  experience_years: { $gte: 3 },
  salary: { $gte: 40000 }
}).sort({ salary: -1 })

// ============================================================
// STEP 6: Verify Student Filtering Data
// ============================================================

// Get active student IDs (students with ACTIVE enrollments)
db.enrollments.distinct("student_id", { status: "ACTIVE" })

// Get student counts by status
db.enrollments.aggregate([
  { $group: { 
      _id: "$status", 
      count: { $sum: 1 },
      studentIds: { $addToSet: "$student_id" }
  }}
])

// Find students by various criteria
db.students.find({
  standard: "10",
  board: "CBSE",
  city: "Mumbai"
})

// Text search for student by name
db.students.find({
  $text: { $search: "Raj" }
})

// ============================================================
// STEP 7: Cleanup (if needed)
// ============================================================

// Remove an index if needed
db.teachers.dropIndex("specialization_1")

// Drop all indexes except _id (WARNING: impacts performance!)
// db.teachers.dropIndexes()

// ============================================================
// EXPECTED DOCUMENT STRUCTURE AFTER UPDATES
// ============================================================

// Sample teacher document after enhancement:
{
  "_id": "T001",
  "full_name": "Rajesh Kumar",
  "email": "rajesh@school.com",
  "phone": "9876543210",
  "password": "hashed_password",
  "role": "TEACHER",
  "specialization": "Mathematics",
  "city": "Mumbai",
  "experience_years": 8,              // ← NEW FIELD
  "salary": 50000,                    // ← NEW FIELD
  "highest_degree": "M.Sc Mathematics", // ← NEW FIELD
  "status": "ACTIVE",                 // ← NEW FIELD
  "join_date": ISODate("2018-06-15"),
  "admin_id": "A001"
}

// ============================================================
// NOTES
// ============================================================

/*
1. Run STEP 1 first to create indexes (one-time setup)
2. Run STEP 2 to initialize all existing teachers
3. Run STEP 3 to add specific data to teachers
4. Run STEP 4 to verify updates were successful
5. Run STEP 5 to test filtering queries
6. Run STEP 6 to verify student-related data

Performance Tips:
- Always create indexes before running large update operations
- bulkWrite is faster than updateOne in a loop
- Use $exists to find documents with missing fields
- Text indexes help with search functionality

For production:
- Take backup before running migrations
- Test on staging database first
- Monitor performance of new indexes
- Use appropriate batch sizes for large collections
*/
