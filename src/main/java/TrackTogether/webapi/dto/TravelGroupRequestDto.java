package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// DTO used to receive data from API request (input)
public class TravelGroupRequestDto {

    @NotNull
    private UUID activityId;           // activity to attach group to
    private Integer maxMembers;        // max capacity of group
    private String location;           // meeting location
    private TransportMode transportMode; // transport type

    public TravelGroupRequestDto() {}

    // Getters and Setters
    public UUID getActivityId() {
        return activityId;
    }

    public void setActivityId(UUID activityId) {
        this.activityId = activityId;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }
}