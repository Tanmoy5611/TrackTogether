package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;


// DTO used to receive data from API request (input)
public class TravelGroupRequestDto {

    @NotNull
    private UUID activityId;           // activity to attach group to

    @NotNull
    @Min(1)
    private Integer maxMembers;        // max capacity of group

    private String location;           // legacy display/meeting point

    private String departureLocation;  // route starting point

    private Double departureLatitude;  // start latitude
    private Double departureLongitude; // start longitude

    @NotNull
    private LocalDateTime departureTime; // planned leave time

    private LocalDateTime estimatedArrivalTime; // estimated arrival

    @NotNull
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

    public String getDepartureLocation() {
        return departureLocation;
    }

    public void setDepartureLocation(String departureLocation) {
        this.departureLocation = departureLocation;
    }

    public Double getDepartureLatitude() {
        return departureLatitude;
    }

    public void setDepartureLatitude(Double departureLatitude) {
        this.departureLatitude = departureLatitude;
    }

    public Double getDepartureLongitude() {
        return departureLongitude;
    }

    public void setDepartureLongitude(Double departureLongitude) {
        this.departureLongitude = departureLongitude;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

}