package TrackTogether.dto;

import TrackTogether.domain.Activity;

// Small DTO representing one suggested activity and why it was suggested
public class HomeSuggestionView {

    private final Activity activity;
    private final HomeSuggestionReason reason;

    public HomeSuggestionView(Activity activity, HomeSuggestionReason reason) {
        this.activity = activity;
        this.reason = reason;
    }

    public Activity getActivity() {
        return activity;
    }

    public HomeSuggestionReason getReason() {
        return reason;
    }
}
