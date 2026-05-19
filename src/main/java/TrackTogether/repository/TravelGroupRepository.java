package TrackTogether.repository;

import TrackTogether.domain.TravelGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TravelGroupRepository extends JpaRepository<TravelGroup, UUID> {
    List<TravelGroup> findAllByActivity_Id(UUID activityId);

    // Locks the selected travel group row during seat-sensitive operations
    // This prevents two concurrent requests from both taking the last available seat
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select travelGroup from TravelGroup travelGroup where travelGroup.groupId = :groupId")
    Optional<TravelGroup> findByIdForUpdate(@Param("groupId") UUID groupId);
}