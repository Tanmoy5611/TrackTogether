package TrackTogether.webapi;

import TrackTogether.domain.Member;
import TrackTogether.service.MemberService;
import TrackTogether.webapi.dto.MemberTravelPreferenceDto;
import TrackTogether.webapi.dto.MemberTravelPreferenceRequestDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {

    private final MemberService memberService;

    public MemberApiController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.findAll();
    }

    @GetMapping("/{originalId}")
    public Member getMemberByOriginalId(@PathVariable String originalId) {
        return memberService.findByOriginalIdNO(originalId);
    }

    @PutMapping("/me/travel-preferences")
    public MemberTravelPreferenceDto updateCurrentMemberTravelPreferences(
            @Valid @RequestBody MemberTravelPreferenceRequestDto request) {
        Member member = memberService.updateCurrentUserTravelPreferences(
                request.getPreferredTransportMode(),
                request.getDefaultDepartureLocation(),
                request.getDefaultLatitude(),
                request.getDefaultLongitude()
        );

        return MemberTravelPreferenceDto.from(member);
    }
}