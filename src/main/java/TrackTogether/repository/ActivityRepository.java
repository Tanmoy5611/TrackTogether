package TrackTogether.repository;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    long countByCreator(Member creator);

    List<Activity> findAllByVerificationStatusOrCreator(ActivityVerificationStatus verificationStatus, Member creator);

}