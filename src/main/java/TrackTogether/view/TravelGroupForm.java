package TrackTogether.view;

import TrackTogether.domain.TransportMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TravelGroupForm {

    @NotNull
    private UUID activityId;

    @NotNull
    @Min(1)
    private Integer maxMembers;

    // Kept for older views/API flows that still display one generic location.
    private String location;

    @NotBlank
    private String departureLocation;

    private Double departureLatitude;
    private Double departureLongitude;

    @NotNull
    private LocalDateTime departureTime;

    private LocalDateTime estimatedArrivalTime;

    @NotNull
    private TransportMode mode;
}