package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import TrackTogether.repository.AdminRepository;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.repository.SuperAdminRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActivityPolicyService {

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final ModeratorRepository moderatorRepository;

    public ActivityPolicyService(AdminRepository adminRepository,
                                 SuperAdminRepository superAdminRepository,
                                 ModeratorRepository moderatorRepository) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
        this.moderatorRepository = moderatorRepository;
    }

    public boolean isVisibleTo(Activity activity, Member member) {
        if (activity == null) {
            return true;
        }

        if (activity.getVerificationStatus() == ActivityVerificationStatus.APPROVED) {
            return true;
        }

        return isCreatedBy(activity, member);
    }

    public boolean isKdgActivity(Activity activity) {
        if (activity == null || activity.getCreator() == null || activity.getCreator().getUserId() == null) {
            return false;
        }

        UUID creatorId = activity.getCreator().getUserId();
        return adminRepository.existsByUserId(creatorId)
                || superAdminRepository.existsByUserId(creatorId)
                || moderatorRepository.existsByUserId(creatorId);
    }

    private static boolean isCreatedBy(Activity activity, Member member) {
        return activity.getCreator() != null
                && activity.getCreator().getUserId() != null
                && member != null
                && activity.getCreator().getUserId().equals(member.getUserId());
    }
}