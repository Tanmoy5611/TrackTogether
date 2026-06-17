package TrackTogether.repository;

import TrackTogether.domain.Moderator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface ModeratorRepository extends JpaRepository<Moderator, UUID> {

    @Query(value = "SELECT EXISTS (SELECT 1 FROM moderator WHERE user_id = :userId)", nativeQuery = true)
    boolean existsByUserId(@Param("userId") UUID userId);

    @Query("SELECT m.userId FROM Moderator m")
    Set<UUID> findAllUserIds();

    @Modifying
    @Query(value = "INSERT INTO moderator (user_id) VALUES (:userId)", nativeQuery = true)
    void insertByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM moderator WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") UUID userId);
}