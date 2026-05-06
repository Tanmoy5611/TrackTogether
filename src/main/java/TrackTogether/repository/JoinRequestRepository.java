package TrackTogether.repository;

import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Integer> {

    Optional<JoinRequest> findByGroupAndMember(TravelGroup group, Member member);

    List<JoinRequest> findAllByGroup(TravelGroup group);

    List<JoinRequest> findAllByGroupAndStatusOrderByRequestedAtAsc(TravelGroup group, JoinRequestStatus status);

    List<JoinRequest> findAllByMemberAndGroupIn(Member member, List<TravelGroup> groups);

    long countByGroupAndStatus(TravelGroup group, JoinRequestStatus status);
}