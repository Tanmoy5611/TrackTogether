package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;

import java.util.UUID;

// DTO used to send TravelGroup data to API clients (response)
public class TravelGroupDto {

    private UUID groupId;          // unique identifier of the group
    private Integer maxMembers;    // maximum number of members allowed
    private String location;       // meeting location of the group
    private TransportMode transportMode;
    private UUID ownerId;
    private String ownerName;

    // Constructor used by mapper to create DTO
    public TravelGroupDto(UUID groupId,
                          Integer maxMembers,
                          String location,
                          TransportMode transportMode,
                          UUID ownerId,
                          String ownerName) {
        this.groupId = groupId;
        this.maxMembers = maxMembers;
        this.location = location;
        this.transportMode = transportMode;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    // Getters and Setters
    public UUID getGroupId() {
        return groupId;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public String getLocation() {
        return location;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }
}