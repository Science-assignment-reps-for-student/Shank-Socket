package kr.hs.dsm_scarfs.shank.socket.entities.message.repository;

import kr.hs.dsm_scarfs.shank.socket.entities.message.Message;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends CrudRepository<Message, Integer> {
    Optional<Message> findFirstByStudentIdAndAdminIdOrderByTimeDesc(Integer studentId, Integer adminId);
    List<Message> findAllByStudentIdAndAdminIdOrderByTimeAsc(Integer studentId, Integer adminId);

}
