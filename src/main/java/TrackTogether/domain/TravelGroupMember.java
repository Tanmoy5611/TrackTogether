package TrackTogether.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(
        name = "travel_group_member",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"group_id", "member_id"}
        )
)
@Entity
public class TravelGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private TravelGroup group;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // A shared location belongs to exactly one membership and is removed with that membership.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "location_id")
    private Location location;

    public TravelGroupMember() {
    }

    public TravelGroupMember(TravelGroup group, Member member, Location location) {
        this.group = group;
        this.member = member;
        this.location = location;
    }
}