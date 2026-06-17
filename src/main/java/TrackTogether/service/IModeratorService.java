package TrackTogether.service;

import TrackTogether.domain.Moderator;
import TrackTogether.domain.Report;
import TrackTogether.domain.ReportHistoryEntry;
import TrackTogether.domain.ReportStatus;

import java.util.List;
import java.util.UUID;

public interface IModeratorService {
    List<Report> getAllReports();
    List<Report> searchByStatus(ReportStatus status);
    List<Report> searchPending();
    Report claimReport(UUID reportId);
    Report updateReportStatus(UUID reportId, ReportStatus status);
    Report assignReport(UUID reportId, UUID moderatorId);
    List<Moderator> getAllModerators();

    List<ReportHistoryEntry> getAllReportHistory();

    Report getReportById(UUID reportId);

    List<ReportHistoryEntry> getReportHistoryByReportId(UUID reportId);

    ChatContext getChatContext(UUID reportId);
}
