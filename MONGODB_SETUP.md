# MongoDB Setup & Test Data

This file contains MongoDB commands to set up test data and verify your database is configured correctly.

---

## Step 1: Verify MongoDB is Running

```bash
# In PowerShell or Command Prompt
mongosh
# If successful, you should see: test>
```

If you see errors, MongoDB is not running. Start it:
```bash
mongod
# Or if using MongoDB as a service
# Windows Service: sc start MongoDB
```

---

## Step 2: Create Database and Collection

```javascript
// Switch to the correct database
use tuitionManagementSystem

// Verify we're in the right database
db

// Create the collection by inserting a test document
db.users.insertOne({
  "_id": "ADMIN001",
  "email": "admin@example.com",
  "password": "Admin123456",
  "status": "ACTIVE",
  "created_at": new Date(),
  "roles": ["Admin"]
})

// Verify collection was created
show collections
```

---

## Step 3: Insert Test Users

### Test User 1: Teacher (Matches Sample Data)
```javascript
db.users.insertOne({
  "_id": "T001",
  "email": "rameshkumar@edu.com",
  "password": "RamK#9032",
  "status": "ACTIVE",
  "created_at": ISODate("2026-02-03T00:00:00.000Z"),
  "roles": ["Teacher"],
  "phones": ["9123400001"]
})
```

### Test User 2: Admin
```javascript
db.users.insertOne({
  "_id": "ADMIN002",
  "email": "admin.test@edu.com",
  "password": "AdminPass123",
  "status": "ACTIVE",
  "created_at": new Date(),
  "roles": ["Admin"]
})
```

### Test User 3: Student
```javascript
db.users.insertOne({
  "_id": "S001",
  "email": "student@edu.com",
  "password": "StudentPass123",
  "status": "ACTIVE",
  "created_at": new Date(),
  "roles": ["Student"]
})
```

### Test User 4: Parent
```javascript
db.users.insertOne({
  "_id": "P001",
  "email": "parent@edu.com",
  "password": "ParentPass123",
  "status": "ACTIVE",
  "created_at": new Date(),
  "roles": ["Parent"]
})
```

### Test User 5: Multiple Roles (Admin + Teacher)
```javascript
db.users.insertOne({
  "_id": "T002",
  "email": "admin.teacher@edu.com",
  "password": "AdminTeacher123",
  "status": "ACTIVE",
  "created_at": new Date(),
  "roles": ["Admin", "Teacher"]
})
```

---

## Step 4: Verify Test Users

### View All Users
```javascript
db.users.find()
```

### View Specific User
```javascript
db.users.findOne({email: "rameshkumar@edu.com"})
```

### View Only Emails
```javascript
db.users.find({}, {email: 1, _id: 1, roles: 1})
```

### Count Total Users
```javascript
db.users.countDocuments()
```

---

## Step 5: Expected Console Output When Testing

When you login with the test user (Teacher), you should see:

```
=============================================
[LoginFrame] Login attempt initiated
[LoginFrame] Username/Email: rameshkumar@edu.com
[LoginFrame] Password length: 9
[LoginFrame] Selected Role: Teacher
=============================================

========== LOGIN ATTEMPT ==========
[UserDAO.login] Email: 'rameshkumar@edu.com'
[UserDAO.login] Password length: 9
[UserDAO.login] 🔍 Searching for user with email: 'rameshkumar@edu.com'
[UserDAO.login] ✅ User document found!
[UserDAO.login] 📄 Document Details:
  User ID (_id): T001
  Email: rameshkumar@edu.com
  Stored Password length: 9
  Status: ACTIVE
  Roles: [Teacher]
  Created At: Mon Feb 03 00:00:00 IST 2026
[UserDAO.login] 🔐 Comparing passwords:
  Stored (trimmed): 'RamK#9032' (length: 9)
  Provided (trimmed): 'RamK#9032' (length: 9)
[UserDAO.login] ✅ Password matched successfully!
[UserDAO.login] 🎉 Login SUCCESS!
====================================

[DocumentMapper] Mapping user: T001
[DocumentMapper] Raw roles from DB: [Teacher]
[DocumentMapper] Selected role: 'Teacher'
[DocumentMapper] ✅ User mapped successfully!

[AuthService.login] Starting authentication...
[AuthService.login] Email: rameshkumar@edu.com
[AuthService.login] Selected Role: Teacher
[AuthService.login] ✅ User authenticated: rameshkumar@edu.com
[AuthService.login] User's role in database: 'Teacher'
[AuthService.login] ✅ Role matched! 'Teacher' == 'Teacher'
[AuthService.login] 🎉 Authentication SUCCESS!

[LoginFrame] ✅ Login successful for: rameshkumar@edu.com
```

Then a popup should show: **"Login successful! Welcome, T001"**

---

## Login Test Scenarios

### Scenario 1: Teacher Login ✅
- **Email:** rameshkumar@edu.com
- **Password:** RamK#9032
- **Role:** Teacher
- **Expected Result:** Dashboard opens

### Scenario 2: Admin Login ✅
- **Email:** admin.test@edu.com
- **Password:** AdminPass123
- **Role:** Admin
- **Expected Result:** Admin Dashboard opens

