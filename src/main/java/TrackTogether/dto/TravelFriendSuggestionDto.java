package TrackTogether.dto;

import TrackTogether.domain.TransportMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// DTO returned by the matching service with the score and reason texts
public class TravelFriendSuggestionDto {

    private final UUID groupId;
    private final UUID activityId;
    private final String activityName;
    private final String departureLocation;
    private final LocalDateTime departureTime;
    private final TransportMode transportMode;
    private final long availableSpots;
    private final int score;
    private final List<String> matchReasons;

    public TravelFriendSuggestionDto(UUID groupId,
                                     UUID activityId,
                                     String activityName,
                                     String departureLocation,
                                     LocalDateTime departureTime,
                                     TransportMode transportMode,
                                     long availableSpots,
                                     int score,
                                     List<String> matchReasons) {
        this.groupId = groupId;
        this.activityId = activityId;
        this.activityName = activityName;
        this.departureLocation = departureLocation;
        this.departureTime = departureTime;
        this.transportMode = transportMode;
        this.availableSpots = availableSpots;
        this.score = score;
        this.matchReasons = List.copyOf(matchReasons);
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getDepartureLocation() {
        return departureLocation;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public long getAvailableSpots() {
        return availableSpots;
    }

    public int getScore() {
        return score;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }
}