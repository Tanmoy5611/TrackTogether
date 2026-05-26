package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TravelGroup;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarLinkServiceTest {

    private final GoogleCalendarLinkService googleCalendarLinkService = new GoogleCalendarLinkService();

    @Test
    void buildTravelGroupCalendarUrlUsesDepartureWindow() {
        Activity activity = new Activity();
        activity.setName("Football training");

        TravelGroup group = new TravelGroup(4, "Antwerp Central Station", null);
        group.setActivity(activity);
        group.setDepartureLocation("Antwerp Central Station");
        group.setDepartureTime(LocalDateTime.of(2026, 6, 1, 17, 30));
        group.setEstimatedArrivalTime(LocalDateTime.of(2026, 6, 1, 18, 10));

        String calendarUrl = googleCalendarLinkService.buildTravelGroupCalendarUrl(group);

        assertThat(calendarUrl).contains("calendar.google.com/calendar/render?action=TEMPLATE");
        assertThat(calendarUrl).contains("text=Travel+group+to+Football+training");
        assertThat(calendarUrl).contains("dates=20260601T173000%2F20260601T181000");
        assertThat(calendarUrl).contains("location=Antwerp+Central+Station");
    }
}