### Scenario 3: Student Login ✅
- **Email:** student@edu.com
- **Password:** StudentPass123
- **Role:** Student
- **Expected Result:** Student Dashboard opens

### Scenario 4: Wrong Password ❌
- **Email:** rameshkumar@edu.com
- **Password:** WrongPassword
- **Role:** Teacher
- **Console Output:** `[UserDAO.login] ❌ Password mismatch!`
- **Expected Result:** Error dialog

### Scenario 5: Wrong Email ❌
- **Email:** nonexistent@edu.com
- **Password:** RamK#9032
- **Role:** Teacher
- **Console Output:** `[UserDAO.login] ❌ No user found with email 'nonexistent@edu.com'`
- **Expected Result:** Error dialog

### Scenario 6: Wrong Role ❌
- **Email:** rameshkumar@edu.com
- **Password:** RamK#9032
- **Role:** Admin (but user is Teacher)
- **Console Output:** `[AuthService.login] ❌ Role mismatch!`
- **Expected Result:** Error dialog

---

## MongoDB Useful Commands

### Delete All Users
```javascript
db.users.deleteMany({})
```

### Delete Specific User
```javascript
db.users.deleteOne({email: "rameshkumar@edu.com"})
```

### Update Password
```javascript
db.users.updateOne(
  {email: "rameshkumar@edu.com"},
  {$set: {password: "NewPassword123"}}
)
```

### Update User Status
```javascript
db.users.updateOne(
  {email: "rameshkumar@edu.com"},
  {$set: {status: "INACTIVE"}}
)
```

### Check Database Info
```javascript
db.stats()
db.users.stats()
```

### Create Index on Email (for faster searches)
```javascript
db.users.createIndex({email: 1})
```

### View Indexes
```javascript
db.users.getIndexes()
```

---

## Troubleshooting

### Issue: Cannot connect to MongoDB
```
Error: MongoServerSelectionError: connect ECONNREFUSED 127.0.0.1:27017
```
**Solution:** Start MongoDB with `mongod`

### Issue: Database doesn't exist
```
use tuitionManagementSystem
db.users.find()
// Returns empty
```
**Solution:** Insert a test document first (see Step 3 above)

### Issue: Users collection is empty
```javascript
db.users.countDocuments()
// Returns 0
```
**Solution:** Insert test data using Step 3 above

### Issue: Getting "No user found" in console but user exists
**Solution:** 
1. Check exact email in database: `db.users.findOne({email: "your-email"})`
2. Compare with what you're entering in the UI
3. Watch for whitespace or case differences

---

## MongoDB Connection String

Your application uses:
```
mongodb://localhost:27017
```

If running MongoDB on a different server, update in [DBConnection.java](src/db/DBConnection.java#L10):

```java
private static final String CONNECTION_STRING = "mongodb://your-server:27017";
```

For authentication:
```java
private static final String CONNECTION_STRING = "mongodb://username:password@your-server:27017";
```

---

## Document Schema Reference

Every user document in the `users` collection should have:

```json
{
  "_id": "T001",                              // Required: Unique identifier
  "email": "rameshkumar@edu.com",             // Required: Email address
  "password": "RamK#9032",                    // Required: Password (plain text or hashed)
  "status": "ACTIVE",                         // Recommended: ACTIVE, INACTIVE, SUSPENDED
  "created_at": ISODate("2026-02-03"),        // Recommended: Account creation date
  "roles": ["Teacher"],                       // Required: Array of roles
  "phones": ["9123400001"],                   // Optional: Array of phone numbers
  "last_login": null,                         // Optional: Last login timestamp
  "name": "Ramesh Kumar",                     // Optional: Full name
  "address": "123 Main St",                   // Optional: Address
  "board": "CBSE"                             // Optional: Education board
}
```

**Minimum Required Fields:**
- `_id` - User ID
- `email` - Email address
- `password` - Password
- `roles` - Array with at least one role

**Valid Roles:**
- "Admin"
- "Teacher"
- "Student"
- "Parent"

---

## Quick Setup Script

Run all commands at once:

```javascript
use tuitionManagementSystem

// Create admin user
db.users.insertOne({_id:"ADMIN001",email:"admin@example.com",password:"Admin123456",status:"ACTIVE",created_at:new Date(),roles:["Admin"]})

// Create teacher user
db.users.insertOne({_id:"T001",email:"rameshkumar@edu.com",password:"RamK#9032",status:"ACTIVE",created_at:ISODate("2026-02-03T00:00:00.000Z"),roles:["Teacher"],phones:["9123400001"]})

// Create student user
db.users.insertOne({_id:"S001",email:"student@edu.com",password:"StudentPass123",status:"ACTIVE",created_at:new Date(),roles:["Student"]})

// Create parent user
db.users.insertOne({_id:"P001",email:"parent@edu.com",password:"ParentPass123",status:"ACTIVE",created_at:new Date(),roles:["Parent"]})

// Verify
db.users.find()
```

---

## Next Steps

1. ✅ Create test users in MongoDB (use commands above)
2. ✅ Compile the application: `.\compile.bat`
3. ✅ Run the application and test login
4. ✅ Monitor console output
5. ✅ Try all test scenarios above
6. ✅ Refer to DEBUGGING_GUIDE.md if issues occur

