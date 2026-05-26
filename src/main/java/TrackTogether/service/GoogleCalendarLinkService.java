package TrackTogether.service;

import TrackTogether.domain.TravelGroup;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class GoogleCalendarLinkService {

    private static final String GOOGLE_CALENDAR_TEMPLATE_URL =
            "https://calendar.google.com/calendar/render?action=TEMPLATE";
    private static final DateTimeFormatter GOOGLE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public String buildTravelGroupCalendarUrl(TravelGroup group) {
        if (group == null || group.getDepartureTime() == null) {
            return null;
        }

        String activityName = group.getActivity() != null ? group.getActivity().getName() : "activity";
        LocalDateTime startsAt = group.getDepartureTime();
        // Uses the planned arrival when it exists, otherwise keeps a simple travel block
        LocalDateTime endsAt = group.getEstimatedArrivalTime() != null
                && group.getEstimatedArrivalTime().isAfter(startsAt)
                ? group.getEstimatedArrivalTime()
                : startsAt.plusMinutes(45);

        return buildCalendarUrl(
                "Travel group to " + activityName,
                startsAt,
                endsAt,
                group.getDepartureLocation() != null ? group.getDepartureLocation() : group.getLocation(),
                "Leave with your TrackTogether travel group for " + activityName
        );
    }

    private String buildCalendarUrl(String title,
                                    LocalDateTime startsAt,
                                    LocalDateTime endsAt,
                                    String location,
                                    String details) {
        // Google Calendar accepts this template URL without extra API permissions
        return GOOGLE_CALENDAR_TEMPLATE_URL
                + "&text=" + encode(defaultText(title, "TrackTogether event"))
                + "&dates=" + encode(format(startsAt) + "/" + format(endsAt))
                + "&location=" + encode(defaultText(location, ""))
                + "&details=" + encode(defaultText(details, "Added from TrackTogether"));
    }

    private static String format(LocalDateTime dateTime) {
        return GOOGLE_DATE_TIME_FORMATTER.format(dateTime);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}