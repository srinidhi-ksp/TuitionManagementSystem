package dao;

import java.util.ArrayList;
import java.util.Date;
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
import model.Teacher;

public class TeacherDAO {

    private MongoCollection<Document> teacherCollection;
    private UserDAO userDAO;

    public TeacherDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            teacherCollection = database.getCollection("teachers");
        }
        this.userDAO = new UserDAO();
    }

    public boolean addTeacher(Teacher teacher) {
        if (teacherCollection == null) return false;
        try {
            Document doc = DocumentMapper.teacherToDocument(teacher);
            teacherCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Teacher getTeacherById(String teacherId) {
        if (teacherCollection == null || teacherId == null) return null;
        try {
            Document doc = teacherCollection.find(Filters.or(
                Filters.eq("_id", teacherId),
                Filters.eq("user_id", teacherId)
            )).first();
            Teacher t = DocumentMapper.documentToTeacher(doc);
            if (t != null) enrichWithJoinDate(t, doc);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Document getTeacherExtraDetails(String teacherId) {
        if (teacherCollection == null) return null;
        return teacherCollection.find(Filters.eq("_id", teacherId)).first();
    }

    public boolean deleteTeacher(String userId) {
        if (teacherCollection == null) return false;
        try {
            long deletedCount = teacherCollection.deleteOne(
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

    public boolean updateTeacher(Teacher teacher) {
        if (teacherCollection == null) return false;
        try {
            Document doc = DocumentMapper.teacherToDocument(teacher);
            long matched = teacherCollection.replaceOne(
                Filters.or(
                    Filters.eq("_id",     teacher.getUserId()),
                    Filters.eq("user_id", teacher.getUserId())
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

    private void enrichWithJoinDate(Teacher t, Document doc) {
        Date joinDate = null;
        String userRefId = doc.getString("user_id");
        if (userRefId != null && !userRefId.isEmpty()) {
            joinDate = userDAO.getCreatedAt(userRefId);
        }
        if (joinDate == null && t.getEmail() != null) {
            joinDate = userDAO.getCreatedAtByEmail(t.getEmail());
        }
        if (joinDate == null) {
            joinDate = userDAO.getCreatedAt(t.getUserId());
        }
        if (joinDate != null) {
            t.setJoinDate(new java.text.SimpleDateFormat("dd-MM-yyyy").format(joinDate));
        } else {
            t.setJoinDate("-");
        }
    }

    public List<Teacher> getAllTeachers() {
        List<Teacher> teacherList = new ArrayList<>();
        if (teacherCollection == null) return teacherList;

        try (MongoCursor<Document> cursor = teacherCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Teacher t = DocumentMapper.documentToTeacher(doc);
                if (t != null) {
                    enrichWithJoinDate(t, doc);
                    teacherList.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teacherList;
    }

    /**
     * Get teachers for a specific class
     */
    public List<Teacher> getTeachersByClass(String className) {
        List<Teacher> teacherList = new ArrayList<>();
        if (teacherCollection == null || className == null) return teacherList;

        try (MongoCursor<Document> cursor = teacherCollection.find(
            Filters.in("classes", className)
        ).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Teacher t = DocumentMapper.documentToTeacher(doc);
                if (t != null) {
                    enrichWithJoinDate(t, doc);
                    teacherList.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println("[TeacherDAO] Error getting teachers by class: " + e.getMessage());
            e.printStackTrace();
        }
        return teacherList;
    }

    public Teacher getByUserId(String userId) {
        if (teacherCollection == null || userId == null) return null;
        try {
            String searchId = userId.trim();
            Document doc = teacherCollection.find(Filters.or(
                Filters.eq("user_id", searchId),
                Filters.eq("_id", searchId)
            )).first();

            if (doc == null) {
                model.User user = userDAO.getUserById(searchId);
                if (user != null && user.getEmail() != null) {
                    doc = teacherCollection.find(Filters.eq("email", user.getEmail())).first();
                }
            }

            Teacher teacher = DocumentMapper.documentToTeacher(doc);
            if (teacher != null) enrichWithJoinDate(teacher, doc);
            return teacher;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ══════════════════════════════════════════════════
    // NEW: Dynamic filtering with optional ranges
    // ══════════════════════════════════════════════════

    /**
     * Fetches filtered teachers. All parameters are optional — pass null/"All" to skip.
     *
     * @param specialization exact specialization string or null/"All"
     * @param city           exact city string or null/"All"
     * @param expMin         minimum experience years (-1 to skip)
     * @param expMax         maximum experience years (-1 to skip, use Integer.MAX_VALUE for open-ended)
     * @param salMin         minimum salary (-1 to skip)
     * @param salMax         maximum salary (-1 to skip, use Double.MAX_VALUE for open-ended)
     * @param search         regex on full_name; null/"" to skip
     * @param sortBy         "salary", "experience_years", or null (default: name)
     */
    public List<Teacher> getTeachersFiltered(String specialization, String city,
                                              int expMin, int expMax,
                                              double salMin, double salMax,
                                              String search, String sortBy) {
        List<Teacher> result = new ArrayList<>();
        if (teacherCollection == null) return result;

        try {
            List<Bson> conditions = new ArrayList<>();

            if (specialization != null && !specialization.isEmpty() && !"All".equals(specialization)) {
                conditions.add(Filters.eq("specialization", specialization));
            }
            if (city != null && !city.isEmpty() && !"All".equals(city)) {
                conditions.add(Filters.eq("city", city));
            }
            if (expMin >= 0 && expMax >= 0) {
                conditions.add(Filters.and(
                    Filters.gte("experience_years", expMin),
                    Filters.lte("experience_years", expMax)
                ));
            }
            if (salMin >= 0 && salMax >= 0) {
                conditions.add(Filters.and(
                    Filters.gte("salary", salMin),
                    Filters.lte("salary", salMax)
                ));
            }
            if (search != null && !search.trim().isEmpty()) {
                String regex = search.trim();
                conditions.add(Filters.or(
                    Filters.regex("full_name", regex, "i"),
                    Filters.regex("_id", regex, "i")
                ));
            }

            Bson query = conditions.isEmpty() ? new Document() : Filters.and(conditions);

            // Sort
            Document sort;
            if ("salary".equals(sortBy)) {
                sort = new Document("salary", -1);
            } else if ("experience_years".equals(sortBy)) {
                sort = new Document("experience_years", -1);
            } else {
                sort = new Document("full_name", 1);
            }

            try (MongoCursor<Document> cursor = teacherCollection.find(query).sort(sort).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Teacher t = DocumentMapper.documentToTeacher(doc);
                    if (t != null) {
                        enrichWithJoinDate(t, doc);
                        result.add(t);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TeacherDAO] Error in getTeachersFiltered: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns distinct city values from the teachers collection.
     */
    public List<String> getDistinctCities() {
        List<String> cities = new ArrayList<>();
        if (teacherCollection == null) return cities;
        try {
            teacherCollection.distinct("city", String.class).into(cities);
            cities.removeIf(c -> c == null || c.trim().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cities;
    }

    /**
     * Returns distinct specialization values from the teachers collection.
     */
    public List<String> getDistinctSpecializations() {
        List<String> specs = new ArrayList<>();
        if (teacherCollection == null) return specs;
        try {
            teacherCollection.distinct("specialization", String.class).into(specs);
            specs.removeIf(s -> s == null || s.trim().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return specs;
    }
}
