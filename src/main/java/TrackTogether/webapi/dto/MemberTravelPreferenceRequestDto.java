package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request body for saving the logged-in member's travel preferences
public class MemberTravelPreferenceRequestDto {

    @NotNull
    private TransportMode preferredTransportMode;

    @NotBlank
    private String defaultDepartureLocation;

    private Double defaultLatitude;
    private Double defaultLongitude;

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
}