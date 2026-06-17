package TrackTogether.service;

import TrackTogether.domain.Admin;
import TrackTogether.domain.Member;
import TrackTogether.domain.SuperAdmin;
import TrackTogether.repository.AdminRepository;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.repository.SuperAdminRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class RoleService {

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final ModeratorRepository moderatorRepository;

    public RoleService(AdminRepository adminRepository,
                       SuperAdminRepository superAdminRepository,
                       ModeratorRepository moderatorRepository) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
        this.moderatorRepository = moderatorRepository;
    }

    public void updateUserRole(Member user, String newRole) {

        UUID userId = user.getUserId();

        superAdminRepository.deleteById(userId);
        adminRepository.deleteById(userId);
        moderatorRepository.deleteByUserId(userId);

        switch (newRole) {

            case "MEMBER" -> {}

            case "MODERATOR" -> moderatorRepository.insertByUserId(userId);

            case "ADMIN" -> {
                Admin admin = new Admin();
                admin.setUserId(userId);
                adminRepository.save(admin);
            }

            case "SUPER_ADMIN" -> {
                Admin admin = new Admin();
                admin.setUserId(userId);
                adminRepository.save(admin);

                SuperAdmin superAdmin = new SuperAdmin();
                superAdmin.setUserId(userId);
                superAdminRepository.save(superAdmin);
            }

            default -> throw new IllegalArgumentException("Invalid role");
        }
    }
}