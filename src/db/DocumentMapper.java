package db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import model.Student;
import model.Teacher;
import model.User;

public class DocumentMapper {

    // ====================================
    // USER MAPPER
    // ====================================
    public static User documentToUser(Document doc) {
        if (doc == null) {
            System.err.println("[DocumentMapper] Document is null!");
            return null;
        }
        
        User user = new User();
        Object idObj = doc.get("_id");
        String userId = idObj != null ? idObj.toString() : null;
        user.setUserId(userId);
        System.out.println("[DocumentMapper] Mapping user: " + userId);
        
        user.setEmail(doc.getString("email"));
        String fullName = doc.getString("full_name");
        if (fullName == null) fullName = doc.getString("name");
        user.setName(fullName);
        String phone = doc.getString("phone");
        if (phone == null) {
            List<String> phones = doc.getList("phones", String.class);
            if (phones != null && !phones.isEmpty()) phone = phones.get(0);
        }
        user.setPhone(phone);
        user.setPassword(doc.getString("password"));
        
        // ✅ Get roles as a List (ARRAY from MongoDB)
        List<String> roles = doc.getList("roles", String.class);
        System.out.println("[DocumentMapper] Raw roles array from DB: " + roles);
        
        if (roles != null && !roles.isEmpty()) {
            // Store the complete roles array
            user.setRoles(roles);
            
            // Set primary role: prefer ADMIN if present, otherwise first role
            String primaryRole = roles.get(0);
            for (String r : roles) {
                if (r != null && r.equalsIgnoreCase("admin")) {
                    primaryRole = r;
                    break;
                }
            }
            user.setRole(primaryRole);
            System.out.println("[DocumentMapper] Roles stored: " + roles);
            System.out.println("[DocumentMapper] Primary role set to: '" + primaryRole + "'");
        } else {
            System.err.println("[DocumentMapper] ⚠️  No roles found for user!");
            user.setRole("Unknown");
            user.setRoles(new ArrayList<>());
        }
        
        user.setCreatedAt(doc.getDate("created_at"));
        System.out.println("[DocumentMapper] ✅ User mapped successfully!");
        return user;
    }

    public static Document userToDocument(User user) {
        Document doc = new Document();
        if (user.getUserId() != null) doc.append("_id", user.getUserId());
        doc.append("email", user.getEmail());
        doc.append("password", user.getPassword());
        doc.append("status", "ACTIVE");
        
        if (user.getCreatedAt() != null) doc.append("created_at", user.getCreatedAt());
        else doc.append("created_at", new java.util.Date());
        
        List<String> roles = new ArrayList<>();
        if (user.getRole() != null) roles.add(user.getRole());
        else roles.add("USER");
        doc.append("roles", roles);
        
        return doc;
    }

    // ====================================
    // STUDENT MAPPER
    // ====================================
    public static Student documentToStudent(Document doc) {
        if (doc == null) return null;
        Student s = new Student();
        Object idObj = doc.get("_id");
        s.setUserId(idObj != null ? idObj.toString() : null); // _id in Student matches S001 etc
        s.setName(doc.getString("full_name"));
        s.setDob(doc.getDate("dob"));
        s.setEmail(doc.getString("email"));
        s.setPhone(doc.getString("phone"));

        java.util.Date jd = doc.getDate("joinDate");
        if (jd == null) jd = doc.getDate("join_date");
        s.setJoinDate(jd);

        Object stdObj = doc.get("standard");
        if (stdObj == null) stdObj = doc.get("class");
        if (stdObj == null) stdObj = doc.get("current_std");
        s.setCurrentStd(stdObj != null ? stdObj.toString() : null);
        s.setBoard(doc.getString("board"));

        // city from dedicated field, fallback to address
        String city = doc.getString("city");
        if (city == null) city = doc.getString("address");
        s.setCity(city);
        s.setStreet(doc.getString("street"));

        // ✅ Link to Parent using parent_user_id (Uxx)
        s.setParentUserId(doc.getString("parent_user_id"));

        return s;
    }

    public static Document studentToDocument(Student student) {
        Document doc = new Document();
        if (student.getUserId() != null) doc.append("_id", student.getUserId());

        doc.append("full_name", student.getName());
        if (student.getDob()      != null) doc.append("dob",         student.getDob());
        if (student.getJoinDate() != null) doc.append("join_date",   student.getJoinDate());
        doc.append("email",       student.getEmail());
        // ✅ Persist using DB field name 'standard' (also write current_std for compat)
        if (student.getCurrentStd() != null) {
            doc.append("standard",    student.getCurrentStd());
            doc.append("current_std", student.getCurrentStd());
        }
        if (student.getBoard()      != null) doc.append("board",       student.getBoard());
        if (student.getCity()       != null) doc.append("city",        student.getCity());
        if (student.getStreet()     != null) doc.append("street",      student.getStreet());
        // keep legacy address field populated for backward compat
        if (student.getCity() != null) doc.append("address", student.getCity());

        if (student.getPhone() != null) doc.append("phone", student.getPhone());
        else doc.append("phone", "9999999999"); // Fallback for data integrity

        // ✅ Persist Parent Link
        if (student.getParentUserId() != null) {
            doc.append("parent_user_id", student.getParentUserId());
        }

        return doc;
    }
    
