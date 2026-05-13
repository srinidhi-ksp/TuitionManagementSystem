package dao;

import java.util.Date;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

import db.DBConnection;
import model.SalaryRules;

/**
 * DAO for the 'salary_rules' collection.
 * Always works with the single document _id = "DEFAULT".
 */
public class SalaryRulesDAO {

    private MongoCollection<Document> collection;

    public SalaryRulesDAO() {
        MongoDatabase db = DBConnection.getDatabase();
        if (db != null) {
            collection = db.getCollection("salary_rules");
            ensureDefault();
        }
    }

    /** Insert the default rules document if missing. */
    private void ensureDefault() {
        try {
            if (collection.countDocuments(Filters.eq("_id", "DEFAULT")) == 0) {
                collection.insertOne(rulesToDocument(new SalaryRules()));
                System.out.println("[SalaryRulesDAO] Inserted default salary rules.");
            }
        } catch (Exception e) {
            System.err.println("[SalaryRulesDAO] ensureDefault error: " + e.getMessage());
        }
    }

    /** Load the DEFAULT salary rules document; returns hardcoded defaults on failure. */
    public SalaryRules findDefault() {
        if (collection == null) return new SalaryRules();
        try {
            Document doc = collection.find(Filters.eq("_id", "DEFAULT")).first();
            if (doc == null) return new SalaryRules();
            return documentToRules(doc);
        } catch (Exception e) {
            System.err.println("[SalaryRulesDAO] findDefault error: " + e.getMessage());
            return new SalaryRules();
        }
    }

    /** Save / update the DEFAULT salary rules document. */
    public void upsert(SalaryRules rules) {
        if (collection == null || rules == null) return;
        try {
            rules.setLastUpdatedAt(new Date());
            collection.updateOne(
                Filters.eq("_id", "DEFAULT"),
                new Document("$set", rulesToDocument(rules)),
                new UpdateOptions().upsert(true)
            );
            System.out.println("[SalaryRulesDAO] Salary rules saved.");
        } catch (Exception e) {
            System.err.println("[SalaryRulesDAO] upsert error: " + e.getMessage());
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private static SalaryRules documentToRules(Document doc) {
        SalaryRules r = new SalaryRules();
        r.setId(doc.getString("_id"));
        Object free  = doc.get("freeDaysAllowed");
        if (free  instanceof Number) r.setFreeDaysAllowed(((Number)free).intValue());
        Object base  = doc.get("baseDeductionPerAbsentDay");
        if (base  instanceof Number) r.setBaseDeductionPerAbsentDay(((Number)base).intValue());
        Object incr  = doc.get("deductionIncrementPerDay");
        if (incr  instanceof Number) r.setDeductionIncrementPerDay(((Number)incr).intValue());
        Object bonus = doc.get("bonusPerExtraSlot");
        if (bonus instanceof Number) r.setBonusPerExtraSlot(((Number)bonus).intValue());
        r.setLastUpdatedBy(doc.getString("lastUpdatedBy"));
        r.setLastUpdatedAt(doc.getDate("lastUpdatedAt"));
        return r;
    }

    private static Document rulesToDocument(SalaryRules r) {
        Document doc = new Document();
        doc.append("_id",                      r.getId() != null ? r.getId() : "DEFAULT");
        doc.append("freeDaysAllowed",           r.getFreeDaysAllowed());
        doc.append("baseDeductionPerAbsentDay", r.getBaseDeductionPerAbsentDay());
        doc.append("deductionIncrementPerDay",  r.getDeductionIncrementPerDay());
        doc.append("bonusPerExtraSlot",         r.getBonusPerExtraSlot());
        if (r.getLastUpdatedBy() != null) doc.append("lastUpdatedBy", r.getLastUpdatedBy());
        doc.append("lastUpdatedAt", r.getLastUpdatedAt() != null ? r.getLastUpdatedAt() : new Date());
        return doc;
    }
}
