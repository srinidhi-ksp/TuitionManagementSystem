package service;

import dao.TeacherDAO;
import dao.UserDAO;
import model.Teacher;
import java.util.List;

public class TeacherService {

    private UserDAO userDAO;
    private TeacherDAO teacherDAO;

    public TeacherService() {
        userDAO = new UserDAO();
        teacherDAO = new TeacherDAO();
    }

    public boolean registerTeacher(Teacher teacher) {

        boolean userCreated = userDAO.addUser(teacher);

        if (userCreated) {
            return teacherDAO.addTeacher(teacher);
        }

        return false;
    }

    public Teacher getTeacher(String userId) {
        return teacherDAO.getTeacherById(userId);
    }

    public boolean deleteTeacher(String userId) {

        boolean teacherDeleted = teacherDAO.deleteTeacher(userId);

        if (teacherDeleted) {
            return userDAO.deleteUser(userId);
        }

        return false;
    }

    public List<Teacher> getAllTeachers() {
        return teacherDAO.getAllTeachers();
    }
}