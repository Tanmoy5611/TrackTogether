package TrackTogether.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Integer id;

    private String address;
    private Double latitude;
    private Double longitude;
    LocalDateTime timestamp;

    @OneToOne(mappedBy = "location")
    private TravelGroupMember travelGroupMember;

    public Location(){}

    public Location(Integer id, String address, Double latitude, Double longitude, LocalDateTime timestamp) {
        this.id = id;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TravelGroupMember getTravelGroupMember() {
        return travelGroupMember;
    }

    public void setTravelGroupMember(TravelGroupMember travelGroupMember) {
        this.travelGroupMember = travelGroupMember;
    }
}