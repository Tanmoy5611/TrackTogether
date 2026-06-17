package TrackTogether.webapi;

import TrackTogether.domain.Member;
import TrackTogether.domain.Report;
import TrackTogether.service.CurrentUserService;
import TrackTogether.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageReportApiController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    public MessageReportApiController(ReportService reportService,
                                      CurrentUserService currentUserService) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{messageId}/reports")
    public ResponseEntity<Void> createReportForMessage(@PathVariable UUID messageId,
                                                       @RequestBody CreateReportRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        Member reporter = currentUserService.getCurrentUser();
        Report created = reportService.createReportForMessage(messageId, reporter, request.reason());
        URI location = uriBuilder
                .path("/api/messages/{messageId}/reports/{reportId}")
                .buildAndExpand(messageId, created.getReportId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    public record CreateReportRequest(String reason) {}
}