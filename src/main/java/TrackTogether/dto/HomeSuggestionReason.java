package TrackTogether.dto;

// Enum used to avoid hardcoded suggestion reason strings in the service
public enum HomeSuggestionReason {
    SAME_LOCATION("Same location"),
    SAME_TIME("Same time"),
    UPCOMING_THIS_WEEK("Upcoming this week");

    private final String label;

    HomeSuggestionReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
