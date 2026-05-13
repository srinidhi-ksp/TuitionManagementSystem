package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import db.DBConnection;
import db.DocumentMapper;
import model.Student;

public class StudentDAO {

    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> enrollmentCollection;

    public StudentDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            studentCollection = database.getCollection("students");
            enrollmentCollection = database.getCollection("enrollments");
        }
    }

    public boolean addStudent(Student student) {
        if (studentCollection == null) return false;
        try {
            Document doc = DocumentMapper.studentToDocument(student);
            studentCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Student getStudentById(String userId) {
        if (studentCollection == null) return null;
        try {
            Document doc = studentCollection.find(
                Filters.or(
                    Filters.eq("_id", userId),
                    Filters.eq("user_id", userId)
                )
            ).first();
            return DocumentMapper.documentToStudent(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * CRITICAL: Robust mapping: user_id (U01) -> Student object (S001)
     * Uses multiple strategies to find student by user ID
     */
    public Student getStudentByUserId(String userIdValue) {
        if (studentCollection == null || userIdValue == null) {
            System.err.println("[StudentDAO] ❌ getStudentByUserId: Collection or ID is null");
            return null;
        }

        try {
            String searchId = userIdValue.trim();
            System.out.println("[StudentDAO] 🔍 Attempting to map User ID: '" + searchId + "'");

            // STRATEGY 1: Try exact match on 'user_id' field (if documents have this field)
            Document doc = studentCollection.find(Filters.eq("user_id", searchId)).first();
            if (doc != null) {
                System.out.println("[StudentDAO]   ✅ Found via user_id field");
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) {
                    System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                }
                return s;
            }
            System.out.println("[StudentDAO]   ⚠️  No match on user_id field");

            // STRATEGY 2: Try match on email (most reliable cross-reference)
            System.out.println("[StudentDAO]   🔄 Trying email-based lookup...");
            UserDAO userDAO = new UserDAO();
            model.User user = userDAO.getUserById(searchId);
            if (user != null && user.getEmail() != null) {
                String email = user.getEmail();
                System.out.println("[StudentDAO]   Searching by email: " + email);
                doc = studentCollection.find(Filters.eq("email", email)).first();
                if (doc != null) {
                    System.out.println("[StudentDAO]   ✅ Found via email field");
                    Student s = DocumentMapper.documentToStudent(doc);
                    if (s != null) {
                        System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                    }
                    return s;
                }
            }
            System.out.println("[StudentDAO]   ⚠️  No match on email field");

            // STRATEGY 3: Fallback - Check if the provided ID is already the Student ID (_id)
            if (searchId.startsWith("S")) {
                System.out.println("[StudentDAO]   🔄 ID starts with 'S', trying as student_id...");
                doc = studentCollection.find(Filters.eq("_id", searchId)).first();
                if (doc != null) {
                    System.out.println("[StudentDAO]   ✅ Found via _id field (already student_id)");
                    Student s = DocumentMapper.documentToStudent(doc);
                    if (s != null) {
                        System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                    }
                    return s;
                }
            }

            System.err.println("[StudentDAO] ❌ FAILED to map User ID: " + searchId + " (all strategies exhausted)");
            return null;

        } catch (Exception e) {
            System.err.println("[StudentDAO] ❌ Error in getStudentByUserId: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Student getStudentByEmail(String email) {
        if (studentCollection == null) return null;
        try {
            Document doc = studentCollection.find(
                Filters.eq("email", email)
            ).first();
            return DocumentMapper.documentToStudent(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✅ FIX: Fetch Student + Enrollment + Batch info
     */
    public Student getStudentFullDetails(String studentId) {
        MongoDatabase db = DBConnection.getDatabase();
        MongoCollection<Document> studentCol = db.getCollection("students");
        MongoCollection<Document> enrollCol = db.getCollection("enrollments");
        MongoCollection<Document> batchCol = db.getCollection("batches");

        Document studentDoc = studentCol.find(Filters.eq("_id", studentId)).first();
        if (studentDoc == null) {
            studentDoc = studentCol.find(Filters.eq("user_id", studentId)).first();
        }

        if (studentDoc == null) return null;

        Student s = DocumentMapper.documentToStudent(studentDoc);

        // ✅ ENROLLMENT JOIN
        Document enrollDoc = enrollCol.find(Filters.or(
            Filters.eq("student_id", studentId),
            Filters.eq("student_user_id", studentId),
            Filters.eq("user_id", studentId)
        )).first();

        if (enrollDoc != null) {
            s.setJoinDate(enrollDoc.getDate("enroll_date"));

            Object bIdObj = enrollDoc.get("batch_id");
            if (bIdObj != null) {
                Bson batchFilter;
                if (bIdObj instanceof Integer) {
                    batchFilter = Filters.eq("_id", (Integer) bIdObj);
                } else {
                    batchFilter = Filters.eq("_id", bIdObj.toString());
                }

                Document batchDoc = batchCol.find(batchFilter).first();
                if (batchDoc != null) {
                    s.setCurrentStd(batchDoc.getString("standard"));
                    s.setBoard(batchDoc.getString("board"));
                }
            }
        }
        return s;
    }


    public boolean deleteStudent(String userId) {
        if (studentCollection == null) return false;
        try {
            long deletedCount = studentCollection.deleteOne(
                Filters.or(
                    Filters.eq("_id", userId),
                    Filters.eq("user_id", userId)
                )
            ).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStudent(Student student) {
        if (studentCollection == null) return false;
        try {
            Document doc = DocumentMapper.studentToDocument(student);
            long matched = studentCollection.replaceOne(
                Filters.or(
                    Filters.eq("_id",     student.getUserId()),
                    Filters.eq("user_id", student.getUserId())
                ),
                doc,
                new ReplaceOptions().upsert(false)
            ).getMatchedCount();
            return matched > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Student> getAllStudents() {
        List<Student> studentList = new ArrayList<>();
        if (studentCollection == null) return studentList;

        try (MongoCursor<Document> cursor = studentCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) {
                    studentList.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return studentList;
    }

    public List<Student> getStudentsByIds(List<String> userIds) {
        List<Student> studentList = new ArrayList<>();
        if (studentCollection == null || userIds.isEmpty()) return studentList;

        try (MongoCursor<Document> cursor = studentCollection.find(Filters.in("user_id", userIds)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) studentList.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return studentList;
    }

    /**
     * ✅ Fetch all students linked to a specific parent (by users._id)
     */
    public List<Student> getStudentsByParentUserId(String parentUserId) {
        List<Student> studentList = new ArrayList<>();
        if (studentCollection == null || parentUserId == null) return studentList;

        try (MongoCursor<Document> cursor = studentCollection.find(buildParentLookupFilter(parentUserId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) studentList.add(s);
            }
        } catch (Exception e) {
            System.err.println("[StudentDAO] Error fetching students by parent_user_id: " + e.getMessage());
        }
        return studentList;
    }

    // ══════════════════════════════════════════════════
    // NEW: Active/Inactive detection via enrollments
    // ══════════════════════════════════════════════════

    private Bson buildParentLookupFilter(String parentIdentifier) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> phones = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> emails = new java.util.LinkedHashSet<>();

        addIfPresent(ids, parentIdentifier);

        MongoDatabase db = DBConnection.getDatabase();
        if (db != null) {
            MongoCollection<Document> users = db.getCollection("users");
            Document userDoc = users.find(Filters.or(
                Filters.eq("_id", parentIdentifier),
                Filters.eq("email", parentIdentifier)
            )).first();

            if (userDoc != null) {
                addIfPresent(ids, stringValue(userDoc.get("_id")));
                addIfPresent(emails, userDoc.getString("email"));
                addIfPresent(phones, userDoc.getString("phone"));
                List<String> userPhones = userDoc.getList("phones", String.class);
                if (userPhones != null) {
                    for (String phone : userPhones) addIfPresent(phones, phone);
                }
            }

            MongoCollection<Document> parents = db.getCollection("parents");
            java.util.List<Bson> parentFilters = new java.util.ArrayList<>();
            parentFilters.add(Filters.eq("user_id", parentIdentifier));
            parentFilters.add(Filters.eq("_id", parentIdentifier));
            parentFilters.add(Filters.eq("email", parentIdentifier));
            for (String phone : phones) parentFilters.add(Filters.eq("phone", phone));

            Document parentDoc = parents.find(Filters.or(parentFilters)).first();
            if (parentDoc != null) {
                addIfPresent(ids, parentDoc.getString("user_id"));
                addIfPresent(ids, stringValue(parentDoc.get("_id")));
                addIfPresent(emails, parentDoc.getString("email"));
                addIfPresent(phones, parentDoc.getString("phone"));
            }
        }

        java.util.List<Bson> filters = new java.util.ArrayList<>();
        for (String id : ids) {
            filters.add(Filters.eq("parent_user_id", id));
            filters.add(Filters.eq("parent.user_id", id));
            filters.add(Filters.eq("parent.parent_id", id));
        }
        for (String phone : phones) filters.add(Filters.eq("parent.phone", phone));
        for (String email : emails) filters.add(Filters.eq("parent.email", email));

        return filters.isEmpty() ? Filters.eq("parent_user_id", parentIdentifier) : Filters.or(filters);
    }

    private static void addIfPresent(java.util.Set<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value.trim());
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Returns distinct student IDs that have at least one ACTIVE enrollment.
     * Checks both 'student_user_id' and 'student_id' field names.
     */
    public List<String> getActiveStudentIds() {
        List<String> ids = new ArrayList<>();
        if (enrollmentCollection == null) return ids;
        try {
            // Check student_user_id field
            enrollmentCollection
                .distinct("student_user_id", Filters.eq("status", "ACTIVE"), String.class)
                .into(ids);
            // Also check student_id field for alternate schema
            List<String> alt = new ArrayList<>();
            enrollmentCollection
                .distinct("student_id", Filters.eq("status", "ACTIVE"), String.class)
                .into(alt);
            for (String id : alt) {
                if (id != null && !ids.contains(id)) ids.add(id);
            }
        } catch (Exception e) {
            System.err.println("[StudentDAO] Error fetching active student IDs: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Returns count of active students (have at least one ACTIVE enrollment).
     */
    public int countActiveStudents() {
        return getActiveStudentIds().size();
    }

    /**
     * Returns count of inactive students (no ACTIVE enrollments).
     */
    public int countInactiveStudents() {
        int total = (int) (studentCollection != null ? studentCollection.countDocuments() : 0);
        return total - countActiveStudents();
    }

    /**
     * Fetches filtered students based on tab (ALL/ACTIVE/INACTIVE), dropdown filters,
     * and an optional name/ID search string.
     *
     * @param tab       "ALL", "ACTIVE", or "INACTIVE"
     * @param standard  null or "All" to skip, otherwise exact match
     * @param board     null or "All" to skip, otherwise exact match
     * @param city      null or "All" to skip, otherwise exact match
     * @param search    null or "" to skip; otherwise regex on full_name or _id
     */
    public List<Student> getStudentsFiltered(String tab, String standard, String board,
                                              String city, String search) {
        List<Student> result = new ArrayList<>();
        if (studentCollection == null) return result;

        try {
            List<Bson> conditions = new ArrayList<>();

            // ── Active / Inactive tab filter ──
            if ("ACTIVE".equalsIgnoreCase(tab)) {
                List<String> activeIds = getActiveStudentIds();
                if (activeIds.isEmpty()) return result; // no active students
                conditions.add(Filters.in("_id", activeIds));
            } else if ("INACTIVE".equalsIgnoreCase(tab)) {
                List<String> activeIds = getActiveStudentIds();
                conditions.add(Filters.nin("_id", activeIds));
            }

            // ── Standard filter ──
            if (standard != null && !standard.isEmpty() && !"All".equals(standard)) {
                conditions.add(Filters.or(
                    Filters.eq("standard", standard),
                    Filters.eq("current_std", standard)
                ));
            }

            // ── Board filter ──
            if (board != null && !board.isEmpty() && !"All".equals(board)) {
                conditions.add(Filters.eq("board", board));
            }

            // ── City filter ──
            if (city != null && !city.isEmpty() && !"All".equals(city)) {
                conditions.add(Filters.eq("city", city));
            }

            // ── Search filter (name or ID) ──
            if (search != null && !search.trim().isEmpty()) {
                String regex = search.trim();
                conditions.add(Filters.or(
                    Filters.regex("full_name", regex, "i"),
                    Filters.regex("_id", regex, "i")
                ));
            }

            Bson query = conditions.isEmpty() ? new Document() : Filters.and(conditions);

            try (MongoCursor<Document> cursor = studentCollection.find(query).iterator()) {
                while (cursor.hasNext()) {
                    Student s = DocumentMapper.documentToStudent(cursor.next());
                    if (s != null) result.add(s);
                }
            }
        } catch (Exception e) {
            System.err.println("[StudentDAO] Error in getStudentsFiltered: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns distinct city values from the students collection for populating dropdowns.
     */
    public List<String> getDistinctCities() {
        List<String> cities = new ArrayList<>();
        if (studentCollection == null) return cities;
        try {
            studentCollection.distinct("city", String.class).into(cities);
            cities.removeIf(c -> c == null || c.trim().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cities;
    }
}
