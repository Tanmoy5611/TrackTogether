package TrackTogether.webapi.dto;

import TrackTogether.domain.TransportMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TravelGroupRequestDto {

    @NotNull
    private UUID activityId;

    @NotNull
    @Min(1)
    private Integer maxMembers;

    private String location;

    private String departureLocation;

    private Double departureLatitude;
    private Double departureLongitude;

    @NotNull
    private LocalDateTime departureTime;

    private LocalDateTime estimatedArrivalTime;

    @NotNull
    private TransportMode transportMode;

    public TravelGroupRequestDto() {}
}