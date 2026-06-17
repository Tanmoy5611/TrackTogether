package TrackTogether.webapi;

import TrackTogether.domain.Moderator;
import TrackTogether.domain.Report;
import TrackTogether.domain.ReportHistoryEntry;
import TrackTogether.domain.ReportStatus;
import TrackTogether.service.IModeratorService;
import TrackTogether.service.ChatContext;
import TrackTogether.webapi.dto.ChatContextMessageDto;
import TrackTogether.webapi.dto.ModeratorDto;
import TrackTogether.webapi.dto.ReportDetailDto;
import TrackTogether.webapi.dto.ReportDto;
import TrackTogether.webapi.dto.ReportHistoryEntryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/moderators")
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorApiController {

    private final IModeratorService moderatorService;

    public ModeratorApiController(IModeratorService moderatorService) {
        this.moderatorService = moderatorService;
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportDetailDto> getReportDetail(@PathVariable UUID reportId) {
        Report r = moderatorService.getReportById(reportId);
        return ResponseEntity.ok(toDetailDto(r));
    }

    @GetMapping("/reports/{reportId}/context")
    public ResponseEntity<List<ChatContextMessageDto>> getReportChatContext(@PathVariable UUID reportId) {
        ChatContext ctx = moderatorService.getChatContext(reportId);
        List<ChatContextMessageDto> body = ctx.messages().stream()
                .map(m -> new ChatContextMessageDto(
                        m.getMessageId(),
                        m.getSender().getName(),
                        m.getMessage(),
                        m.getTimeStamp(),
                        m.getMessageId().equals(ctx.flaggedMessageId())
                ))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/reports/{reportId}/history")
    public ResponseEntity<List<ReportHistoryEntryDto>> getReportHistory(@PathVariable UUID reportId) {
        List<ReportHistoryEntry> history = moderatorService.getReportHistoryByReportId(reportId);
        List<ReportHistoryEntryDto> body = history.stream()
                .map(e -> new ReportHistoryEntryDto(
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getChangedAt(),
                        e.getChangedBy() != null ? e.getChangedBy().getName() : "System"
                ))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReportDto>> getReports(@RequestParam(required = false) ReportStatus status,
                                                      @RequestParam(required = false, defaultValue = "false") boolean pending) {
        List<Report> reports = pending ? moderatorService.searchPending() : moderatorService.searchByStatus(status);

        List<ReportDto> body = reports.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(body);
    }

    @PostMapping("/reports/{reportId}/claim")
    public ResponseEntity<ReportDto> claimReport(@PathVariable UUID reportId) {
        Report updated = moderatorService.claimReport(reportId);
        return ResponseEntity.ok(toDto(updated));
    }

    @PatchMapping("/reports/{reportId}/status")
    public ResponseEntity<ReportDto> updateReportStatus(@PathVariable UUID reportId,
                                                        @RequestParam ReportStatus status) {
        Report updated = moderatorService.updateReportStatus(reportId, status);
        return ResponseEntity.ok(toDto(updated));
    }

    // ── Admin-only: assign a report to a specific moderator ──────────────────

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ModeratorDto>> getModerators() {
        List<Moderator> moderators = moderatorService.getAllModerators();
        List<ModeratorDto> body = moderators.stream()
                .map(m -> new ModeratorDto(m.getUserId(), m.getName()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/reports/{reportId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> assignReport(@PathVariable UUID reportId,
                                                  @RequestParam UUID moderatorId) {
        Report updated = moderatorService.assignReport(reportId, moderatorId);
        return ResponseEntity.ok(toDto(updated));
    }

    // ── History ──────────────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<List<ReportHistoryEntryDto>> getAllReportHistory() {
        List<ReportHistoryEntry> history = moderatorService.getAllReportHistory();
        List<ReportHistoryEntryDto> body = history.stream()
                .map(e -> new ReportHistoryEntryDto(
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getChangedAt(),
                        e.getChangedBy() != null ? e.getChangedBy().getName() : "System"
                ))
                .toList();
        return ResponseEntity.ok(body);
    }

    // Helpers

    private ReportDto toDto(Report r) {
        String moderatorName = r.getModerator() != null ? r.getModerator().getName() : null;
        return new ReportDto(r.getReportId(), r.getReason(), r.getCreatedAt(), r.getStatus(), moderatorName);
    }

    private ReportDetailDto toDetailDto(Report r) {
        String reporterName  = r.getReporter()  != null ? r.getReporter().getName()  : null;
        String reporterEmail = r.getReporter()  != null ? r.getReporter().getEmail() : null;
        String reportedName  = r.getReported()  != null ? r.getReported().getName()  : null;
        String reportedEmail = r.getReported()  != null ? r.getReported().getEmail() : null;
        String msgContent    = r.getMessage()   != null ? r.getMessage().getMessage() : null;
        var    msgTimestamp  = r.getMessage()   != null ? r.getMessage().getTimeStamp() : null;
        String modName       = r.getModerator() != null ? r.getModerator().getName()  : null;

        return new ReportDetailDto(
                r.getReportId(),
                r.getReason(),
                r.getCreatedAt(),
                r.getStatus(),
                reporterName,
                reporterEmail,
                reportedName,
                reportedEmail,
                msgContent,
                msgTimestamp,
                modName
        );
    }
}