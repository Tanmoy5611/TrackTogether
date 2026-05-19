package TrackTogether.domain;

import jakarta.persistence.*;


// guarantees duplicates cannot exist
@Table(
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    public TravelGroupMember(){

    }

    public TravelGroupMember(TravelGroup group, Member member, Location location) {
        this.group = group;
        this.member = member;
        this.location = location;
    }

    public TravelGroup getGroup() {
        return group;
    }

    public void setGroup(TravelGroup group) {
        this.group = group;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}