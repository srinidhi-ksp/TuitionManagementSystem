package service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import dao.SalaryDAO;
import dao.TeacherDAO;
import db.DBConnection;
import model.SalaryRecord;
import model.Teacher;

public class SalaryService {

    private SalaryDAO salaryDAO;
    private TeacherDAO teacherDAO;
    private MongoCollection<Document> attendanceCollection;

    public SalaryService() {
        this.salaryDAO = new SalaryDAO();
        this.teacherDAO = new TeacherDAO();
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            this.attendanceCollection = database.getCollection("attendance");
        }
    }

    public void calculateSalary(String teacherId, int month, int year) {
        try {
            // 1. Get teacher base salary
            Teacher teacher = teacherDAO.getTeacherById(teacherId);
            if (teacher == null) return;
            
            // Assume base_salary is stored in teacher.getSalary()
            double baseSalary = teacher.getSalary();
            if (baseSalary <= 0) baseSalary = 5000; // Fallback as per prompt example

            // 2. Define month range
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1, 0, 0, 0);
            Date startOfMonth = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            Date endOfMonth = cal.getTime();

            // 3. Fetch attendance. CANCELLED records are ignored because only ABSENT is penalized.
            List<Document> attendances = new ArrayList<>();
            String monthPrefix = String.format("%04d-%02d-", year, month);
            Bson dateFilter = Filters.or(
                Filters.regex("date_str", "^" + monthPrefix),
                Filters.and(Filters.gte("attendance_date", startOfMonth), Filters.lte("attendance_date", endOfMonth)),
                Filters.and(Filters.gte("date", startOfMonth), Filters.lte("date", endOfMonth))
            );
            attendanceCollection.find(Filters.and(
                Filters.or(Filters.eq("user_id", teacherId), Filters.eq("teacher_id", teacherId)),
                Filters.eq("type", "TEACHER"),
                dateFilter
            )).into(attendances);

            int totalWorkingDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int absentDays = 0;

            for (Document doc : attendances) {
                if ("ABSENT".equalsIgnoreCase(doc.getString("status"))) {
                    absentDays++;
                }
            }

            int presentDays = Math.max(0, attendances.size() - absentDays);
            
            // Tiered Deduction Logic:
            // 1. If absentDays <= 5: Deduction = 0
            // 2. If absentDays > 5: Deduction = 500 + (absentDays - 5) * 100
            double deduction = 0;
            if (absentDays > 5) {
                deduction = 500 + (absentDays - 5) * 100;
            }
            
            double finalSalary = Math.max(0, baseSalary - deduction);
            double perDaySalary = baseSalary / totalWorkingDays;

            // 4. Save record
            SalaryRecord record = new SalaryRecord();
            record.setId("SAL_" + teacherId + "_" + year + "_" + String.format("%02d", month));
            record.setTeacherId(teacherId);
            record.setMonth(String.format("%02d", month));
            record.setYear(String.valueOf(year));
            record.setTotalDays(totalWorkingDays);
            record.setPresentDays(presentDays);
            record.setAbsentDays(absentDays);
            record.setPerDaySalary(perDaySalary);
            record.setDeduction(deduction);
            record.setFinalSalary(finalSalary);

            salaryDAO.upsertSalary(record);
            System.out.println("[SalaryService] ✅ Recalculated salary for " + teacherId + " (" + month + "/" + year + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
