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
    private String title;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ConversationType getType() {
        if (type != null) {
            return type;
        }

        return travelGroup == null ? ConversationType.DIRECT : ConversationType.TRAVEL_GROUP;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public TravelGroup getTravelGroup() {
        return travelGroup;
    }

    public void setTravelGroup(TravelGroup travelGroup) {
        this.travelGroup = travelGroup;
    }

    public List<MemberConversation> getMembers() {
        return members;
    }

    public void setMembers(List<MemberConversation> members) {
        this.members = members;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

}