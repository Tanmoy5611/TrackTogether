package TrackTogether.dto.delijn;

import java.time.LocalDateTime;
import java.util.List;

public class DeLijnRouteOptionDto {

    private final DeLijnStopDto originStop;
    private final DeLijnStopDto destinationStop;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final List<String> lineNumbers;
    private final String transportType;
    private final String lineDestination;
    private final String note;
    private final boolean realtime;
    private final boolean arrivesBeforeRequestedTime;

    // Creates a basic route option without transport type or display note metadata
    public DeLijnRouteOptionDto(DeLijnStopDto originStop,
                                DeLijnStopDto destinationStop,
                                LocalDateTime departureTime,
                                LocalDateTime arrivalTime,
                                List<String> lineNumbers,
                                boolean realtime,
                                boolean arrivesBeforeRequestedTime) {
        this(
                originStop,
                destinationStop,
                departureTime,
                arrivalTime,
                lineNumbers,
                null,
                null,
                null,
                realtime,
                arrivesBeforeRequestedTime
        );
    }

    // Creates a route option with destination/note text but without an explicit transport type
    public DeLijnRouteOptionDto(DeLijnStopDto originStop,
                                DeLijnStopDto destinationStop,
                                LocalDateTime departureTime,
                                LocalDateTime arrivalTime,
                                List<String> lineNumbers,
                                String lineDestination,
                                String note,
                                boolean realtime,
                                boolean arrivesBeforeRequestedTime) {
        this(
                originStop,
                destinationStop,
                departureTime,
                arrivalTime,
                lineNumbers,
                null,
                lineDestination,
                note,
                realtime,
                arrivesBeforeRequestedTime
        );
    }

    // Creates a full route option for API responses and frontend map cards
    public DeLijnRouteOptionDto(DeLijnStopDto originStop,
                                DeLijnStopDto destinationStop,
                                LocalDateTime departureTime,
                                LocalDateTime arrivalTime,
                                List<String> lineNumbers,
                                String transportType,
                                String lineDestination,
                                String note,
                                boolean realtime,
                                boolean arrivesBeforeRequestedTime) {
        this.originStop = originStop;
        this.destinationStop = destinationStop;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.lineNumbers = List.copyOf(lineNumbers);
        this.transportType = transportType;
        this.lineDestination = lineDestination;
        this.note = note;
        this.realtime = realtime;
        this.arrivesBeforeRequestedTime = arrivesBeforeRequestedTime;
    }

    public DeLijnStopDto getOriginStop() {
        return originStop;
    }

    public DeLijnStopDto getDestinationStop() {
        return destinationStop;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public List<String> getLineNumbers() {
        return lineNumbers;
    }

    public String getTransportType() {
        return transportType;
    }

    public String getLineDestination() {
        return lineDestination;
    }

    public String getNote() {
        return note;
    }

    // Indicates whether the option came from live departure data
    public boolean isRealtime() {
        return realtime;
    }

    // Indicates whether the option arrives before the requested arrival time
    public boolean isArrivesBeforeRequestedTime() {
        return arrivesBeforeRequestedTime;
    }
}