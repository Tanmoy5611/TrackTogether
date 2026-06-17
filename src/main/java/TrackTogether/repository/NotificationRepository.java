package TrackTogether.repository;

import TrackTogether.domain.Member;
import TrackTogether.domain.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByRecipientOrderByCreatedAtDesc(Member recipient);

    long countByRecipientAndReadFalse(Member recipient);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient = :recipient AND n.read = false")
    void markAllAsReadForRecipient(Member recipient);
}