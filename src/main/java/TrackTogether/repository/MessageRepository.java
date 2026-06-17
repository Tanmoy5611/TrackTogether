package TrackTogether.repository;

import TrackTogether.domain.Message;
import TrackTogether.domain.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    void deleteAllByConversation(Conversation conversation);

    @Query("""
        SELECT m
        FROM Message m
        JOIN FETCH m.sender
        WHERE m.conversation.conversationId = :conversationId
        ORDER BY m.timeStamp
        """)
    List<Message> findByConversationIdWithSender(UUID conversationId);

    /** Up to {@code pageable.pageSize} messages sent strictly before {@code ts}, newest first. */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.sender
        WHERE m.conversation.conversationId = :convId
          AND m.timeStamp < :ts
        ORDER BY m.timeStamp DESC
        """)
    List<Message> findMessagesBefore(@Param("convId") UUID convId,
                                     @Param("ts") LocalDateTime ts,
                                     Pageable pageable);

    /** Up to {@code pageable.pageSize} messages sent strictly after {@code ts}, oldest first. */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.sender
        WHERE m.conversation.conversationId = :convId
          AND m.timeStamp > :ts
        ORDER BY m.timeStamp ASC
        """)
    List<Message> findMessagesAfter(@Param("convId") UUID convId,
                                    @Param("ts") LocalDateTime ts,
                                    Pageable pageable);
}