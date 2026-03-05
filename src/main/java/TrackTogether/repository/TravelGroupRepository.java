package TrackTogether.repository;

import TrackTogether.domain.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TravelGroupRepository extends JpaRepository<TravelGroup, UUID> {
}
