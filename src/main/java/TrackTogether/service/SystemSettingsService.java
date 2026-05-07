package TrackTogether.service;

import TrackTogether.domain.SystemSettings;
import TrackTogether.repository.SystemSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SystemSettingsRepository systemSettingsRepository;

    public SystemSettingsService(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public SystemSettings getSettings() {
        // If the database is still empty, create the default settings automatically
        return systemSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);
    }

    public boolean isTravelGroupJoinApprovalEnabled() {
        return getSettings().isTravelGroupJoinApprovalEnabled();
    }

    @Transactional
    public SystemSettings updateTravelGroupJoinApproval(boolean enabled) {
        SystemSettings settings = getSettings();
        settings.setTravelGroupJoinApprovalEnabled(enabled);
        return systemSettingsRepository.save(settings);
    }

    private SystemSettings createDefaultSettings() {
        SystemSettings settings = new SystemSettings();
        // Default is direct join, because that is the normal project requirement
        settings.setTravelGroupJoinApprovalEnabled(false);
        return systemSettingsRepository.save(settings);
    }
}