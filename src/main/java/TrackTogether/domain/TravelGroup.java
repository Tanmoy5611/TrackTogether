package TrackTogether.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class TravelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupId;   // unique id for the travel group

    @Enumerated(EnumType.STRING)
    private TransportMode transportMode;   // car, bike, bus etc

    private String location;         // meeting location
    private Integer maxMembers;  // number of available spots

    @ManyToOne(optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;       // the activity this group belongs to

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;            // member who created the group

    @OneToOne(mappedBy = "travelGroup", optional = false)
    private Conversation conversation;  // conversation for the group

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<TravelGroupMember> members = new ArrayList<>();   // people in the group

    public TravelGroup(){
        // required by JPA
    }

    public TravelGroup(Integer maxMembers, String location, TransportMode transportMode) {
        this.maxMembers = maxMembers;
        this.location = location;
        this.transportMode = transportMode;
    }

    // Getters and Setters
    public UUID getGroupId() {
        return groupId;
    }
    public TransportMode getTransportMode() {
        return transportMode;
    }
    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public Integer getMaxMembers() {
        return maxMembers;
    }
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    public Activity getActivity() {
        return activity;
    }
    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    public Member getOwner() {
        return owner;
    }
    public void setOwner(Member owner) {
        this.owner = owner;
    }
    public Conversation getConversation() {
        return conversation;
    }
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }
    public List<TravelGroupMember> getMembers() {
        return members;
    }
    public void setMembers(List<TravelGroupMember> members) {
        this.members = members;
    }

    // Method to add a member
    public void addMember(TravelGroupMember member) {
        members.add(member);
        member.setGroup(this);
    }

    // Method to check the availability of spots (Domain-driven Design)
    public boolean hasAvailableSpots(long currentMembers) {
        return currentMembers < maxMembers;
    }
}