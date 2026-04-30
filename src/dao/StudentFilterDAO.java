package dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.Student;

/**
 * StudentFilterDAO handles dynamic filtering of students based on various criteria.
 * Supports: Status (Active/Inactive), Standard, Board, City, and Search (by name/ID)
 */
public class StudentFilterDAO {

    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> enrollmentCollection;

    public StudentFilterDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            studentCollection = database.getCollection("students");
            enrollmentCollection = database.getCollection("enrollments");
        }
    }

    /**
     * Retrieves all Student IDs with "ACTIVE" enrollment status
     * ACTIVE = enrolled in at least one batch with status = "ACTIVE"
     */
    public Set<String> getActiveStudentIds() {
        Set<String> activeIds = new HashSet<>();
        if (enrollmentCollection == null) return activeIds;

        try {
            MongoCursor<Document> cursor = enrollmentCollection.find(
                Filters.eq("status", "ACTIVE")
            ).iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String studentId = doc.getString("student_id");
                if (studentId != null) {
                    activeIds.add(studentId);
                }
            }
            cursor.close();
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error fetching active students: " + e.getMessage());
            e.printStackTrace();
        }
        return activeIds;
    }

    /**
     * Filter students by Status, Standard, Board, City and Search criteria
     * 
     * @param status "ALL", "ACTIVE", or "INACTIVE"
     * @param standard Selected standard (null = no filter)
     * @param board Selected board (null = no filter)
     * @param city Selected city (null = no filter)
     * @param searchTerm Search by name or student ID (null = no search)
     * @return List of filtered students
     */
    public List<Student> filterStudents(String status, String standard, String board, 
                                       String city, String searchTerm) {
        List<Student> results = new ArrayList<>();
        if (studentCollection == null) return results;

        try {
            // Step 1: Get active student IDs if needed
            Set<String> activeIds = new HashSet<>();
            if ("ACTIVE".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
                activeIds = getActiveStudentIds();
            }

            // Step 2: Build MongoDB query
            List<Bson> filters = new ArrayList<>();

            // Filter by status
            if ("ACTIVE".equalsIgnoreCase(status)) {
                filters.add(Filters.in("_id", activeIds));
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                filters.add(Filters.nin("_id", activeIds));
            }
            // If status == "ALL", don't add status filter

            // Filter by standard
            if (standard != null && !standard.isEmpty() && !"All".equals(standard)) {
                filters.add(Filters.or(
                    Filters.eq("standard", standard),
                    Filters.eq("current_std", standard)
                ));
            }

            // Filter by board
            if (board != null && !board.isEmpty() && !"All".equals(board)) {
                filters.add(Filters.eq("board", board));
            }

            // Filter by city
            if (city != null && !city.isEmpty() && !"All".equals(city)) {
                filters.add(Filters.eq("city", city));
            }

            // Filter by search term (name or ID)
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchRegex = Pattern.quote(searchTerm.trim());
                Pattern pattern = Pattern.compile(searchRegex, Pattern.CASE_INSENSITIVE);
                filters.add(Filters.or(
                    Filters.regex("full_name", searchRegex, "i"),
                    Filters.regex("_id", searchRegex, "i")
                ));
            }

            // Step 3: Execute query
            Bson query = filters.isEmpty() ? new Document() : Filters.and(filters);
            MongoCursor<Document> cursor = studentCollection.find(query).iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student student = DocumentMapper.documentToStudent(doc);
                if (student != null) {
                    results.add(student);
                }
            }
            cursor.close();

        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error filtering students: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Get all unique standard values from the database
     */
    public List<String> getAllStandards() {
        List<String> standards = new ArrayList<>();
        if (studentCollection == null) return standards;

        try {
            List<String> distinctValues = studentCollection.distinct("standard", String.class).into(new ArrayList<>());
            for (String val : distinctValues) {
                if (val != null && !val.isEmpty()) {
                    standards.add(val);
                }
            }
            
            // Fallback: also check current_std field
            List<String> legacyValues = studentCollection.distinct("current_std", String.class).into(new ArrayList<>());
            for (String val : legacyValues) {
                if (val != null && !val.isEmpty() && !standards.contains(val)) {
                    standards.add(val);
                }
            }

            java.util.Collections.sort(standards);
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error fetching standards: " + e.getMessage());
        }

        return standards;
    }

    /**
     * Get all unique board values from the database
     */
    public List<String> getAllBoards() {
        List<String> boards = new ArrayList<>();
        if (studentCollection == null) return boards;

        try {
            List<String> distinctValues = studentCollection.distinct("board", String.class).into(new ArrayList<>());
            for (String val : distinctValues) {
                if (val != null && !val.isEmpty()) {
                    boards.add(val);
                }
            }
            java.util.Collections.sort(boards);
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error fetching boards: " + e.getMessage());
        }

        return boards;
    }

    /**
     * Get all unique city values from the database
     */
    public List<String> getAllCities() {
        List<String> cities = new ArrayList<>();
        if (studentCollection == null) return cities;

        try {
            List<String> distinctValues = studentCollection.distinct("city", String.class).into(new ArrayList<>());
            for (String val : distinctValues) {
                if (val != null && !val.isEmpty()) {
                    cities.add(val);
                }
            }
            java.util.Collections.sort(cities);
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error fetching cities: " + e.getMessage());
        }

        return cities;
    }

    /**
     * Get count of active students
     */
    public long getActiveStudentCount() {
        if (studentCollection == null || enrollmentCollection == null) return 0;
        try {
            Set<String> activeIds = getActiveStudentIds();
            return studentCollection.countDocuments(Filters.in("_id", activeIds));
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error counting active students: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get count of inactive students
     */
    public long getInactiveStudentCount() {
        if (studentCollection == null || enrollmentCollection == null) return 0;
        try {
            long total = studentCollection.countDocuments();
            Set<String> activeIds = getActiveStudentIds();
            return total - activeIds.size();
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error counting inactive students: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get total student count
     */
    public long getTotalStudentCount() {
        if (studentCollection == null) return 0;
        try {
            return studentCollection.countDocuments();
        } catch (Exception e) {
            System.err.println("[StudentFilterDAO] Error counting total students: " + e.getMessage());
        }
        return 0;
    }
}
