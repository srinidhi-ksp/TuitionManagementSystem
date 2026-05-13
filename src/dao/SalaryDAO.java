package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import db.DBConnection;
import model.SalaryRecord;

public class SalaryDAO {
    private MongoCollection<Document> salaryCollection;

    public SalaryDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            this.salaryCollection = database.getCollection("salary_records");
        }
    }

    /** Upsert a salary record — now persists extra_slots and extra_bonus. */
    public void upsertSalary(SalaryRecord record) {
        if (salaryCollection == null) return;

        Document doc = new Document()
            .append("teacher_id",   record.getTeacherId())
            .append("month",        record.getMonth())
            .append("year",         record.getYear())
            .append("total_days",   record.getTotalDays())
            .append("present_days", record.getPresentDays())
            .append("absent_days",  record.getAbsentDays())
            .append("per_day_salary", record.getPerDaySalary())
            .append("deduction",    record.getDeduction())
            .append("extra_slots",  record.getExtraSlots())
            .append("extra_bonus",  record.getExtraBonus())
            .append("final_salary", record.getFinalSalary())
            .append("last_updated", new Date());

        salaryCollection.updateOne(
            Filters.eq("_id", record.getId()),
            new Document("$set", doc),
            new UpdateOptions().upsert(true)
        );

        // ── Notification: SALARY_PROCESSED ────────────────────────────────────
        new Thread(() -> {
            try {
                String teacherId   = record.getTeacherId();
                String monthLabel  = record.getMonth() + "/" + record.getYear();
                double baseSalary  = record.getPerDaySalary() * record.getTotalDays();
                double deduction   = record.getDeduction();
                double extraBonus  = record.getExtraBonus();
                int    extraSlots  = record.getExtraSlots();
                double finalSalary = record.getFinalSalary();
                int    absentDays  = record.getAbsentDays();

                // Resolve teacher name
                String teacherName = teacherId;
                try {
                    com.mongodb.client.MongoDatabase mdb = db.DBConnection.getDatabase();
                    if (mdb != null) {
                        Document tDoc = mdb.getCollection("teachers").find(
                            Filters.or(
                                Filters.eq("_id", teacherId),
                                Filters.eq("user_id", teacherId)
                            )
                        ).first();
                        if (tDoc != null) {
                            String n = tDoc.getString("full_name");
                            if (n == null) n = tDoc.getString("name");
                            if (n != null) teacherName = n;
                        }
                    }
                } catch (Exception ignored) {}

                service.NotificationService ns = service.NotificationService.getInstance();

                // Always: salary processed notification
                ns.push(new service.NotificationDocument(
                    service.NotificationService.ROLE_TEACHER, teacherId,
                    service.NotificationService.SALARY_PROCESSED,
                    "Salary Processed — " + monthLabel,
                    String.format("Your salary for %s has been processed. " +
                        "Base: ₹%,.0f | Deduction: ₹%,.0f | Extra Bonus: ₹%,.0f | Net: ₹%,.0f. " +
                        "Contact admin for any queries.",
                        monthLabel, baseSalary, deduction, extraBonus, finalSalary))
                    .month(record.getMonth()).year(record.getYear()));

                // Deduction notice (if applicable)
                if (deduction > 0 && absentDays > 0) {
                    ns.push(new service.NotificationDocument(
                        service.NotificationService.ROLE_TEACHER, teacherId,
                        service.NotificationService.SALARY_DEDUCTION,
                        "Salary Deduction Notice — " + monthLabel,
                        String.format("A deduction of ₹%,.0f has been applied to your %s salary " +
                            "due to %d absent day(s). Please maintain regular attendance.",
                            deduction, monthLabel, absentDays))
                        .month(record.getMonth()).year(record.getYear()));
                }

                // Bonus notice (if applicable)
                if (extraBonus > 0 && extraSlots > 0) {
                    ns.push(new service.NotificationDocument(
                        service.NotificationService.ROLE_TEACHER, teacherId,
                        service.NotificationService.SALARY_BONUS,
                        "Extra Slot Bonus — " + monthLabel,
                        String.format("You have been credited ₹%,.0f as a bonus for teaching " +
                            "%d extra slot(s) in %s. Thank you for your dedication!",
                            extraBonus, extraSlots, monthLabel))
                        .month(record.getMonth()).year(record.getYear()));
                }

                // Admin summary
                final String finalTeacherName = teacherName;
                ns.push(new service.NotificationDocument(
                    service.NotificationService.ROLE_ADMIN, "ADMIN",
                    service.NotificationService.SALARY_PROCESSED,
                    "Salary Processed — " + finalTeacherName + " (" + monthLabel + ")",
                    String.format("Salary for %s (%s) processed. Net: ₹%,.0f | " +
                        "Deduction: ₹%,.0f | Bonus: ₹%,.0f.",
                        finalTeacherName, monthLabel, finalSalary, deduction, extraBonus))
                    .month(record.getMonth()).year(record.getYear()));

            } catch (Exception ex) {
                System.err.println("[SalaryDAO] Notification error: " + ex.getMessage());
            }
        }).start();
        // ── End notification ──────────────────────────────────────────────────
    }


    public List<SalaryRecord> getAllSalaryRecords(String month, String year) {
        List<SalaryRecord> list = new ArrayList<>();
        if (salaryCollection == null) return list;

        for (Document doc : salaryCollection.find(Filters.and(
            Filters.eq("month", month),
            Filters.eq("year", year)
        ))) {
            list.add(documentToRecord(doc));
        }
        return list;
    }

    public SalaryRecord findById(String id) {
        if (salaryCollection == null || id == null) return null;
        try {
            Document doc = salaryCollection.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToRecord(doc) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<SalaryRecord> getSalaryByTeacher(String teacherId) {
        List<SalaryRecord> list = new ArrayList<>();
        if (salaryCollection == null) return list;

        for (Document doc : salaryCollection.find(Filters.eq("teacher_id", teacherId))
                .sort(com.mongodb.client.model.Sorts.descending("year", "month"))) {
            list.add(documentToRecord(doc));
        }
        return list;
    }

    // ── Mapping helper ────────────────────────────────────────────────────────

    private static SalaryRecord documentToRecord(Document doc) {
        SalaryRecord r = new SalaryRecord();
        r.setId(doc.getString("_id"));
        r.setTeacherId(doc.getString("teacher_id"));
        r.setMonth(doc.getString("month"));
        r.setYear(doc.getString("year"));
        r.setTotalDays(doc.getInteger("total_days", 0));
        r.setPresentDays(doc.getInteger("present_days", 0));
        r.setAbsentDays(doc.getInteger("absent_days", 0));
        r.setPerDaySalary(toDouble(doc.get("per_day_salary")));
        r.setDeduction(toDouble(doc.get("deduction")));
        // extra_slots / extra_bonus — treat missing as 0 for pre-migration records
        r.setExtraSlots(doc.getInteger("extra_slots", 0));
        r.setExtraBonus(toDouble(doc.get("extra_bonus")));
        r.setFinalSalary(toDouble(doc.get("final_salary")));
        r.setLastUpdated(doc.getDate("last_updated"));
        return r;
    }

    private static double toDouble(Object v) {
        return (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
    }
}
