package TrackTogether.repository;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberConversationRepository extends JpaRepository<MemberConversation, Integer> {
    boolean existsByConversationAndMember(Conversation conversation, Member member);

    Optional<MemberConversation> findByConversationAndMember(Conversation conversation, Member member);

    List<MemberConversation> findAllByConversation(Conversation conversation);
}