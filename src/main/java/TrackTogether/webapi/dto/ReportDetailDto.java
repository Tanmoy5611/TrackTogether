package TrackTogether.webapi.dto;

import TrackTogether.domain.ReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReportDetailDto(
        UUID reportId,
        String reason,
        LocalDate createdAt,
        ReportStatus status,

        // Reporter
        String reporterName,
        String reporterEmail,

        // Reported
        String reportedName,
        String reportedEmail,

        // Flagged message
        String messageContent,
        LocalDateTime messageTimestamp,

        // Assigned moderator
        String assignedModeratorName
) {
}