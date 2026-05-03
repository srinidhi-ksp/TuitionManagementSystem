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

    public void upsertSalary(SalaryRecord record) {
        if (salaryCollection == null) return;

        Document doc = new Document()
            .append("teacher_id", record.getTeacherId())
            .append("month", record.getMonth())
            .append("year", record.getYear())
            .append("total_days", record.getTotalDays())
            .append("present_days", record.getPresentDays())
            .append("absent_days", record.getAbsentDays())
            .append("per_day_salary", record.getPerDaySalary())
            .append("deduction", record.getDeduction())
            .append("final_salary", record.getFinalSalary())
            .append("last_updated", new Date());

        salaryCollection.updateOne(
            Filters.eq("_id", record.getId()),
            new Document("$set", doc),
            new UpdateOptions().upsert(true)
        );
    }

    public List<SalaryRecord> getAllSalaryRecords(String month, String year) {
        List<SalaryRecord> list = new ArrayList<>();
        if (salaryCollection == null) return list;

        for (Document doc : salaryCollection.find(Filters.and(
            Filters.eq("month", month),
            Filters.eq("year", year)
        ))) {
            SalaryRecord r = new SalaryRecord();
            r.setId(doc.getString("_id"));
            r.setTeacherId(doc.getString("teacher_id"));
            r.setMonth(doc.getString("month"));
            r.setYear(doc.getString("year"));
            r.setTotalDays(doc.getInteger("total_days", 0));
            r.setPresentDays(doc.getInteger("present_days", 0));
            r.setAbsentDays(doc.getInteger("absent_days", 0));
            r.setPerDaySalary(doc.getDouble("per_day_salary"));
            r.setDeduction(doc.getDouble("deduction"));
            r.setFinalSalary(doc.getDouble("final_salary"));
            r.setLastUpdated(doc.getDate("last_updated"));
            list.add(r);
        }
        return list;
    }
}
