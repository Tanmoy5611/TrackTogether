package TrackTogether.repository;

import TrackTogether.domain.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TravelGroupRepository extends JpaRepository<TravelGroup, UUID> {
    List<TravelGroup> findAllByActivity_Id(UUID activityId);
}
