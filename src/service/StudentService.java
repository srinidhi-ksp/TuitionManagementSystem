package service;

import dao.StudentDAO;
import dao.UserDAO;
import model.Student;
import java.util.List;

public class StudentService {

    private UserDAO userDAO;
    private StudentDAO studentDAO;

    public StudentService() {
        userDAO = new UserDAO();
        studentDAO = new StudentDAO();
    }

    // Register student
    public boolean registerStudent(Student student) {

        boolean userCreated = userDAO.addUser(student);

        if (userCreated) {
            return studentDAO.addStudent(student);
        }

        return false;
    }

    // Get student
    public Student getStudent(String userId) {
        return studentDAO.getStudentById(userId);
    }

    // Delete student
    public boolean deleteStudent(String userId) {

        boolean studentDeleted = studentDAO.deleteStudent(userId);

        if (studentDeleted) {
            return userDAO.deleteUser(userId);
        }

        return false;
    }

    // Get all students
    public List<Student> getAllStudents() {
        return studentDAO.getAllStudents();
    }
}