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
import dao.SalaryRulesDAO;
import dao.TeacherDAO;
import dao.TeacherExtraSlotDAO;
import db.DBConnection;
import model.SalaryRecord;
import model.SalaryRules;
import model.Teacher;

/**
 * Salary calculation service — implements the escalating deduction + extra-slot bonus model.
 * See requirements §6, §8.
 */
public class SalaryService {

    private SalaryDAO salaryDAO;
    private TeacherDAO teacherDAO;
    private SalaryRulesDAO salaryRulesDAO;
    private TeacherExtraSlotDAO extraSlotDAO;
    private MongoCollection<Document> attendanceCollection;

    public SalaryService() {
        this.salaryDAO       = new SalaryDAO();
        this.teacherDAO      = new TeacherDAO();
        this.salaryRulesDAO  = new SalaryRulesDAO();
        this.extraSlotDAO    = new TeacherExtraSlotDAO();
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            this.attendanceCollection = database.getCollection("attendance");
        }
    }

    /**
     * Calculate (or recalculate) salary for a single teacher for a given month/year.
     * Uses the escalating deduction model from salary_rules and counts extra slots.
     *
     * @param teacherId the teacher's ID (e.g. "T001")
     * @param month     1-based month number
     * @param year      4-digit year
     */
    public void calculateSalary(String teacherId, int month, int year) {
        try {
            // 1. Get teacher base salary
            Teacher teacher = teacherDAO.getTeacherById(teacherId);
            if (teacher == null) return;

            double baseSalary = teacher.getSalary();
            if (baseSalary <= 0) baseSalary = 5000; // Fallback

            // 2. Load salary rules (or use defaults if collection missing)
            SalaryRules rules = salaryRulesDAO.findDefault();

            // 3. Define month range for attendance queries
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1, 0, 0, 0);
            Date startOfMonth = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            Date endOfMonth = cal.getTime();
            int totalWorkingDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            // 4. Fetch attendance records for this teacher in this month
            List<Document> attendances = new ArrayList<>();
            String monthPrefix = String.format("%04d-%02d-", year, month);
            Bson dateFilter = Filters.or(
                Filters.regex("date_str", "^" + monthPrefix),
                Filters.and(Filters.gte("attendance_date", startOfMonth), Filters.lte("attendance_date", endOfMonth)),
                Filters.and(Filters.gte("date", startOfMonth), Filters.lte("date", endOfMonth))
            );

            if (attendanceCollection != null) {
                attendanceCollection.find(Filters.and(
                    Filters.or(Filters.eq("user_id", teacherId), Filters.eq("teacher_id", teacherId)),
                    Filters.eq("type", "TEACHER"),
                    dateFilter
                )).into(attendances);
            }

            int absentDays = 0;
            for (Document doc : attendances) {
                if ("ABSENT".equalsIgnoreCase(doc.getString("status"))) {
                    absentDays++;
                }
            }
            int presentDays = Math.max(0, attendances.size() - absentDays);

            // 5. Escalating deduction — §8.1
            // freeDaysAllowed absent days are grace. After that:
            //   1st chargeable: baseDeductionPerAbsentDay
            //   2nd chargeable: baseDeductionPerAbsentDay + 1 * deductionIncrementPerDay
            //   Nth chargeable: baseDeductionPerAbsentDay + (N-1) * deductionIncrementPerDay
            double deduction = rules.calculateDeduction(absentDays);

            // 6. Extra slot bonus — §8.3
            String monthStr = String.format("%02d", month);
            String yearStr  = String.valueOf(year);
            long extraSlotCount = extraSlotDAO.countByTeacherMonthYear(teacherId, monthStr, yearStr);
            double extraBonus   = rules.calculateBonus((int) extraSlotCount);

            // 7. Per-day salary (for display only)
            double perDaySalary = (totalWorkingDays > 0) ? baseSalary / totalWorkingDays : 0;

            // 8. Final salary — §6.2
            double finalSalary = baseSalary - deduction + extraBonus;

            // 9. Persist record
            String recordId = "SAL_" + teacherId + "_" + yearStr + "_" + monthStr;
            SalaryRecord record = new SalaryRecord();
            record.setId(recordId);
            record.setTeacherId(teacherId);
            record.setMonth(monthStr);
            record.setYear(yearStr);
            record.setTotalDays(totalWorkingDays);
            record.setPresentDays(presentDays);
            record.setAbsentDays(absentDays);
            record.setPerDaySalary(perDaySalary);
            record.setDeduction(deduction);
            record.setExtraSlots((int) extraSlotCount);
            record.setExtraBonus(extraBonus);
            record.setFinalSalary(finalSalary);
            record.setLastUpdated(new Date());

            salaryDAO.upsertSalary(record);
            System.out.println("[SalaryService] ✅ Salary for " + teacherId
                + " (" + month + "/" + year + "): base=" + baseSalary
                + " deduction=" + deduction + " extraBonus=" + extraBonus
                + " final=" + finalSalary);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate salary for a teacher and immediately return the updated record.
     * Used by LogExtraSlotDialog to refresh a single row without full recalculation.
     */
    public SalaryRecord calculateAndReturn(String teacherId, int month, int year) {
        calculateSalary(teacherId, month, year);
        String recordId = "SAL_" + teacherId + "_" + year + "_" + String.format("%02d", month);
        return salaryDAO.findById(recordId);
    }
}
