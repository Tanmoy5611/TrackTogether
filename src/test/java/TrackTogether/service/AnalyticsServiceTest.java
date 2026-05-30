package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.dto.analytics.AdminAnalyticsView;
import TrackTogether.dto.analytics.UserAnalyticsView;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.support.StaticMessageSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private TravelGroupRepository travelGroupRepository;

    @Mock
    private TravelGroupMemberRepository travelGroupMemberRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                activityRepository,
                travelGroupRepository,
                travelGroupMemberRepository,
                messageSource()
        );
    }

    @Test
    void adminAnalyticsCalculatesBaselineActualSavingsAndBreakdowns() {
        Activity workshop = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "Workshop", 10.0);
        Activity meetup = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", "Meetup", 10.0);

        TravelGroup carpoolGroup = group(workshop, TransportMode.CARPOOL);
        TravelGroup bikeGroup = group(meetup, TransportMode.BIKE);

        List<TravelGroupMember> memberships = List.of(
                membership(carpoolGroup),
                membership(carpoolGroup),
                membership(carpoolGroup),
                membership(bikeGroup),
                membership(bikeGroup)
        );

        when(travelGroupRepository.findAll()).thenReturn(List.of(carpoolGroup, bikeGroup));
        when(travelGroupMemberRepository.findAll()).thenReturn(memberships);
        when(travelGroupMemberRepository.countMembersByGroupIn(List.of(carpoolGroup, bikeGroup))).thenReturn(List.<Object[]>of(
                countRow(carpoolGroup, 3),
                countRow(bikeGroup, 2)
        ));

        AdminAnalyticsView dashboard = analyticsService.getAdminAnalytics();

        assertThat(dashboard.totalEvents()).isEqualTo(2);
        assertThat(dashboard.totalTravelGroups()).isEqualTo(2);
        assertThat(dashboard.totalParticipants()).isEqualTo(5);
        assertThat(dashboard.averageGroupSize()).isEqualTo(2.5);
        assertThat(dashboard.totalBaselineEmissionsKg()).isEqualTo(6.0);
        assertThat(dashboard.totalActualEmissionsKg()).isEqualTo(1.2);
        assertThat(dashboard.totalSavingsKg()).isEqualTo(4.8);
        assertThat(dashboard.savingsPercentage()).isEqualTo(80.0);
        assertThat(dashboard.transportModeBreakdown())
                .extracting("transportMode", "memberCount")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("Carpool", 3L),
                        org.assertj.core.groups.Tuple.tuple("Bike", 2L)
                );
    }

    @Test
    void userAnalyticsCalculatesPersonalSavingsFromJoinedGroups() {
        Member member = new Member();
        Activity activity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3", "Hackathon", 12.0);
        TravelGroup carpoolGroup = group(activity, TransportMode.CARPOOL);
        TravelGroupMember membership = new TravelGroupMember(carpoolGroup, member, null);

        when(activityRepository.countByCreator(member)).thenReturn(2L);
        when(travelGroupMemberRepository.findAllByMember(member)).thenReturn(List.of(membership));
        when(travelGroupMemberRepository.countMembersByGroupIn(List.of(carpoolGroup))).thenReturn(List.<Object[]>of(
                countRow(carpoolGroup, 3)
        ));

        UserAnalyticsView analytics = analyticsService.getUserAnalytics(member);

        assertThat(analytics.totalTripsCreated()).isEqualTo(2);
        assertThat(analytics.totalJoinedTrips()).isEqualTo(1);
        assertThat(analytics.personalBaselineEmissionsKg()).isEqualTo(1.44);
        assertThat(analytics.personalActualEmissionsKg()).isEqualTo(0.48);
        assertThat(analytics.personalCo2SavingsKg()).isEqualTo(0.96);
        assertThat(analytics.personalSavingsPercentage()).isEqualTo(66.67);
    }

    private static Activity activity(String id, String name, double distanceKm) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", UUID.fromString(id));
        activity.setName(name);
        activity.setDate(LocalDate.of(2026, 6, 1));
        activity.setTime(LocalTime.of(18, 0));
        activity.setDistanceKm(distanceKm);
        return activity;
    }

    private static TravelGroup group(Activity activity, TransportMode mode) {
        TravelGroup group = new TravelGroup(4, "Antwerp Central Station", mode);
        ReflectionTestUtils.setField(group, "groupId", UUID.randomUUID());
        group.setActivity(activity);
        return group;
    }

    private static Object[] countRow(TravelGroup group, long count) {
        return new Object[]{group.getGroupId(), count};
    }

    private static TravelGroupMember membership(TravelGroup group) {
        return new TravelGroupMember(group, new Member(), null);
    }

    private static StaticMessageSource messageSource() {
        StaticMessageSource messageSource = new StaticMessageSource();
        addMessage(messageSource, "transportMode.car", "Car");
        addMessage(messageSource, "transportMode.carpool", "Carpool");
        addMessage(messageSource, "transportMode.bike", "Bike");
        addMessage(messageSource, "transportMode.walk", "Walk");
        addMessage(messageSource, "transportMode.publicTransport", "Public transport");
        addMessage(messageSource, "analytics.time.morning", "Morning");
        addMessage(messageSource, "analytics.time.daytime", "Daytime");
        addMessage(messageSource, "analytics.time.evening", "Evening");
        addMessage(messageSource, "analytics.time.night", "Night");
        return messageSource;
    }

    private static void addMessage(StaticMessageSource messageSource, String code, String value) {
        messageSource.addMessage(code, Locale.ENGLISH, value);
        messageSource.addMessage(code, Locale.US, value);
    }
}