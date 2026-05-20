package TrackTogether.repository;

import TrackTogether.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByOriginalId(String originalId);

    Optional<Member> findByEmailIgnoreCase(String email);
}