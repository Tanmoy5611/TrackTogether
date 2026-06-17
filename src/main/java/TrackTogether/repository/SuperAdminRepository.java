package TrackTogether.repository;


import TrackTogether.domain.SuperAdmin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, UUID> {

    Boolean existsByUserId(UUID userId);

    @Query("SELECT a.userId FROM SuperAdmin a")
    Set<UUID> findAllUserIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SuperAdmin a")
    List<SuperAdmin> findAllForUpdate();
}