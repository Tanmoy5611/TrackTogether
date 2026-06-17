package TrackTogether.repository;

import TrackTogether.domain.Report;
import TrackTogether.domain.Conversation;
import TrackTogether.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByStatus(ReportStatus status);

    List<Report> findByStatusIn(java.util.Collection<ReportStatus> statuses);

    void deleteAllByMessage_Conversation(Conversation conversation);

    boolean existsByMessage_MessageIdAndReporter_UserId(UUID messageId, UUID reporterId);

    /** Loads the report together with its lazy {@code message} association in one query. */
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.message WHERE r.reportId = :id")
    Optional<Report> findByIdWithMessage(@Param("id") UUID id);

    /*
     Loads the report with its message, the message's conversation, and the message's
     sender - everything needed to build the chat-context window.
     */
    @Query("""
        SELECT r FROM Report r
        LEFT JOIN FETCH r.message m
        LEFT JOIN FETCH m.conversation
        LEFT JOIN FETCH m.sender
        WHERE r.reportId = :id
        """)
    Optional<Report> findByIdWithMessageAndConversation(@Param("id") UUID id);
}