    // ====================================
    // TEACHER MAPPER
    // ====================================
    public static Teacher documentToTeacher(Document doc) {
        if (doc == null) return null;
        Teacher t = new Teacher();
        Object idObj = doc.get("_id");
        t.setUserId(idObj != null ? idObj.toString() : null); // Maps like T001
        t.setAuthUserId(doc.getString("user_id"));
        t.setName(doc.getString("full_name"));
        // ✅ CRITICAL: read email so getCreatedAtByEmail() can look up users.created_at
        t.setEmail(doc.getString("email"));
        t.setPhone(doc.getString("phone"));
        t.setSpecialization(doc.getString("specialization"));

        // ── Status field ──
        String status = doc.getString("status");
        t.setStatus(status != null ? status : "ACTIVE");

        // ── City: read from multiple possible fields ──
        String city = doc.getString("city");
        if (city == null) city = doc.getString("address");
        t.setCity(city);
        t.setStreet(doc.getString("street"));

        // ── Join Date: from DB field, caller uses users.created_at as fallback ──
        Date joinDate = doc.getDate("joinDate");
        if (joinDate == null) joinDate = doc.getDate("join_date");
        if (joinDate != null) {
            t.setJoinDate(new java.text.SimpleDateFormat("dd-MM-yyyy").format(joinDate));
        } else {
            t.setJoinDate("-");
        }

        // ── Qualifications list ──
        List<String> quals = doc.getList("qualifications", String.class);
        if (quals == null) {
            String qualification = doc.getString("qualification");
            if (qualification != null && !qualification.trim().isEmpty()) {
                quals = new ArrayList<>();
                quals.add(qualification);
            }
        }
        if (quals != null) t.setQualifications(quals);

        Object expObj = doc.get("experience_years");
        if (expObj instanceof Number) {
            t.setExperience(((Number) expObj).intValue());
        } else {
            t.setExperience(0);
        }

        String highestDegree = doc.getString("highest_degree");
        if (highestDegree == null) highestDegree = doc.getString("qualification");
        t.setHighestDegree(highestDegree);

        // ── Salary: prefer flat number field (from bulkWrite), fallback to nested doc ──
        Object flatSalObj = doc.get("salary");
        if (flatSalObj instanceof Number) {
            // Flat salary field (e.g. salary: 50000)
            t.setSalary(((Number) flatSalObj).doubleValue());
        } else if (flatSalObj instanceof Document) {
            // Legacy nested salary doc
            Document salDoc = (Document) flatSalObj;
            Teacher.Salary salary = new Teacher.Salary();
            Object baseSalObj = salDoc.get("base_salary");
            if (baseSalObj instanceof Number) {
                salary.setBaseSalary(((Number) baseSalObj).doubleValue());
                t.setSalary(((Number) baseSalObj).doubleValue()); // mirror to flat
            }
            Object workingDaysObj = salDoc.get("working_days");
            if (workingDaysObj instanceof Number) {
                salary.setWorkingDays(((Number) workingDaysObj).intValue());
            }
            t.setLegacySalary(salary);
        }

        return t;
    }

    public static Document teacherToDocument(Teacher teacher) {
        Document doc = new Document();
        if (teacher.getUserId() != null) doc.append("_id", teacher.getUserId());
        if (teacher.getAuthUserId() != null) doc.append("user_id", teacher.getAuthUserId());

        doc.append("full_name",      teacher.getName());
        doc.append("specialization", teacher.getSpecialization());
        doc.append("phone",          teacher.getPhone() != null ? teacher.getPhone() : "9999999999");
        doc.append("status",         teacher.getStatus() != null ? teacher.getStatus() : "ACTIVE");

        // Persist city, street, join_date to DB
        if (teacher.getCity()     != null) doc.append("city",      teacher.getCity());
        if (teacher.getStreet()   != null) doc.append("street",    teacher.getStreet());
        if (teacher.getJoinDate() != null) doc.append("join_date", teacher.getJoinDate());

        // ── NEW flat fields ──
        doc.append("experience_years", teacher.getExperience());
        if (teacher.getHighestDegree() != null) doc.append("highest_degree", teacher.getHighestDegree());
        // Store salary as flat number (compatible with bulkWrite schema)
        if (teacher.getSalary() > 0) {
            doc.append("salary", teacher.getSalary());
        } else if (teacher.getLegacySalary() != null) {
            // Fallback: persist legacy nested salary
            Document salDoc = new Document();
            salDoc.append("base_salary",  teacher.getLegacySalary().getBaseSalary());
            salDoc.append("working_days", teacher.getLegacySalary().getWorkingDays());
            doc.append("salary", salDoc);
        }

        if (teacher.getQualifications() != null) {
            doc.append("qualifications", teacher.getQualifications());
        }

        return doc;
    }

