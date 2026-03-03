package TrackTogether.repository;

import TrackTogether.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
}