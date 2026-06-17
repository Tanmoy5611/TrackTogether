package TrackTogether.repository;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import TrackTogether.domain.Member;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByMembers_Member_UserId(UUID userId);

    List<Conversation> findByMembers_Member_UserIdAndTravelGroupIsNull(UUID userId);

    List<Conversation> findByMembers_Member_UserIdAndTravelGroupIsNotNull(UUID userId);

    List<Conversation> findByMembers_Member_UserIdAndType(UUID userId, ConversationType type);

    Optional<Conversation> findByTravelGroup_GroupId(UUID groupId);

    @Query("""
        SELECT c FROM Conversation c
        JOIN c.members firstMemberConversation
        JOIN c.members secondMemberConversation
        WHERE c.travelGroup IS NULL
          AND (c.type IS NULL OR c.type = TrackTogether.domain.ConversationType.DIRECT)
          AND firstMemberConversation.member = :firstMember
          AND secondMemberConversation.member = :secondMember
        """)
    Optional<Conversation> findConversationByMembers(Member firstMember, Member secondMember);
}