    // ====================================
    // SUBJECT MAPPER
    // ====================================
    public static model.Subject documentToSubject(Document doc) {
        if (doc == null) return null;
        model.Subject s = new model.Subject();
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) {
            s.setSubjectId(((Number) idObj).intValue());
        } else if (idObj != null) {
            String str = idObj.toString().replaceAll("\\D+", "");
            if (!str.isEmpty()) s.setSubjectId(Integer.parseInt(str));
        }
        
        String subjectName = doc.getString("subject_name");
        if (subjectName == null) subjectName = doc.getString("name");
        s.setSubjectName(subjectName);
        s.setCategory(doc.getString("category"));
        Object feeObj = doc.get("monthly_fee");
        if (feeObj == null) feeObj = doc.get("fee");
        
        if (feeObj instanceof Number) {
            s.setMonthlyFee(((Number) feeObj).doubleValue());
        } else if (feeObj != null) {
            try { 
                s.setMonthlyFee(Double.parseDouble(feeObj.toString())); 
            } catch (Exception ignored) {
                // Try extracting from 'fees' object if monthly_fee is not a plain number
                Document fees = (Document) doc.get("fees");
                if (fees != null && !fees.isEmpty()) {
                    for (String key : fees.keySet()) {
                        if (fees.get(key) instanceof Number) {
                            s.setMonthlyFee(((Number) fees.get(key)).doubleValue());
                            break;
                        }
                    }
                }
            }
        } else {
            // feeObj is null, try fees object directly
            Document fees = (Document) doc.get("fees");
            if (fees != null && !fees.isEmpty()) {
                for (String key : fees.keySet()) {
                    if (fees.get(key) instanceof Number) {
                        s.setMonthlyFee(((Number) fees.get(key)).doubleValue());
                        break;
                    }
                }
            }
        }
        s.setStatus(doc.getString("status"));
        s.setSyllabusVersion(doc.getString("syllabus_version"));
        
