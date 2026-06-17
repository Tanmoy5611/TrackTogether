package TrackTogether.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
public class TravelGroupActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup group;

    @ManyToOne(optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Member actor;

    @ManyToOne
    @JoinColumn(name = "target_member_id")
    private Member targetMember;

    @ManyToOne
    @JoinColumn(name = "join_request_id")
    private JoinRequest joinRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelGroupActivityType type;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}