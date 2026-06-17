package TrackTogether.repository;

import TrackTogether.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {

    boolean existsByUserId(UUID userId);

    @Query("SELECT a.userId FROM Admin a")
    Set<UUID> findAllUserIds();
}