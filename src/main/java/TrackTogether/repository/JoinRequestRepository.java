package TrackTogether.repository;

import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Integer> {

    Optional<JoinRequest> findByGroupAndMember(TravelGroup group, Member member);

    List<JoinRequest> findAllByGroup(TravelGroup group);

    List<JoinRequest> findAllByGroupAndStatusOrderByRequestedAtAsc(TravelGroup group, JoinRequestStatus status);

    List<JoinRequest> findAllByMemberAndGroupIn(Member member, List<TravelGroup> groups);

    long countByGroupAndStatus(TravelGroup group, JoinRequestStatus status);

    // Counts requests for all visible groups in one query for the overview badges
    @Query("""
            select joinRequest.group.groupId, count(joinRequest)
            from JoinRequest joinRequest
            where joinRequest.group in :groups
              and joinRequest.status = :status
            group by joinRequest.group.groupId
            """)
    List<Object[]> countByGroupInAndStatus(@Param("groups") List<TravelGroup> groups,
                                           @Param("status") JoinRequestStatus status);
}