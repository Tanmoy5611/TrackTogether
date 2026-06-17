package TrackTogether.dto.delijn;

import java.time.LocalDateTime;

public class DeLijnDepartureDto {

    private final String entityNumber;
    private final String stopNumber;
    private final String lineNumber;
    private final String transportType;
    private final String destination;
    private final LocalDateTime scheduledDepartureTime;
    private final LocalDateTime realtimeDepartureTime;
    private final boolean cancelled;

    // Creates a departure without an explicit bus/tram transport type
    public DeLijnDepartureDto(String entityNumber,
                              String stopNumber,
                              String lineNumber,
                              String destination,
                              LocalDateTime scheduledDepartureTime,
                              LocalDateTime realtimeDepartureTime,
                              boolean cancelled) {
        this(entityNumber, stopNumber, lineNumber, null, destination, scheduledDepartureTime, realtimeDepartureTime, cancelled);
    }

    // Creates a departure with all De Lijn fields needed by route suggestions
    public DeLijnDepartureDto(String entityNumber,
                              String stopNumber,
                              String lineNumber,
                              String transportType,
                              String destination,
                              LocalDateTime scheduledDepartureTime,
                              LocalDateTime realtimeDepartureTime,
                              boolean cancelled) {
        this.entityNumber = entityNumber;
        this.stopNumber = stopNumber;
        this.lineNumber = lineNumber;
        this.transportType = transportType;
        this.destination = destination;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.realtimeDepartureTime = realtimeDepartureTime;
        this.cancelled = cancelled;
    }

    public String getEntityNumber() {
        return entityNumber;
    }

    public String getStopNumber() {
        return stopNumber;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getTransportType() {
        return transportType;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public LocalDateTime getRealtimeDepartureTime() {
        return realtimeDepartureTime;
    }

    // Indicates whether De Lijn marked this departure as cancelled
    public boolean isCancelled() {
        return cancelled;
    }
}