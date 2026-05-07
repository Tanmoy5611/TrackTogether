package TrackTogether.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Member extends User {

    private double co2Saved;

    // Preferences used by the friend matching service
    @Enumerated(EnumType.STRING)
    private TransportMode preferredTransportMode;
    private String defaultDepartureLocation;
    private Double defaultLatitude;
    private Double defaultLongitude;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private List<Activity> activities = new ArrayList<>();

    public Member() {}

    public Member(Boolean status, String email, String name, UUID userId, Double co2Saved) {
        super(status, email, name, userId);
        this.co2Saved = co2Saved;
    }

    public double getCo2Saved() {
        return co2Saved;
    }

    public void setCo2Saved(double co2Saved) {
        this.co2Saved = co2Saved;
    }

    public TransportMode getPreferredTransportMode() {
        return preferredTransportMode;
    }

    public void setPreferredTransportMode(TransportMode preferredTransportMode) {
        this.preferredTransportMode = preferredTransportMode;
    }

    public String getDefaultDepartureLocation() {
        return defaultDepartureLocation;
    }

    public void setDefaultDepartureLocation(String defaultDepartureLocation) {
        this.defaultDepartureLocation = defaultDepartureLocation;
    }

    public Double getDefaultLatitude() {
        return defaultLatitude;
    }

    public void setDefaultLatitude(Double defaultLatitude) {
        this.defaultLatitude = defaultLatitude;
    }

    public Double getDefaultLongitude() {
        return defaultLongitude;
    }

    public void setDefaultLongitude(Double defaultLongitude) {
        this.defaultLongitude = defaultLongitude;
    }

    public List<Activity> getActivities() {
        return activities;
    }
}