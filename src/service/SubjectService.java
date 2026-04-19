package service;

import dao.SubjectDAO;
import model.Subject;

import java.util.List;

public class SubjectService {

    private SubjectDAO subjectDAO;

    public SubjectService() {
        subjectDAO = new SubjectDAO();
    }

    public boolean addSubject(Subject subject) {
        return subjectDAO.addSubject(subject);
    }

    public Subject getSubject(int subjectId) {
        return subjectDAO.getSubjectById(subjectId);
    }

    public List<Subject> getAllSubjects() {
        return subjectDAO.getAllSubjects();
    }

    public boolean deleteSubject(int subjectId) {
        return subjectDAO.deleteSubject(subjectId);
    }
}