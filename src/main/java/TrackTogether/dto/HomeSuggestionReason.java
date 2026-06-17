package TrackTogether.dto;

import lombok.Getter;

// Enum used to avoid hardcoded suggestion reason strings in the service
@Getter
public enum HomeSuggestionReason {
    SAME_LOCATION("home.reason.SAME_LOCATION"),
    SAME_TIME("home.reason.SAME_TIME"),
    UPCOMING_THIS_WEEK("home.reason.UPCOMING_THIS_WEEK");

    private final String messageKey;

    HomeSuggestionReason(String messageKey) {
        this.messageKey = messageKey;
    }
}