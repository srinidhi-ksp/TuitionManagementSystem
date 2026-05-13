package dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import db.DBConnection;
import db.DocumentMapper;
import model.Parent;

/**
 * Parent DAO - reads parent records from both the legacy parents collection and
 * embedded students.parent documents.
 */
public class ParentDAO {

// parentCollection removed as per requirement
    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> userCollection;

    public ParentDAO() {
        try {
            MongoDatabase database = DBConnection.getDatabase();
            if (database != null) {
                studentCollection = database.getCollection("students");
                userCollection = database.getCollection("users");
                System.out.println("[ParentDAO] Connected to student/user collections");
            } else {
                System.err.println("[ParentDAO] Database connection failed!");
            }
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Parent getByUserId(String userId) {
        if (userId == null) return null;
        try {
            Document studentDoc = findStudentByParentIdentifier(userId);
            if (studentDoc == null) return null;

            Document parentDoc = studentDoc.get("parent", Document.class);
            if (parentDoc == null) return null;

            Parent parent = DocumentMapper.documentToParent(parentDoc);
            addLinkedStudent(parent, studentDoc);
            return enrichFromUser(parent, userId);
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error fetching parent by user_id: " + e.getMessage());
            return null;
        }
    }

    public Parent getByEmail(String email) {
        if (email == null) return null;
        try {
            Document userDoc = userCollection != null ? userCollection.find(Filters.eq("email", email)).first() : null;
            if (userDoc != null) {
                Parent byUser = getByUserId(stringValue(userDoc.get("_id")));
                if (byUser != null) return byUser;
            }

            return null;
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error fetching parent by email: " + e.getMessage());
            return null;
        }
    }

    public boolean addParent(Parent p) {
        // We do not insert into a separate parents collection anymore.
        // Parents are added by embedding them in student documents.
        return true;
    }

    public List<Parent> getAllParents() {
        return getAllParentsWithPhone();
    }

    public List<Parent> getAllParentsWithPhone() {
        Map<String, Parent> parentsById = new LinkedHashMap<>();

        try {
            if (studentCollection != null) {
                for (Document studentDoc : studentCollection.find(Filters.exists("parent"))) {
                    Document parentDoc = studentDoc.get("parent", Document.class);
                    if (parentDoc == null) continue;

                    Parent embedded = DocumentMapper.documentToParent(parentDoc);
                    if (embedded == null || embedded.getUserId() == null) continue;

                    Parent existing = parentsById.get(embedded.getUserId());
                    Parent target = existing != null ? mergeParent(existing, embedded) : embedded;
                    addLinkedStudent(target, studentDoc);
                    parentsById.put(target.getUserId(), enrichFromMatchingUser(target));
                }
            }
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error in getAllParentsWithPhone: " + e.getMessage());
            e.printStackTrace();
        }

        return new ArrayList<>(parentsById.values());
    }

    public boolean updateParent(Parent p) {
        if (p == null || p.getUserId() == null) return false;
        try {
            boolean updated = false;

            if (studentCollection != null) {
                long modified = studentCollection.updateMany(
                    parentIdentifierFilter(p.getUserId()),
                    Updates.combine(
                        Updates.set("parent.full_name", p.getName()),
                        Updates.set("parent.phone", p.getPhone()),
                        Updates.set("parent.preferred_language", p.getPreferredLanguage()),
                        Updates.set("parent.occupation", p.getOccupation()),
                        Updates.set("parent.salary", p.getAnnualIncome()),
                        Updates.set("parent.annual_income", p.getAnnualIncome()),
                        Updates.set("parent.emergency_contact", p.getEmergencyContact()),
                        Updates.set("parent.relation", p.getRelationType())
                    )
                ).getModifiedCount();
                updated = updated || modified > 0;
            }

            if (userCollection != null) {
                long modified = userCollection.updateOne(
                    Filters.eq("_id", p.getUserId()),
                    Updates.combine(
                        Updates.set("phone", p.getPhone()),
                        Updates.set("phones", java.util.Collections.singletonList(p.getPhone()))
                    )
                ).getModifiedCount();
                updated = updated || modified > 0;
            }

            return updated;
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error updating parent: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteParent(String parentId) {
        if (parentId == null) return false;
        try {
            boolean deleted = false;

            if (studentCollection != null) {
                long count = studentCollection.updateMany(
                    parentIdentifierFilter(parentId),
                    Updates.unset("parent")
                ).getModifiedCount();
                deleted = deleted || count > 0;
            }

            if (userCollection != null) {
                long count = userCollection.deleteMany(Filters.eq("_id", parentId)).getDeletedCount();
                deleted = deleted || count > 0;
            }

            return deleted;
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error deleting parent: " + e.getMessage());
            return false;
        }
    }

    private Document findStudentByParentIdentifier(String identifier) {
        if (studentCollection == null) return null;

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("parent_user_id", identifier));
        filters.add(Filters.eq("parent.user_id", identifier));
        filters.add(Filters.eq("parent.parent_id", identifier));
        filters.add(Filters.eq("parent.email", identifier));

        Document userDoc = findUser(identifier);
        if (userDoc != null) {
            String email = userDoc.getString("email");
            if (email != null) filters.add(Filters.eq("parent.email", email));

            String phone = userDoc.getString("phone");
            if (phone != null) filters.add(Filters.eq("parent.phone", phone));

            List<String> phones = userDoc.getList("phones", String.class);
            if (phones != null) {
                for (String p : phones) {
                    if (p != null) filters.add(Filters.eq("parent.phone", p));
                }
            }
        }

        return studentCollection.find(Filters.or(filters)).first();
    }

    private Bson parentIdentifierFilter(String identifier) {
        return Filters.or(
            Filters.eq("parent.user_id", identifier),
            Filters.eq("parent.parent_id", identifier),
            Filters.eq("parent.email", identifier),
            Filters.eq("parent.phone", identifier)
        );
    }

    private Parent enrichFromMatchingUser(Parent parent) {
        if (parent == null || userCollection == null) return parent;

        Document userDoc = null;
        if (parent.getEmail() != null) {
            userDoc = userCollection.find(Filters.eq("email", parent.getEmail())).first();
        }
        if (userDoc == null && parent.getPhone() != null) {
            userDoc = userCollection.find(Filters.or(
                Filters.eq("phone", parent.getPhone()),
                Filters.eq("phones", parent.getPhone())
            )).first();
        }

        applyUserFields(parent, userDoc);
        return parent;
    }

    private Parent enrichFromUser(Parent parent, String loginIdentifier) {
        applyUserFields(parent, findUser(loginIdentifier));
        if (parent.getEmail() == null || parent.getPhone() == null) {
            enrichFromMatchingUser(parent);
        }
        return parent;
    }

    private Document findUser(String identifier) {
        if (userCollection == null || identifier == null) return null;
        return userCollection.find(Filters.or(
            Filters.eq("_id", identifier),
            Filters.eq("email", identifier)
        )).first();
    }

    private void applyUserFields(Parent parent, Document userDoc) {
        if (parent == null || userDoc == null) return;
        if (parent.getEmail() == null) parent.setEmail(userDoc.getString("email"));

        String phone = userDoc.getString("phone");
        if (phone == null || phone.isBlank()) {
            List<String> phones = userDoc.getList("phones", String.class);
            if (phones != null && !phones.isEmpty()) phone = phones.get(0);
        }
        if (parent.getPhone() == null || parent.getPhone().isBlank()) parent.setPhone(phone);
    }

    private Parent mergeParent(Parent primary, Parent fallback) {
        if (primary.getName() == null) primary.setName(fallback.getName());
        if (primary.getPhone() == null) primary.setPhone(fallback.getPhone());
        if (primary.getOccupation() == null) primary.setOccupation(fallback.getOccupation());
        if (primary.getRelationType() == null) primary.setRelationType(fallback.getRelationType());
        if (primary.getAnnualIncome() == 0) primary.setAnnualIncome(fallback.getAnnualIncome());
        if (primary.getPreferredLanguage() == null) primary.setPreferredLanguage(fallback.getPreferredLanguage());
        if (primary.getEmergencyContact() == 0) primary.setEmergencyContact(fallback.getEmergencyContact());
        return primary;
    }

    private void addLinkedStudent(Parent parent, Document studentDoc) {
        if (parent == null || studentDoc == null) return;
        if (parent.getLinkedStudentIds() == null) parent.setLinkedStudentIds(new ArrayList<>());
        String studentId = stringValue(studentDoc.get("_id"));
        if (studentId != null && !parent.getLinkedStudentIds().contains(studentId)) {
            parent.getLinkedStudentIds().add(studentId);
        }
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
