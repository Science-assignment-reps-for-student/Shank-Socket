package kr.hs.dsm_scarfs.shank.socket.entities.user;

import kr.hs.dsm_scarfs.shank.socket.entities.user.admin.AdminRepository;
import kr.hs.dsm_scarfs.shank.socket.entities.user.student.Student;
import kr.hs.dsm_scarfs.shank.socket.entities.user.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserFactory {

    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;

    public User getUser(String email) throws Exception {
        Optional<Student> student = studentRepository.findByEmail(email);
        if (student.isPresent()) return student.get();
        else return adminRepository.findByEmail(email)
                .orElseThrow(Exception::new);
    }

    public User getStudent(Integer id) throws Exception {
        return studentRepository.findById(id)
                .orElseThrow(Exception::new);
    }

    public User getAdmin(Integer id) throws Exception {
        return adminRepository.findById(id)
                .orElseThrow(Exception::new);
    }

}
