package TrackTogether.dto;

import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.TravelGroupActivityLog;
import TrackTogether.domain.TravelGroupActivityType;

import java.time.LocalDateTime;

public class TravelGroupActivityLogView {

    private final String actorName;
    private final String actorInitial;
    private final String message;
    private final String iconClass;
    private final LocalDateTime createdAt;
    private final JoinRequest joinRequest;

    public TravelGroupActivityLogView(String actorName,
                                      String actorInitial,
                                      String message,
                                      String iconClass,
                                      LocalDateTime createdAt,
                                      JoinRequest joinRequest) {
        this.actorName = actorName;
        this.actorInitial = actorInitial;
        this.message = message;
        this.iconClass = iconClass;
        this.createdAt = createdAt;
        this.joinRequest = joinRequest;
    }

    public static TravelGroupActivityLogView from(TravelGroupActivityLog activityLog) {
        String actorName = activityLog.getActor() != null ? activityLog.getActor().getName() : "Someone";
        String targetName = activityLog.getTargetMember() != null ? activityLog.getTargetMember().getName() : "another member";

        // The entity stays clean and this view decides how the log should look
        return new TravelGroupActivityLogView(
                actorName,
                initialFor(actorName),
                messageFor(activityLog.getType(), actorName, targetName),
                iconFor(activityLog.getType()),
                activityLog.getCreatedAt(),
                activityLog.getJoinRequest()
        );
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorInitial() {
        return actorInitial;
    }

    public String getMessage() {
        return message;
    }

    public String getIconClass() {
        return iconClass;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public JoinRequest getJoinRequest() {
        return joinRequest;
    }

    private static String initialFor(String value) {
        if (value == null || value.isBlank()) {
            return "?";
        }

        return value.trim().substring(0, 1);
    }

    private static String messageFor(TravelGroupActivityType type, String actorName, String targetName) {
        return switch (type) {
            case CREATED -> actorName + " created this travel group";
            case JOINED -> actorName + " joined this travel group";
            case LEFT -> actorName + " left this travel group";
            case UPDATED -> actorName + " updated the travel group info";
            case OWNERSHIP_TRANSFERRED -> actorName + " transferred ownership to " + targetName;
            case JOIN_REQUESTED -> actorName + " requested to join";
            case JOIN_REQUEST_ACCEPTED -> actorName + " accepted " + targetName + "'s join request";
            case JOIN_REQUEST_REJECTED -> actorName + " declined " + targetName + "'s join request";
        };
    }

    private static String iconFor(TravelGroupActivityType type) {
        return switch (type) {
            case JOIN_REQUESTED -> "bi-person-plus";
            case JOINED -> "bi-person-check";
            case LEFT -> "bi-box-arrow-right";
            case UPDATED -> "bi-pencil-square";
            case OWNERSHIP_TRANSFERRED -> "bi-stars";
            case JOIN_REQUEST_ACCEPTED -> "bi-check2-circle";
            case JOIN_REQUEST_REJECTED -> "bi-x-circle";
            case CREATED -> "bi-clock-history";
        };
    }
}