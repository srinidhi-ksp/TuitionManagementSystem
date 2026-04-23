package db;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import model.Parent;
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
        String userId = doc.getString("_id");
        user.setUserId(userId);
        System.out.println("[DocumentMapper] Mapping user: " + userId);
        
        user.setEmail(doc.getString("email"));
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
        s.setUserId(doc.getString("_id")); // _id in Student matches S001 etc
        s.setName(doc.getString("full_name"));
        s.setDob(doc.getDate("dob"));
        s.setEmail(doc.getString("email"));
        s.setPhone(doc.getString("phone"));

        // join_date from DB (null if not stored — caller will use users.created_at)
        s.setJoinDate(doc.getDate("join_date"));

        // ✅ DB field is 'standard' (not 'current_std')
        String std = doc.getString("standard");
        if (std == null) std = doc.getString("current_std"); // backward compat
        s.setCurrentStd(std);
        s.setBoard(doc.getString("board"));

        // city from dedicated field, fallback to address
        String city = doc.getString("city");
        if (city == null) city = doc.getString("address");
        s.setCity(city);
        s.setStreet(doc.getString("street"));

        // ── Nested Parent: build both the Parent object AND the flat fields ──
        Document pDoc = (Document) doc.get("parent");
        if (pDoc != null) {
            // Full Parent object (kept for backward compat)
            Parent parent = new Parent();
            parent.setUserId(pDoc.getString("parent_id"));
            parent.setName(pDoc.getString("full_name"));
            parent.setEmergencyContact(parsePhoneToLong(pDoc.getString("phone")));
            parent.setOccupation(pDoc.getString("occupation"));
            s.setParent(parent);

            // ✅ Flat parent fields (SINGLE SOURCE OF TRUTH)
            s.setParentId(pDoc.getString("parent_id"));
            s.setParentName(pDoc.getString("full_name"));
            s.setParentPhone(pDoc.getString("phone"));
            s.setParentOccupation(pDoc.getString("occupation"));
            // relation is not stored in embedded doc — default to "Father"
            s.setParentRelation("Father");
        }

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

        if (student.getParent() != null) {
            Parent p = student.getParent();
            Document pDoc = new Document();
            pDoc.append("parent_id",  p.getUserId());
            pDoc.append("full_name",  p.getName());
            pDoc.append("phone",      String.valueOf(p.getEmergencyContact()));
            pDoc.append("occupation", p.getOccupation());
            doc.append("parent", pDoc);
        }

        return doc;
    }
    
    // ====================================
    // TEACHER MAPPER
    // ====================================
    public static Teacher documentToTeacher(Document doc) {
        if (doc == null) return null;
        Teacher t = new Teacher();
        t.setUserId(doc.getString("_id")); // Maps like T001
        t.setName(doc.getString("full_name"));
        // ✅ CRITICAL: read email so getCreatedAtByEmail() can look up users.created_at
        t.setEmail(doc.getString("email"));
        t.setPhone(doc.getString("phone"));
        t.setSpecialization(doc.getString("specialization"));
        t.setRole(doc.getString("status")); // Usually status ACTIVE, or role

        // ── City: read from multiple possible fields ──
        String city = doc.getString("city");
        if (city == null) city = doc.getString("address");
        t.setCity(city);
        t.setStreet(doc.getString("street"));

        // ── Join Date: from DB field, caller uses users.created_at as fallback ──
        t.setJoinDate(doc.getDate("join_date"));

        // ── Qualifications list ──
        List<String> quals = doc.getList("qualifications", String.class);
        if (quals != null) t.setQualifications(quals);

        // ── Salary nested doc ──
        Document salDoc = (Document) doc.get("salary");
        if (salDoc != null) {
            Teacher.Salary salary = new Teacher.Salary();
            Object baseSalObj = salDoc.get("base_salary");
            if (baseSalObj instanceof Number) {
                salary.setBaseSalary(((Number) baseSalObj).doubleValue());
            }
            Object workingDaysObj = salDoc.get("working_days");
            if (workingDaysObj instanceof Number) {
                salary.setWorkingDays(((Number) workingDaysObj).intValue());
            }
            t.setSalary(salary);
        }

        return t;
    }

    public static Document teacherToDocument(Teacher teacher) {
        Document doc = new Document();
        if (teacher.getUserId() != null) doc.append("_id", teacher.getUserId());

        doc.append("full_name",      teacher.getName());
        doc.append("specialization", teacher.getSpecialization());
        doc.append("phone",          teacher.getPhone() != null ? teacher.getPhone() : "9999999999");
        doc.append("status",         "ACTIVE");

        // Persist city, street, join_date to DB
        if (teacher.getCity()     != null) doc.append("city",      teacher.getCity());
        if (teacher.getStreet()   != null) doc.append("street",    teacher.getStreet());
        if (teacher.getJoinDate() != null) doc.append("join_date", teacher.getJoinDate());

        if (teacher.getQualifications() != null) {
            doc.append("qualifications", teacher.getQualifications());
        }

        if (teacher.getSalary() != null) {
            Document salDoc = new Document();
            salDoc.append("base_salary",  teacher.getSalary().getBaseSalary());
            salDoc.append("working_days", teacher.getSalary().getWorkingDays());
            doc.append("salary", salDoc);
        }

        return doc;
    }

    // ====================================
    // SUBJECT MAPPER
    // ====================================
    public static model.Subject documentToSubject(Document doc) {
        if (doc == null) return null;
        model.Subject s = new model.Subject();
        // ID could be integer or string, checking the notepad dataset, _id: 1, 2, ...
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) {
            s.setSubjectId(((Number) idObj).intValue());
        } else if (idObj != null) {
            try {
                s.setSubjectId(Integer.parseInt(idObj.toString()));
            } catch (Exception e) {}
        }
        
        s.setSubjectName(doc.getString("subject_name"));
        s.setCategory(doc.getString("category"));
        s.setMonthlyFee(doc.get("monthly_fee") instanceof Number ? ((Number)doc.get("monthly_fee")).doubleValue() : 0.0);
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
        if (idObj instanceof Number) b.setBatchId(((Number) idObj).intValue());
        else if (idObj != null) try { b.setBatchId(Integer.parseInt(idObj.toString())); } catch (Exception e) {}

        Object subjObj = doc.get("subject_id");
        if (subjObj instanceof Number) b.setSubjectId(((Number) subjObj).intValue());
        else if (subjObj != null) try { b.setSubjectId(Integer.parseInt(subjObj.toString())); } catch (Exception e) {}

        b.setTeacherUserId(doc.getString("teacher_id"));
        b.setBatchName(doc.getString("batch_name"));

        // ── timing: try dedicated 'timing' string first ──
        String timing = doc.getString("timing");
        b.setTiming(timing);

        // ── try Date objects ──
        b.setStartTime(doc.getDate("start_time"));
        b.setEndTime(doc.getDate("end_time"));

        // ── try schedule sub-document (some DB records use this) ──
        Document schedule = (Document) doc.get("schedule");
        if (schedule != null) {
            // schedule.start_time / schedule.end_time may be strings like "09:00"
            Object ss = schedule.get("start_time");
            Object se = schedule.get("end_time");
            String sStr = ss != null ? ss.toString() : null;
            String eStr = se != null ? se.toString() : null;
            if (b.getTiming() == null && sStr != null && eStr != null) {
                b.setTiming(sStr + " - " + eStr);
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

    // ====================================
    // ENROLLMENT MAPPER
    // ====================================
    public static model.Enrollment documentToEnrollment(Document doc) {
        if (doc == null) return null;
        model.Enrollment e = new model.Enrollment();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) e.setEnrollmentId(((Number) idObj).intValue());
        else if (idObj != null) try { e.setEnrollmentId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        e.setStudentUserId(doc.getString("student_user_id"));
        // Try all possible field names the DB may use
        if (e.getStudentUserId() == null) e.setStudentUserId(doc.getString("student_id"));
        if (e.getStudentUserId() == null) e.setStudentUserId(doc.getString("user_id"));
        if (e.getStudentUserId() == null) {
            // Last attempt: convert numeric student_id to string
            Object rawId = doc.get("student_user_id");
            if (rawId == null) rawId = doc.get("student_id");
            if (rawId == null) rawId = doc.get("user_id");
            if (rawId instanceof Number) e.setStudentUserId(rawId.toString());
        }
        
        Object batchObj = doc.get("batch_id");
        if (batchObj instanceof Number) e.setBatchId(((Number) batchObj).intValue());
        else if (batchObj != null) try { e.setBatchId(Integer.parseInt(batchObj.toString())); } catch (Exception ex) {}
        
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
    // PAYMENT MAPPER
    // ====================================
    public static model.Payment documentToPayment(Document doc) {
        if (doc == null) return null;
        model.Payment p = new model.Payment();
        
        Object idObj = doc.get("_id");
        if (idObj instanceof Number) p.setPaymentId(((Number) idObj).intValue());
        else if (idObj != null) try { p.setPaymentId(Integer.parseInt(idObj.toString())); } catch (Exception ex) {}
        
        Object feeObj = doc.get("fee_id");
        if (feeObj instanceof Number) p.setFeeId(((Number) feeObj).intValue());
        else if (feeObj != null) try { p.setFeeId(Integer.parseInt(feeObj.toString())); } catch (Exception ex) {}
        
        Object amountObj = doc.get("amount_paid");
        if (amountObj instanceof Number) p.setAmountPaid(((Number) amountObj).doubleValue());
        
        p.setPaymentDate(doc.getDate("payment_date"));
        if (p.getPaymentDate() == null) p.setPaymentDate(doc.getDate("date"));
        
        p.setPaymentMode(doc.getString("payment_mode"));
        p.setReceiptNo(doc.getString("receipt_no"));
        
        return p;
    }

    public static Document paymentToDocument(model.Payment p) {
        Document doc = new Document();
        doc.append("_id", p.getPaymentId());
        doc.append("fee_id", p.getFeeId());
        doc.append("amount_paid", p.getAmountPaid());
        doc.append("payment_date", p.getPaymentDate());
        doc.append("payment_mode", p.getPaymentMode());
        doc.append("receipt_no", p.getReceiptNo());
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
        
        p.setUserId(doc.getString("user_id"));
        if (p.getUserId() == null) p.setUserId(doc.getString("_id"));
        
        p.setPreferredLanguage(doc.getString("preferred_language"));
        p.setOccupation(doc.getString("occupation"));
        
        Object incomeObj = doc.get("annual_income");
        if (incomeObj instanceof Number) p.setAnnualIncome(((Number) incomeObj).doubleValue());
        else if (incomeObj != null) try { p.setAnnualIncome(Double.parseDouble(incomeObj.toString())); } catch (Exception ex) {}
        
        Object emergencyObj = doc.get("emergency_contact");
        if (emergencyObj instanceof Number) p.setEmergencyContact(((Number) emergencyObj).longValue());
        else if (emergencyObj != null) try { p.setEmergencyContact(Long.parseLong(emergencyObj.toString())); } catch (Exception ex) {}
        
        p.setRelationType(doc.getString("relation_type"));
        p.setName(doc.getString("name"));
        
        return p;
    }

    public static Document parentToDocument(model.Parent p) {
        Document doc = new Document();
        doc.append("user_id", p.getUserId());
        doc.append("preferred_language", p.getPreferredLanguage());
        doc.append("occupation", p.getOccupation());
        doc.append("annual_income", p.getAnnualIncome());
        doc.append("emergency_contact", p.getEmergencyContact());
        doc.append("relation_type", p.getRelationType());
        if (p.getName() != null) doc.append("name", p.getName());
        return doc;
    }

    private static long parsePhoneToLong(String phone) {
        try {
            if (phone != null) return Long.parseLong(phone);
        } catch(Exception e) {}
        return 0;
    }
}
