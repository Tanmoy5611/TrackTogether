package TrackTogether.webapi.dto;

import TrackTogether.domain.ReportStatus;

import java.time.LocalDateTime;

public record ReportHistoryEntryDto(
        ReportStatus fromStatus,
        ReportStatus toStatus,
        LocalDateTime changedAt,
        String changedByName
) {}