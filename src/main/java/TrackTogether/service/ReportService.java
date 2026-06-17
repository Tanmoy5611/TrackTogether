package TrackTogether.service;

import TrackTogether.domain.Member;
import TrackTogether.domain.Message;
import TrackTogether.domain.Report;
import TrackTogether.domain.ReportStatus;
import TrackTogether.exceptions.NotFoundException;
import TrackTogether.repository.MessageRepository;
import TrackTogether.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;


@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final MessageRepository messageRepository;

    public ReportService(ReportRepository reportRepository,
                         MessageRepository messageRepository) {
        this.reportRepository = reportRepository;
        this.messageRepository = messageRepository;
    }

    public Report createReportForMessage(UUID messageId, Member reporter, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required.");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> NotFoundException.forMessageId(messageId));

        Member reported = message.getSender();

        if (reported.getUserId().equals(reporter.getUserId())) {
            throw new IllegalArgumentException("You cannot report your own message.");
        }

        if (reportRepository.existsByMessage_MessageIdAndReporter_UserId(messageId, reporter.getUserId())) {
            throw new IllegalArgumentException("You have already reported this message.");
        }

        Report report = new Report();
        report.setReason(reason);
        report.setCreatedAt(LocalDate.now());
        report.setStatus(ReportStatus.OPEN);
        report.setReporter(reporter);
        report.setReported(reported);
        report.setMessage(message);

        return reportRepository.save(report);
    }
}