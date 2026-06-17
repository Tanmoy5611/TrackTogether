package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;

import java.time.LocalDateTime;
import java.util.UUID;

// DTO used to send TravelGroup data to API clients (response)
public class TravelGroupDto {

    private UUID groupId;          // unique identifier of the group
    private Integer maxMembers;    // maximum number of members allowed
    private String location;       // meeting/departure point
    private String departureLocation; // route starting point
    private Double departureLatitude; // start latitude
    private Double departureLongitude; // start longitude
    private Double arrivalLatitude;   // destination latitude
    private Double arrivalLongitude;  // destination longitude
    private LocalDateTime departureTime; // planned leave time
    private LocalDateTime estimatedArrivalTime; // estimated arrival
    private TransportMode transportMode;
    private UUID ownerId;
    private String ownerName;


    // Constructor used by mapper to create DTO
    public TravelGroupDto(UUID groupId,
                          Integer maxMembers,
                          String location,
                          String departureLocation,
                          Double departureLatitude,
                          Double departureLongitude,
                          Double arrivalLatitude,
                          Double arrivalLongitude,
                          LocalDateTime departureTime,
                          LocalDateTime estimatedArrivalTime,
                          TransportMode transportMode,
                          UUID ownerId,
                          String ownerName) {
        this.groupId = groupId;
        this.maxMembers = maxMembers;
        this.location = location;
        this.departureLocation = departureLocation;
        this.departureLatitude = departureLatitude;
        this.departureLongitude = departureLongitude;
        this.arrivalLatitude = arrivalLatitude;
        this.arrivalLongitude = arrivalLongitude;
        this.departureTime = departureTime;
        this.estimatedArrivalTime = estimatedArrivalTime;
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

    public String getDepartureLocation() {
        return departureLocation;
    }

    public Double getDepartureLatitude() {
        return departureLatitude;
    }

    public Double getDepartureLongitude() {
        return departureLongitude;
    }

    public Double getArrivalLatitude() {
        return arrivalLatitude;
    }

    public Double getArrivalLongitude() {
        return arrivalLongitude;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

}