package TrackTogether.webapi.dto;

import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;

import java.util.UUID;

// Response DTO so the API does not return the full Member entity after saving
public class MemberTravelPreferenceDto {

    private final UUID memberId;
    private final TransportMode preferredTransportMode;
    private final String defaultDepartureLocation;
    private final Double defaultLatitude;
    private final Double defaultLongitude;

    public MemberTravelPreferenceDto(UUID memberId,
                                     TransportMode preferredTransportMode,
                                     String defaultDepartureLocation,
                                     Double defaultLatitude,
                                     Double defaultLongitude) {
        this.memberId = memberId;
        this.preferredTransportMode = preferredTransportMode;
        this.defaultDepartureLocation = defaultDepartureLocation;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
    }

    public static MemberTravelPreferenceDto from(Member member) {
        return new MemberTravelPreferenceDto(
                member.getUserId(),
                member.getPreferredTransportMode(),
                member.getDefaultDepartureLocation(),
                member.getDefaultLatitude(),
                member.getDefaultLongitude()
        );
    }

    public UUID getMemberId() {
        return memberId;
    }

    public TransportMode getPreferredTransportMode() {
        return preferredTransportMode;
    }

    public String getDefaultDepartureLocation() {
        return defaultDepartureLocation;
    }

    public Double getDefaultLatitude() {
        return defaultLatitude;
    }

    public Double getDefaultLongitude() {
        return defaultLongitude;
    }
}