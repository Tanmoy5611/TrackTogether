package TrackTogether.dto;

import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.TravelGroupActivityLog;
import TrackTogether.domain.TravelGroupActivityType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TravelGroupActivityLogView {

    private final String actorName;
    private final String actorInitial;
    private final String targetName;
    private final String messageKey;
    private final String iconClass;
    private final LocalDateTime createdAt;
    private final JoinRequest joinRequest;

    public TravelGroupActivityLogView(String actorName,
                                      String actorInitial,
                                      String targetName,
                                      String messageKey,
                                      String iconClass,
                                      LocalDateTime createdAt,
                                      JoinRequest joinRequest) {
        this.actorName = actorName;
        this.actorInitial = actorInitial;
        this.targetName = targetName;
        this.messageKey = messageKey;
        this.iconClass = iconClass;
        this.createdAt = createdAt;
        this.joinRequest = joinRequest;
    }

    public static TravelGroupActivityLogView from(TravelGroupActivityLog activityLog) {
        String actorName = activityLog.getActor() != null ? activityLog.getActor().getName() : null;
        String targetName = activityLog.getTargetMember() != null ? activityLog.getTargetMember().getName() : null;

        // The entity stays clean and this view decides how the log should look
        return new TravelGroupActivityLogView(
                actorName,
                initialFor(actorName),
                targetName,
                messageKeyFor(activityLog.getType()),
                iconFor(activityLog.getType()),
                activityLog.getCreatedAt(),
                activityLog.getJoinRequest()
        );
    }

    private static String initialFor(String value) {
        if (value == null || value.isBlank()) {
            return "?";
        }

        return value.trim().substring(0, 1);
    }

    private static String messageKeyFor(TravelGroupActivityType type) {
        return switch (type) {
            case CREATED -> "activityLog.CREATED";
            case JOINED -> "activityLog.JOINED";
            case LEFT -> "activityLog.LEFT";
            case UPDATED -> "activityLog.UPDATED";
            case OWNERSHIP_TRANSFERRED -> "activityLog.OWNERSHIP_TRANSFERRED";
            case JOIN_REQUESTED -> "activityLog.JOIN_REQUESTED";
            case JOIN_REQUEST_ACCEPTED -> "activityLog.JOIN_REQUEST_ACCEPTED";
            case JOIN_REQUEST_REJECTED -> "activityLog.JOIN_REQUEST_REJECTED";
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