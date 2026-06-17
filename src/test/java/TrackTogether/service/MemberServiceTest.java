package TrackTogether.service;

import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MemberService memberService;

    @Test
    void updateCurrentUserTravelPreferencesAllowsMissingDefaultDepartureLocation() {
        Member member = new Member();
        member.setUserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(memberRepository.save(member)).thenReturn(member);

        Member saved = memberService.updateCurrentUserTravelPreferences(
                TransportMode.PUBLIC_TRANSPORT,
                " ",
                null,
                null
        );

        assertThat(saved.getPreferredTransportMode()).isEqualTo(TransportMode.PUBLIC_TRANSPORT);
        assertThat(saved.getDefaultDepartureLocation()).isNull();
        assertThat(saved.getDefaultLatitude()).isNull();
        assertThat(saved.getDefaultLongitude()).isNull();
    }

    @Test
    void updateCurrentUserTravelPreferencesRejectsPartialCoordinates() {
        Member member = new Member();

        when(currentUserService.getCurrentUser()).thenReturn(member);

        assertThatThrownBy(() -> memberService.updateCurrentUserTravelPreferences(
                TransportMode.BIKE,
                "Groenplaats",
                51.2194,
                null
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Default latitude and longitude must be provided together");
    }
}