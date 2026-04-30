package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import db.DBConnection;
import db.DocumentMapper;
import model.Teacher;

/**
 * TeacherFilterDAO handles dynamic filtering of teachers based on various criteria.
 * Supports: Specialization, Experience Range, City, Salary Range, and Search
 */
public class TeacherFilterDAO {

    private MongoCollection<Document> teacherCollection;

    public TeacherFilterDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            teacherCollection = database.getCollection("teachers");
        }
    }

    /**
     * Filter teachers by multiple criteria
     * 
     * @param specialization Selected specialization (null = no filter)
     * @param experienceRange "0-2", "3-5", "5+" (null = no filter)
     * @param city Selected city (null = no filter)
     * @param salaryRange "<20000", "20000-40000", "40000+" (null = no filter)
     * @param searchTerm Search by name or teacher ID (null = no search)
     * @param sortBy "salary", "experience", or null (no sort)
     * @return List of filtered and sorted teachers
     */
    public List<Teacher> filterTeachers(String specialization, String experienceRange,
                                       String city, String salaryRange, 
                                       String searchTerm, String sortBy) {
        List<Teacher> results = new ArrayList<>();
        if (teacherCollection == null) return results;

        try {
            List<Bson> filters = new ArrayList<>();

            // Filter by specialization
            if (specialization != null && !specialization.isEmpty() && !"All".equals(specialization)) {
                filters.add(Filters.eq("specialization", specialization));
            }

            // Filter by experience range
            if (experienceRange != null && !experienceRange.isEmpty() && !"All".equals(experienceRange)) {
                Bson expFilter = buildExperienceFilter(experienceRange);
                if (expFilter != null) {
                    filters.add(expFilter);
                }
            }

            // Filter by city
            if (city != null && !city.isEmpty() && !"All".equals(city)) {
                filters.add(Filters.eq("city", city));
            }

            // Filter by salary range
            if (salaryRange != null && !salaryRange.isEmpty() && !"All".equals(salaryRange)) {
                Bson salaryFilter = buildSalaryFilter(salaryRange);
                if (salaryFilter != null) {
                    filters.add(salaryFilter);
                }
            }

            // Filter by search term (name or ID)
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                filters.add(Filters.or(
                    Filters.regex("full_name", Pattern.quote(searchTerm.trim()), "i"),
                    Filters.regex("_id", Pattern.quote(searchTerm.trim()), "i")
                ));
            }

            // Build query
            Bson query = filters.isEmpty() ? new Document() : Filters.and(filters);

            // Execute query with optional sorting
            MongoCursor<Document> cursor;
            if ("salary".equalsIgnoreCase(sortBy)) {
                cursor = teacherCollection.find(query).sort(Sorts.descending("salary")).iterator();
            } else if ("experience".equalsIgnoreCase(sortBy)) {
                cursor = teacherCollection.find(query).sort(Sorts.descending("experience_years")).iterator();
            } else {
                cursor = teacherCollection.find(query).iterator();
            }

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Teacher teacher = DocumentMapper.documentToTeacher(doc);
                if (teacher != null) {
                    results.add(teacher);
                }
            }
            cursor.close();

        } catch (Exception e) {
            System.err.println("[TeacherFilterDAO] Error filtering teachers: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Build experience range filter
     */
    private Bson buildExperienceFilter(String experienceRange) {
        if ("0-2".equals(experienceRange)) {
            return Filters.and(
                Filters.gte("experience_years", 0),
                Filters.lte("experience_years", 2)
            );
        } else if ("3-5".equals(experienceRange)) {
            return Filters.and(
                Filters.gte("experience_years", 3),
                Filters.lte("experience_years", 5)
            );
        } else if ("5+".equals(experienceRange)) {
            return Filters.gte("experience_years", 5);
        }
        return null;
    }

    /**
     * Build salary range filter
     */
    private Bson buildSalaryFilter(String salaryRange) {
        if ("<20000".equals(salaryRange)) {
            return Filters.lt("salary", 20000);
        } else if ("20000-40000".equals(salaryRange)) {
            return Filters.and(
                Filters.gte("salary", 20000),
                Filters.lte("salary", 40000)
            );
        } else if ("40000+".equals(salaryRange)) {
            return Filters.gte("salary", 40000);
        }
        return null;
    }

    /**
     * Get all unique specialization values from the database
     */
    public List<String> getAllSpecializations() {
        List<String> specs = new ArrayList<>();
        if (teacherCollection == null) return specs;

        try {
            List<String> distinctValues = teacherCollection.distinct("specialization", String.class)
                .into(new ArrayList<>());
            for (String val : distinctValues) {
                if (val != null && !val.isEmpty()) {
                    specs.add(val);
                }
            }
            java.util.Collections.sort(specs);
        } catch (Exception e) {
            System.err.println("[TeacherFilterDAO] Error fetching specializations: " + e.getMessage());
        }

        return specs;
    }

    /**
     * Get all unique city values from the database
     */
    public List<String> getAllCities() {
        List<String> cities = new ArrayList<>();
        if (teacherCollection == null) return cities;

        try {
            List<String> distinctValues = teacherCollection.distinct("city", String.class)
                .into(new ArrayList<>());
            for (String val : distinctValues) {
                if (val != null && !val.isEmpty()) {
                    cities.add(val);
                }
            }
            java.util.Collections.sort(cities);
        } catch (Exception e) {
            System.err.println("[TeacherFilterDAO] Error fetching cities: " + e.getMessage());
        }

        return cities;
    }

    /**
     * Get total teacher count
     */
    public long getTotalTeacherCount() {
        if (teacherCollection == null) return 0;
        try {
            return teacherCollection.countDocuments();
        } catch (Exception e) {
            System.err.println("[TeacherFilterDAO] Error counting teachers: " + e.getMessage());
        }
        return 0;
    }
}