        // chapters list
        List<Document> chaptersDoc = doc.getList("chapters", Document.class);
        if (chaptersDoc != null) {
            List<model.Subject.Chapter> chapters = new ArrayList<>();
            for (Document cDoc : chaptersDoc) {
                model.Subject.Chapter c = new model.Subject.Chapter();
                Object cId = cDoc.get("chapter_id");
                if (cId instanceof Number) c.setChapterId(((Number)cId).intValue());
                c.setName(cDoc.getString("name"));
                c.setDifficulty(cDoc.getString("difficulty"));
                chapters.add(c);
            }
            s.setChapters(chapters);
        }
        return s;
    }

    public static Document subjectToDocument(model.Subject subject) {
        Document doc = new Document();
        doc.append("_id", subject.getSubjectId());
        doc.append("subject_name", subject.getSubjectName());
        doc.append("category", subject.getCategory());
        doc.append("monthly_fee", subject.getMonthlyFee());
        doc.append("status", subject.getStatus());
        doc.append("syllabus_version", subject.getSyllabusVersion());
        
        if (subject.getChapters() != null) {
            List<Document> cDocs = new ArrayList<>();
            for (model.Subject.Chapter c : subject.getChapters()) {
                Document cDoc = new Document();
                cDoc.append("chapter_id", c.getChapterId());
                cDoc.append("name", c.getName());
                cDoc.append("difficulty", c.getDifficulty());
                cDocs.add(cDoc);
            }
            doc.append("chapters", cDocs);
        }
        return doc;
    }

    // ====================================
    // BATCH MAPPER
    // ====================================
    public static model.Batch documentToBatch(Document doc) {
        if (doc == null) return null;
        model.Batch b = new model.Batch();

        Object idObj = doc.get("_id");
        if (idObj instanceof Number) {
            b.setBatchId(((Number) idObj).intValue());
        } else if (idObj != null) {
            String str = idObj.toString().replaceAll("\\D+", "");
            if (!str.isEmpty()) b.setBatchId(Integer.parseInt(str));
        }

        Object subjObj = doc.get("subject_id");
        if (subjObj instanceof Number) {
            b.setSubjectId(((Number) subjObj).intValue());
        } else if (subjObj != null) {
            String str = subjObj.toString().replaceAll("\\D+", "");
            if (!str.isEmpty()) b.setSubjectId(Integer.parseInt(str));
        }

        b.setTeacherUserId(doc.getString("teacher_id"));
        b.setBatchName(doc.getString("batch_name"));

        // ── timing: try dedicated 'timing' string first ──
        String timing = doc.getString("timing");
        b.setTiming(timing);

        // ── try Date objects ──
        b.setStartTime(doc.getDate("start_time"));
        b.setEndTime(doc.getDate("end_time"));

        // ── try schedule ARRAY (new requirement) ──
        Object scheduleObj = doc.get("schedule");
        if (scheduleObj instanceof java.util.List) {
            java.util.List<?> scheduleList = (java.util.List<?>) scheduleObj;
            java.util.List<model.Schedule> schedules = new java.util.ArrayList<>();
            for (Object item : scheduleList) {
                String day = null;
                String start = null;
                String end = null;

                if (item instanceof Document) {
                    Document sDoc = (Document) item;
                    day = normalizeDay(sDoc.getString("day"));
                    start = sDoc.getString("start");
                    end = sDoc.getString("end");
                    if (start == null) start = sDoc.getString("start_time");
                    if (end == null) end = sDoc.getString("end_time");
                    if (start == null) start = sDoc.getString("startTime");
                    if (end == null) end = sDoc.getString("endTime");
                } else if (item instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) item;
                    Object dayObj = map.get("day");
                    Object startObj = map.get("start");
                    Object endObj = map.get("end");
                    if (startObj == null) startObj = map.get("start_time");
                    if (endObj == null) endObj = map.get("end_time");
                    day = normalizeDay(dayObj != null ? dayObj.toString() : null);
                    start = startObj != null ? startObj.toString() : null;
                    end = endObj != null ? endObj.toString() : null;
                } else if (item instanceof java.util.List) {
                    java.util.List<?> inner = (java.util.List<?>) item;
                    if (inner.size() >= 3) {
                        day = normalizeDay(inner.get(0) != null ? inner.get(0).toString() : null);
                        start = inner.get(1) != null ? inner.get(1).toString() : null;
                        end = inner.get(2) != null ? inner.get(2).toString() : null;
                    }
                }

                if (day != null || start != null || end != null) {
                    model.Schedule sc = new model.Schedule();
                    sc.setDay(day);
                    sc.setStart(start);
                    sc.setEnd(end);
                    if (item instanceof Document) {
                        Document sDoc = (Document) item;
                        String tsId = sDoc.getString("timeslot_id");
                        if (tsId == null) tsId = sDoc.getString("timeslotId");
                        sc.setTimeslotId(tsId);
                    }
                    schedules.add(sc);
                }
            }
            b.setSchedules(schedules);

            java.util.List<model.ScheduleEntry> entries = new java.util.ArrayList<>();
            for (model.Schedule sc : schedules) {
                if (sc.getDay() != null && sc.getTimeslotId() != null) {
                    entries.add(new model.ScheduleEntry(sc.getDay(), sc.getTimeslotId()));
                }
            }
            if (!entries.isEmpty()) {
                b.setScheduleEntries(entries);
                b.setTimeslotId(entries.get(0).getTimeslotId());
            }
            
            // If timing is null, derive it from the first schedule entry
            if (b.getTiming() == null && !schedules.isEmpty()) {
                model.Schedule s = schedules.get(0);
                b.setTiming(s.getDay() + " " + s.getStart() + " - " + s.getEnd());
            }
        } else if (scheduleObj instanceof Document) {
            // Legacy single document support
            Document schedule = (Document) scheduleObj;
            String sStr = schedule.getString("start_time");
            if (sStr == null) sStr = schedule.getString("start");
            String eStr = schedule.getString("end_time");
            if (eStr == null) eStr = schedule.getString("end");
            String day = normalizeDay(schedule.getString("day"));
            String tsId = schedule.getString("timeslot_id");
            if (tsId == null) tsId = schedule.getString("timeslotId");
            
            if (b.getTiming() == null && sStr != null && eStr != null) {
                b.setTiming((day != null ? day + " " : "") + sStr + " - " + eStr);
            }
            
            // Still populate the list for consistency
            java.util.List<model.Schedule> schedules = new java.util.ArrayList<>();
            model.Schedule sc = new model.Schedule(day, sStr, eStr);
            sc.setTimeslotId(tsId);
            schedules.add(sc);
            b.setSchedules(schedules);
            if (day != null && tsId != null) {
                java.util.List<model.ScheduleEntry> entries = new java.util.ArrayList<>();
                entries.add(new model.ScheduleEntry(day, tsId));
                b.setScheduleEntries(entries);
                b.setTimeslotId(tsId);
            }
        }

        // ── If timing still null, derive from Date start/end ──
        if (b.getTiming() == null && b.getStartTime() != null && b.getEndTime() != null) {
            java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm");
            b.setTiming(tf.format(b.getStartTime()) + " - " + tf.format(b.getEndTime()));
        }

        b.setMeetingLink(doc.getString("meeting_link"));
        b.setClassMode(doc.getString("class_mode"));
        b.setCategory(doc.getString("category"));
        b.setStatus(doc.getString("status"));
        if (b.getStatus() == null) b.setStatus("ACTIVE"); // Default

        // Read dedicated 'standard' field; derive from category as fallback
        String std = doc.getString("standard");
        if (std == null && b.getCategory() != null) {
            String cat = b.getCategory().trim();
            if (cat.toLowerCase().startsWith("class ")) {
                std = cat.substring(6).trim(); // "Class 12" → "12"
            } else {
                std = cat; // Already a plain number
            }
        }
        b.setStandard(std);
        return b;
    }

    private static String normalizeDay(String day) {
        if (day == null) return null;
        String value = day.trim().toUpperCase();
        if (value.startsWith("MON")) return "MON";
        if (value.startsWith("TUE")) return "TUE";
        if (value.startsWith("WED")) return "WED";
        if (value.startsWith("THU")) return "THU";
        if (value.startsWith("FRI")) return "FRI";
        if (value.startsWith("SAT")) return "SAT";
        if (value.startsWith("SUN")) return "SUN";
        return value;
    }

    public static Document batchToDocument(model.Batch batch) {
        Document doc = new Document();
        doc.append("_id",         batch.getBatchId());
        doc.append("subject_id",  batch.getSubjectId());
        doc.append("teacher_id",  batch.getTeacherUserId());
        doc.append("batch_name",  batch.getBatchName());
        doc.append("start_time",  batch.getStartTime());
        doc.append("end_time",    batch.getEndTime());
        doc.append("meeting_link",batch.getMeetingLink());
        doc.append("class_mode",  batch.getClassMode());
        doc.append("category",    batch.getCategory());
        doc.append("standard",    batch.getStandard());
        doc.append("status",      batch.getStatus() != null ? batch.getStatus() : "ACTIVE");
        
        // Persist schedule list if present. Prefer scheduleEntries because the admin
        // timetable groups by day + timeslot_id.
        if (batch.getScheduleEntries() != null && !batch.getScheduleEntries().isEmpty()) {
            java.util.List<Document> sDocs = new java.util.ArrayList<>();
            for (model.ScheduleEntry e : batch.getScheduleEntries()) {
                Document sDoc = new Document();
                sDoc.append("day", e.getDay());
                sDoc.append("timeslot_id", e.getTimeslotId());
                sDoc.append("timeslotId", e.getTimeslotId());
                sDocs.add(sDoc);
            }
            doc.append("schedule", sDocs);
        } else if (batch.getSchedules() != null && !batch.getSchedules().isEmpty()) {
            java.util.List<Document> sDocs = new java.util.ArrayList<>();
            for (model.Schedule s : batch.getSchedules()) {
                Document sDoc = new Document();
                sDoc.append("day", s.getDay());
                sDoc.append("start", s.getStart());
                sDoc.append("end", s.getEnd());
                if (s.getTimeslotId() != null) {
                    sDoc.append("timeslot_id", s.getTimeslotId());
                    sDoc.append("timeslotId", s.getTimeslotId());
                }
                sDocs.add(sDoc);
            }
            doc.append("schedule", sDocs);
        }

        // Persist timing string to DB for direct retrieval
        if (batch.getTiming() != null) {
            doc.append("timing", batch.getTiming());
        } else if (batch.getStartTime() != null && batch.getEndTime() != null) {
            java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm");
            doc.append("timing", tf.format(batch.getStartTime()) + " - " + tf.format(batch.getEndTime()));
        }
        return doc;
    }

    // ====================================
    // ATTENDANCE MAPPER
    // ====================================
    public static model.Attendance documentToAttendance(Document doc) {
        if (doc == null) return null;
        model.Attendance a = new model.Attendance();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) a.setAttendanceId(((Number) idObj).intValue());
        else if (idObj != null) try { a.setAttendanceId(Integer.parseInt(idObj.toString())); } catch (Exception e) {}
        
        a.setUserId(doc.getString("user_id"));
        a.setStatus(doc.getString("status"));
        a.setReason(doc.getString("reason"));
        a.setMarkedBy(doc.getString("marked_by"));
        a.setAttendanceDate(doc.getDate("attendance_date"));
        
        // Handle alternative date field
        if (a.getAttendanceDate() == null) {
            a.setAttendanceDate(doc.getDate("date"));
        }
        
        return a;
    }
    public static Document attendanceToDocument(model.Attendance att) {
        Document doc = new Document();
        doc.append("_id", att.getAttendanceId());
        doc.append("user_id", att.getUserId());
        doc.append("status", att.getStatus());
        doc.append("reason", att.getReason());
        doc.append("marked_by", att.getMarkedBy());
        doc.append("attendance_date", att.getAttendanceDate());
        return doc;
    }

    public static model.Enrollment documentToEnrollment(Document doc) {
        if (doc == null) return null;
        model.Enrollment e = new model.Enrollment();
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) e.setEnrollmentId(((Number) idObj).intValue());
        else if (idObj != null) {
            try { e.setEnrollmentId(Integer.parseInt(idObj.toString().replaceAll("\\D", ""))); } catch (Exception ex) {}
        }
        e.setStudentUserId(doc.getString("student_user_id"));
        if (e.getStudentUserId() == null) e.setStudentUserId(doc.getString("student_id"));
        if (e.getStudentUserId() == null) e.setStudentUserId(doc.getString("user_id"));
        if (e.getStudentUserId() == null) {
            Object rawId = doc.get("student_user_id");
            if (rawId == null) rawId = doc.get("student_id");
            if (rawId == null) rawId = doc.get("user_id");
            if (rawId instanceof Number) e.setStudentUserId(rawId.toString());
        }
        Object batchObj = doc.get("batch_id");
        if (batchObj instanceof Number) e.setBatchId(((Number) batchObj).intValue());
        else if (batchObj != null) {
            try { e.setBatchId(Integer.parseInt(batchObj.toString().replaceAll("\\D", ""))); } catch (Exception ex) {}
        }
        e.setStatus(doc.getString("status"));
        e.setRemarks(doc.getString("remarks"));
        e.setEnrollmentDate(doc.getDate("enrollment_date"));
        if (e.getEnrollmentDate() == null) e.setEnrollmentDate(doc.getDate("date"));
        return e;
    }

    public static Document enrollmentToDocument(model.Enrollment e) {
        Document doc = new Document();
        doc.append("_id", e.getEnrollmentId());
        doc.append("student_user_id", e.getStudentUserId());
        doc.append("batch_id", e.getBatchId());
        doc.append("status", e.getStatus());
        doc.append("remarks", e.getRemarks());
        doc.append("enrollment_date", e.getEnrollmentDate());
        return doc;
    }

    // ====================================
    // FEE MAPPER
    // ====================================
    public static model.Fee documentToFee(Document doc) {
        if (doc == null) return null;
        model.Fee f = new model.Fee();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) f.setFeeId(((Number) idObj).intValue());
        else if (idObj != null) try { f.setFeeId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        f.setUserId(doc.getString("user_id"));
        
        Object totalObj = doc.get("total_amount");
        if (totalObj instanceof Number) f.setTotalAmount(((Number) totalObj).doubleValue());
        
        Object paidObj = doc.get("paid_amount");
        if (paidObj instanceof Number) f.setPaidAmount(((Number) paidObj).doubleValue());
        
        f.setDueDate(doc.getDate("due_date"));
        f.setStatus(doc.getString("status"));
        return f;
    }

    public static Document feeToDocument(model.Fee fee) {
        Document doc = new Document();
        doc.append("_id", fee.getFeeId());
        doc.append("user_id", fee.getUserId());
        doc.append("total_amount", fee.getTotalAmount());
        doc.append("paid_amount", fee.getPaidAmount());
        doc.append("due_date", fee.getDueDate());
        doc.append("status", fee.getStatus());
        return doc;
    }

    // ====================================
    // PAYMENT MAPPER (NEW SCHEMA)
    // ====================================
    /**
     * Convert Document to Payment object
     * Supports new schema: student_id, subject_id, amount_paid, payment_mode, payment_date, month
     * Also supports legacy schema: fee_id, receipt_no
     */
    public static model.Payment documentToPayment(Document doc) {
        if (doc == null) return null;
        model.Payment p = new model.Payment();

        // ID
        Object id = doc.get("_id");
        if (id != null) p.setPaymentId(id.hashCode());

        // Core fields — use "amount" (new schema), fallback to "amount_paid" (legacy)
        Object amt = doc.get("amount");
        if (amt == null) amt = doc.get("amount_paid");
        if (amt instanceof Number) p.setAmountPaid(((Number) amt).doubleValue());

        // Method — use "method" (new), fallback to "payment_mode" (legacy)
        String method = doc.getString("method");
        if (method == null) method = doc.getString("payment_mode");
        p.setPaymentMode(method);

        // Status
        String status = doc.getString("status");
        if ("PAID".equalsIgnoreCase(status)) {
            status = "SUCCESS";
        }
        p.setStatus(status);

        // Date — use "date" (new), fallback to "payment_date" (legacy)
        Object dateObj = doc.get("date");
        if (dateObj == null) dateObj = doc.get("payment_date");
        if (dateObj instanceof java.util.Date) p.setPaymentDate((java.util.Date) dateObj);

        // Batch and Student IDs
        String sId = doc.getString("student_id");
        if (sId == null) sId = doc.getString("studentId");
        p.setStudentId(sId);

        // Subject ID is stored as String in new schema
        Object subjectObj = doc.get("subject_id");
        if (subjectObj instanceof Number) p.setSubjectId(String.valueOf(((Number) subjectObj).intValue()));
        else if (subjectObj != null) p.setSubjectId(subjectObj.toString());

        // Legacy fields for backward compatibility
        Object feeObj = doc.get("fee_id");
        if (feeObj instanceof Number) p.setFeeId(((Number) feeObj).intValue());
        else if (feeObj != null) try { p.setFeeId(Integer.parseInt(feeObj.toString())); } catch (Exception ex) {}

        Object batchId = doc.get("batch_id");
        if (batchId == null) batchId = doc.get("batchId");
        if (batchId instanceof Number) p.setBatchId(((Number) batchId).intValue());
        else if (batchId instanceof String) {
            String bStr = (String) batchId;
            if (bStr.toLowerCase().startsWith("batch")) {
                try {
                    p.setBatchId(Integer.parseInt(bStr.replaceAll("\\D", "")));
                } catch (Exception e) {}
            }
        }

        // Month
        p.setMonthStr(doc.getString("month"));

        p.setReceiptNo(doc.getString("receipt_no"));

        return p;
    }

    /**
     * Convert Payment object to Document
     * New schema: student_id, subject_id, amount_paid, payment_mode, payment_date, month
     */
    public static Document paymentToDocument(model.Payment p) {
        Document doc = new Document();
        doc.append("_id", p.getPaymentId());
        doc.append("student_id", p.getStudentId());
        if (p.getFeeId() > 0) doc.append("fee_id", p.getFeeId());
        if (p.getSubjectId() != null) doc.append("subject_id", p.getSubjectId());
        if (p.getBatchId() > 0) doc.append("batch_id", p.getBatchId());
        doc.append("month", p.getMonthStr());
        doc.append("status", p.getStatus() != null ? p.getStatus() : "SUCCESS");
        // Standard schema
        doc.append("amount", p.getAmountPaid());
        doc.append("date", p.getPaymentDate());
        doc.append("method", p.getPaymentMode());
        // Legacy aliases for older screens and records.
        doc.append("amount_paid", p.getAmountPaid());
        doc.append("payment_date", p.getPaymentDate());
        doc.append("payment_mode", p.getPaymentMode());
        if (p.getReceiptNo() != null) doc.append("receipt_no", p.getReceiptNo());
        return doc;
    }

    // ====================================
    // TEST MAPPER
    // ====================================
    public static model.Test documentToTest(Document doc) {
        if (doc == null) return null;
        model.Test t = new model.Test();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) t.setTestId(((Number) idObj).intValue());
        else if (idObj != null) try { t.setTestId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        Object batchObj = doc.get("batch_id");
        if (batchObj instanceof Number) t.setBatchId(((Number) batchObj).intValue());
        else if (batchObj != null) try { t.setBatchId(Integer.parseInt(batchObj.toString())); } catch (Exception ex) {}
        
        t.setTestName(doc.getString("test_name"));
        t.setTestDate(doc.getDate("test_date"));
        if (t.getTestDate() == null) t.setTestDate(doc.getDate("date"));
        
        Object maxObj = doc.get("total_marks"); // Use total_marks as per new requirement
        if (maxObj == null) maxObj = doc.get("max_marks");
        if (maxObj instanceof Number) t.setMaxMarks(((Number) maxObj).intValue());
        else if (maxObj != null) try { t.setMaxMarks(Integer.parseInt(maxObj.toString())); } catch (Exception ex) {}
        
        // Handle attempts (raw documents for processing in service)
        t.setAttempts(doc.getList("attempts", Document.class));
        
        return t;
    }

    public static Document testToDocument(model.Test test) {
        Document doc = new Document();
        doc.append("_id", test.getTestId());
        doc.append("batch_id", test.getBatchId());
        doc.append("test_name", test.getTestName());
        doc.append("test_date", test.getTestDate());
        doc.append("max_marks", test.getMaxMarks());
        doc.append("total_marks", test.getMaxMarks());
        doc.append("attempts", test.getAttempts() != null ? test.getAttempts() : new ArrayList<Document>());
        return doc;
    }

    // ====================================
    // MARK MAPPER
    // ====================================
    public static model.Mark documentToMark(Document doc) {
        if (doc == null) return null;
        model.Mark m = new model.Mark();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) m.setMarkId(((Number) idObj).intValue());
        else if (idObj != null) try { m.setMarkId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        Object testObj = doc.get("test_id");
        if (testObj instanceof Number) m.setTestId(((Number) testObj).intValue());
        else if (testObj != null) try { m.setTestId(Integer.parseInt(testObj.toString())); } catch (Exception ex) {}
        
        m.setUserId(doc.getString("user_id"));
        
        Object marksObj = doc.get("marks_obtained");
        if (marksObj instanceof Number) m.setMarksObtained(((Number) marksObj).intValue());
        else if (marksObj != null) try { m.setMarksObtained(Integer.parseInt(marksObj.toString())); } catch (Exception ex) {}
        
        m.setRemarks(doc.getString("remarks"));
        
        return m;
    }

    public static Document markToDocument(model.Mark mark) {
        Document doc = new Document();
        doc.append("_id", mark.getMarkId());
        doc.append("test_id", mark.getTestId());
        doc.append("user_id", mark.getUserId());
        doc.append("marks_obtained", mark.getMarksObtained());
        doc.append("remarks", mark.getRemarks());
        return doc;
    }

    // ====================================
    // CHAPTER PROGRESS MAPPER
    // ====================================
    public static model.ChapterProgress documentToChapterProgress(Document doc) {
        if (doc == null) return null;
        model.ChapterProgress cp = new model.ChapterProgress();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) cp.setProgressId(((Number) idObj).intValue());
        else if (idObj != null) try { cp.setProgressId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        Object batchObj = doc.get("batch_id");
        if (batchObj instanceof Number) cp.setBatchId(((Number) batchObj).intValue());
        else if (batchObj != null) try { cp.setBatchId(Integer.parseInt(batchObj.toString())); } catch (Exception ex) {}
        
        Object chapterObj = doc.get("chapter_id");
        if (chapterObj instanceof Number) cp.setChapterId(((Number) chapterObj).intValue());
        else if (chapterObj != null) try { cp.setChapterId(Integer.parseInt(chapterObj.toString())); } catch (Exception ex) {}
        
        cp.setStatus(doc.getString("status"));
        cp.setRemarks(doc.getString("remarks"));
        
        Object percentObj = doc.get("completion_percentage");
        if (percentObj instanceof Number) cp.setCompletionPercentage(((Number) percentObj).intValue());
        else if (percentObj != null) try { cp.setCompletionPercentage(Integer.parseInt(percentObj.toString())); } catch (Exception ex) {}
        
        cp.setLastUpdated(doc.getDate("last_updated"));
        if (cp.getLastUpdated() == null) cp.setLastUpdated(doc.getDate("date"));
        
        return cp;
    }

    public static Document chapterProgressToDocument(model.ChapterProgress cp) {
        Document doc = new Document();
        doc.append("_id", cp.getProgressId());
        doc.append("batch_id", cp.getBatchId());
        doc.append("chapter_id", cp.getChapterId());
        doc.append("status", cp.getStatus());
        doc.append("remarks", cp.getRemarks());
        doc.append("completion_percentage", cp.getCompletionPercentage());
        doc.append("last_updated", cp.getLastUpdated());
        return doc;
    }

    // ====================================
    // PARENT MAPPER
    // ====================================
    public static model.Parent documentToParent(Document doc) {
        if (doc == null) return null;
        model.Parent p = new model.Parent();
        
        // ✅ Strictly use user_id (matches users._id)
        p.setUserId(doc.getString("user_id"));
        if (p.getUserId() == null) p.setUserId(doc.getString("parent_id"));
        if (p.getUserId() == null) p.setUserId(doc.getString("_id"));
        
        String parentName = doc.getString("name");
        if (parentName == null) parentName = doc.getString("full_name");
        p.setName(parentName);
        p.setEmail(doc.getString("email"));
        p.setPhone(doc.getString("phone"));
        
        p.setPreferredLanguage(doc.getString("preferred_language"));
        p.setOccupation(doc.getString("occupation"));
        
        Object incomeObj = doc.get("annual_income");
        if (incomeObj instanceof Number) p.setAnnualIncome(((Number) incomeObj).doubleValue());
        else if (incomeObj != null) try { p.setAnnualIncome(Double.parseDouble(incomeObj.toString())); } catch (Exception ex) {}
        if (p.getAnnualIncome() == 0) {
            Object salaryObj = doc.get("salary");
            if (salaryObj instanceof Number) p.setAnnualIncome(((Number) salaryObj).doubleValue() * 12);
            else if (salaryObj != null) {
                try { p.setAnnualIncome(Double.parseDouble(salaryObj.toString()) * 12); } catch (Exception ex) {}
            }
        }
        
        Object emergencyObj = doc.get("emergency_contact");
        if (emergencyObj instanceof Number) p.setEmergencyContact(((Number) emergencyObj).longValue());
        else if (emergencyObj != null) try { p.setEmergencyContact(Long.parseLong(emergencyObj.toString())); } catch (Exception ex) {}
        
        String relation = doc.getString("relation_type");
        if (relation == null) relation = doc.getString("relation");
        p.setRelationType(relation);
        
        return p;
    }

    public static Document parentToDocument(model.Parent p) {
        Document doc = new Document();
        // ✅ Strictly use user_id
        doc.append("user_id", p.getUserId());
        doc.append("name", p.getName());
        doc.append("email", p.getEmail());
        doc.append("phone", p.getPhone());
        
        doc.append("preferred_language", p.getPreferredLanguage());
        doc.append("occupation", p.getOccupation());
        doc.append("annual_income", p.getAnnualIncome());
        doc.append("emergency_contact", p.getEmergencyContact());
        doc.append("relation_type", p.getRelationType());
        
        return doc;
    }

    private static long parsePhoneToLong(String phone) {
        try {
            if (phone != null) return Long.parseLong(phone);
        } catch(Exception e) {}
        return 0;
    }
}
