package scratch;

import dao.StudentDAO;
import model.Student;
import java.util.List;

public class VerifyMapping {
    public static void main(String[] args) {
        StudentDAO dao = new StudentDAO();
        String parentId = "U31"; // Test ID
        List<Student> students = dao.getStudentsByParentUserId(parentId);
        System.out.println("Parent ID: " + parentId);
        System.out.println("Students found: " + students.size());
        for (Student s : students) {
            System.out.println(" - " + s.getName() + " (" + s.getUserId() + ")");
        }
    }
}
