package TrackTogether.repository;

import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelGroupActivityLogRepository extends JpaRepository<TravelGroupActivityLog, Integer> {

    Page<TravelGroupActivityLog> findAllByGroup(TravelGroup group, Pageable pageable);

    long countByGroup(TravelGroup group);

    void deleteAllByGroup(TravelGroup group);
}