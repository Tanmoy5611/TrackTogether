package TrackTogether.webapi.dto;

import TrackTogether.dto.delijn.DeLijnRouteOptionDto;

import java.util.List;

public class TravelGroupRouteSuggestionsDto {

    private final boolean supported;
    private final boolean configured;
    private final String message;
    private final String originLabel;
    private final Double originLatitude;
    private final Double originLongitude;
    private final String destinationLabel;
    private final Double destinationLatitude;
    private final Double destinationLongitude;
    private final String transitCoverage;
    private final List<DeLijnRouteOptionDto> options;

    // Creates the route suggestion response consumed by the route suggestions page
    public TravelGroupRouteSuggestionsDto(boolean supported,
                                          boolean configured,
                                          String message,
                                          String originLabel,
                                          Double originLatitude,
                                          Double originLongitude,
                                          String destinationLabel,
                                          Double destinationLatitude,
                                          Double destinationLongitude,
                                          String transitCoverage,
                                          List<DeLijnRouteOptionDto> options) {
        this.supported = supported;
        this.configured = configured;
        this.message = message;
        this.originLabel = originLabel;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLabel = destinationLabel;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.transitCoverage = transitCoverage;
        this.options = List.copyOf(options);
    }

    // Indicates whether route suggestions apply to this travel group
    public boolean isSupported() {
        return supported;
    }

    // Indicates whether De Lijn API settings are available
    public boolean isConfigured() {
        return configured;
    }

    public String getMessage() {
        return message;
    }

    public String getOriginLabel() {
        return originLabel;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public String getDestinationLabel() {
        return destinationLabel;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public String getTransitCoverage() {
        return transitCoverage;
    }

    public List<DeLijnRouteOptionDto> getOptions() {
        return options;
    }
}