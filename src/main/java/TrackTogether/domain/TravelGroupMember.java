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

    public TravelGroupMember(){

    }

    public TravelGroupMember(Member member, TravelGroup group, Integer id) {
        this.group = group;
        this.member = member;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
