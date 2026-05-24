package TrackTogether.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class TravelGroupActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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

    public Integer getId() {
        return id;
    }

    public TravelGroup getGroup() {
        return group;
    }

    public void setGroup(TravelGroup group) {
        this.group = group;
    }

    public Member getActor() {
        return actor;
    }

    public void setActor(Member actor) {
        this.actor = actor;
    }

    public Member getTargetMember() {
        return targetMember;
    }

    public void setTargetMember(Member targetMember) {
        this.targetMember = targetMember;
    }

    public JoinRequest getJoinRequest() {
        return joinRequest;
    }

    public void setJoinRequest(JoinRequest joinRequest) {
        this.joinRequest = joinRequest;
    }

    public TravelGroupActivityType getType() {
        return type;
    }

    public void setType(TravelGroupActivityType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}