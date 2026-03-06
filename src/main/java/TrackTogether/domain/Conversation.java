package TrackTogether.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID conversationId;
    private LocalDateTime createdAt;

    @OneToOne(optional = true)
    @JoinColumn(name = "travel_group_id", unique = true)
    private TravelGroup travelGroup;

    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
    private List<MemberConversation> members = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    public Conversation(){

    }

    public Conversation(UUID conversationId, LocalDateTime createdAt) {
        this.conversationId = conversationId;
        this.createdAt = createdAt;
    }

    public void addMessage(Message message){

    }

    public void getMessages(List<Message> message){

    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TravelGroup getTravelGroup() {
        return travelGroup;
    }

    public void setTravelGroup(TravelGroup travelGroup) {
        this.travelGroup = travelGroup;
    }
}
