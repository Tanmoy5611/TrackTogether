package TrackTogether.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Member recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private String actorName;

    @Column(nullable = false)
    private String groupLocation;

    @Column(nullable = false)
    private UUID groupId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(nullable = false)
    private boolean read = false;

    public Notification() {}

    public Notification(Member recipient, NotificationType type,
                        String actorName, String groupLocation, UUID groupId) {
        this.recipient = recipient;
        this.type = type;
        this.actorName = actorName;
        this.groupLocation = groupLocation;
        this.groupId = groupId;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
}