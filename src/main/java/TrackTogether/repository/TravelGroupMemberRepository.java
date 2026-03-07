package TrackTogether.repository;

import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, Integer> {

    boolean existsByGroupAndMember(TravelGroup group, Member member);

    long countByGroup(TravelGroup group);
}