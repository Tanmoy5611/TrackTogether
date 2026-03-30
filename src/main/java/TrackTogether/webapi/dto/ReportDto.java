package TrackTogether.webapi.dto;

import TrackTogether.domain.ReportStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReportDto(
        UUID reportId,
        String reason,
        LocalDate createdAt,
        ReportStatus status
) {

}
