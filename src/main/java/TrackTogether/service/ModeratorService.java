package TrackTogether.service;

import TrackTogether.domain.Message;
import TrackTogether.domain.Moderator;
import TrackTogether.domain.Report;
import TrackTogether.domain.ReportHistoryEntry;
import TrackTogether.domain.ReportStatus;
import TrackTogether.domain.User;
import TrackTogether.repository.MessageRepository;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.repository.ReportHistoryRepository;
import TrackTogether.repository.ReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ModeratorService implements IModeratorService {
    private static final int CONTEXT_WINDOW = 5;

    private final ReportRepository reportRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final ModeratorRepository moderatorRepository;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;

    public ModeratorService(ReportRepository reportRepository,
                            ReportHistoryRepository reportHistoryRepository,
                            ModeratorRepository moderatorRepository,
                            MessageRepository messageRepository,
                            CurrentUserService currentUserService) {
        this.reportRepository = reportRepository;
        this.reportHistoryRepository = reportHistoryRepository;
        this.moderatorRepository = moderatorRepository;
        this.messageRepository = messageRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Override
    public List<Report> searchByStatus(ReportStatus status) {
        if (status == null) {
            return getAllReports();
        }
        return reportRepository.findByStatus(status);
    }

    @Override
    public List<Report> searchPending() {
        return reportRepository.findByStatusIn(List.of(ReportStatus.OPEN, ReportStatus.REVIEWED));
    }

    @Override
    public Report claimReport(UUID reportId) {
        Moderator moderator = getCurrentModerator();
        Report report = getReport(reportId);

        ReportStatus fromStatus = report.getStatus();
        report.setModerator(moderator);
        if (report.getStatus() == ReportStatus.OPEN) {
            report.setStatus(ReportStatus.REVIEWED);
        }
        Report saved = reportRepository.save(report);

        recordHistory(saved, fromStatus, saved.getStatus(), moderator);
        return saved;
    }

    @Override
    public Report updateReportStatus(UUID reportId, ReportStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report status is required");
        }

        Moderator moderator = getCurrentModerator();
        Report report = getReport(reportId);
        if (report.getModerator() != null && !report.getModerator().getUserId().equals(moderator.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this report");
        }

        ReportStatus fromStatus = report.getStatus();
        report.setModerator(moderator);
        report.setStatus(status);
        Report saved = reportRepository.save(report);

        recordHistory(saved, fromStatus, status, moderator);
        return saved;
    }

    @Override
    public Report assignReport(UUID reportId, UUID moderatorId) {
        Moderator moderator = moderatorRepository.findById(moderatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderator not found"));

        Report report = getReport(reportId);

        ReportStatus fromStatus = report.getStatus();
        report.setModerator(moderator);
        if (report.getStatus() == ReportStatus.OPEN) {
            report.setStatus(ReportStatus.REVIEWED);
        }
        Report saved = reportRepository.save(report);

        User currentUser = currentUserService.getCurrentUser();
        recordHistory(saved, fromStatus, saved.getStatus(), currentUser);
        return saved;
    }

    @Override
    public List<Moderator> getAllModerators() {
        return moderatorRepository.findAll();
    }

    @Override
    public List<ReportHistoryEntry> getAllReportHistory() {
        return reportHistoryRepository.findAllByOrderByChangedAtDesc();
    }

    @Override
    public Report getReportById(UUID reportId) {
        return reportRepository.findByIdWithMessage(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    @Override
    public List<ReportHistoryEntry> getReportHistoryByReportId(UUID reportId) {
        return reportHistoryRepository.findByReport_ReportIdOrderByChangedAtDesc(reportId);
    }

    @Override
    public ChatContext getChatContext(UUID reportId) {
        Report report = reportRepository.findByIdWithMessageAndConversation(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        Message flagged = report.getMessage();
        UUID convId = flagged.getConversation().getConversationId();
        java.time.LocalDateTime ts = flagged.getTimeStamp();

        // Messages before the flagged one, fetched newest-first then reversed to chronological
        List<Message> before = messageRepository.findMessagesBefore(convId, ts, PageRequest.of(0, CONTEXT_WINDOW));
        Collections.reverse(before);

        // Messages after the flagged one, already in chronological order
        List<Message> after = messageRepository.findMessagesAfter(convId, ts, PageRequest.of(0, CONTEXT_WINDOW));

        List<Message> window = new ArrayList<>(before);
        window.add(flagged);
        window.addAll(after);

        return new ChatContext(flagged.getMessageId(), window);
    }

    private void recordHistory(Report report, ReportStatus fromStatus, ReportStatus toStatus, User changedBy) {
        ReportHistoryEntry entry = new ReportHistoryEntry();
        entry.setReport(report);
        entry.setFromStatus(fromStatus);
        entry.setToStatus(toStatus);
        entry.setChangedBy(changedBy);
        entry.setChangedAt(LocalDateTime.now());
        reportHistoryRepository.save(entry);
    }

    private Moderator getCurrentModerator() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser instanceof Moderator moderator) {
            return moderator;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not a moderator");
    }

    private Report getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }
}