package TrackTogether.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
public class ReportHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Report report;

    @Enumerated(EnumType.STRING)
    private ReportStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    private User changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;
}