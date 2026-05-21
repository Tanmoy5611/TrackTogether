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

    // Returns the user-facing route suggestion status message
    public String getMessage() {
        return message;
    }

    // Returns the origin label used for the route query
    public String getOriginLabel() {
        return originLabel;
    }

    // Returns the origin latitude used for the route query
    public Double getOriginLatitude() {
        return originLatitude;
    }

    // Returns the origin longitude used for the route query
    public Double getOriginLongitude() {
        return originLongitude;
    }

    // Returns the destination label used for the route query
    public String getDestinationLabel() {
        return destinationLabel;
    }

    // Returns the destination latitude used for the route query
    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    // Returns the destination longitude used for the route query
    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    // Returns text describing De Lijn transport coverage
    public String getTransitCoverage() {
        return transitCoverage;
    }

    // Returns the suggested De Lijn route options
    public List<DeLijnRouteOptionDto> getOptions() {
        return options;
    }
}