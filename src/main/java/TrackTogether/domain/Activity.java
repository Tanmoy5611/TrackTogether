package TrackTogether.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
public class Activity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String description;
    private String location;

    // Distance from the meeting/start point to the activity. Used for CO2 reporting.
    private Double distanceKm;

    // Coordinates used by map views and route planning.
    private Double latitude;
    private Double longitude;

    private LocalDate date;
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    private ActivityVerificationStatus verificationStatus = ActivityVerificationStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private Member creator;

    public Activity() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public ActivityVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(ActivityVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Member getCreator() {
        return creator;
    }

    public void setCreator(Member creator) {
        this.creator = creator;
    }
}