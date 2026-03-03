package TrackTogether.repository;

import TrackTogether.domain.TravelGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, Integer> {
}