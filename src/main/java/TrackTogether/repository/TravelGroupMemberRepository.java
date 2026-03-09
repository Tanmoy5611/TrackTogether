package TrackTogether.repository;

import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, Integer> {

    boolean existsByGroupAndMember(TravelGroup group, Member member);

    long countByGroup(TravelGroup group);

    Optional<TravelGroupMember> findByGroupAndMember(TravelGroup group, Member member);
}