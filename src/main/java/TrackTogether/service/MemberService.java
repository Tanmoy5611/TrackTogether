package TrackTogether.service;

import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.exceptions.NotFoundException;
import TrackTogether.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final CurrentUserService currentUserService;

    @Autowired
    public MemberService(MemberRepository memberRepository,
                         CurrentUserService currentUserService) {
        this.memberRepository = memberRepository;
        this.currentUserService = currentUserService;
    }

    public Member findById(UUID id){
        return memberRepository.findById(id).orElseThrow();
    }

    public Optional<Member> findByOriginalId(String id){
        return memberRepository.findByOriginalId(id);
    }

    public Member findByOriginalIdNO(String id){
        return memberRepository.findByOriginalId(id).orElseThrow(() -> NotFoundException.foUserOriginalId(id));
    }

    public List<Member> findAll(){
        return memberRepository.findAll();
    }

    public void save(Member member){
        memberRepository.save(member);
    }

    // Update travel preferences for the currently logged-in member
    public Member updateCurrentUserTravelPreferences(TransportMode preferredTransportMode,
                                                     String defaultDepartureLocation,
                                                     Double defaultLatitude,
                                                     Double defaultLongitude) {
        // Always use the logged-in member so one user cannot edit another member
        Member member = currentUserService.getCurrentUser();
        return updateTravelPreferences(
                member,
                preferredTransportMode,
                defaultDepartureLocation,
                defaultLatitude,
                defaultLongitude
        );
    }

    // Update travel preferences for a specific member
    public Member updateTravelPreferences(Member member,
                                          TransportMode preferredTransportMode,
                                          String defaultDepartureLocation,
                                          Double defaultLatitude,
                                          Double defaultLongitude) {
        if (preferredTransportMode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferred transport mode is required");
        }

        String normalizedDepartureLocation = normalizeOptionalText(defaultDepartureLocation);
        if (normalizedDepartureLocation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default departure location is required");
        }

        validateCoordinates(defaultLatitude, defaultLongitude);

        member.setPreferredTransportMode(preferredTransportMode);
        member.setDefaultDepartureLocation(normalizedDepartureLocation);
        member.setDefaultLatitude(defaultLatitude);
        member.setDefaultLongitude(defaultLongitude);

        return memberRepository.save(member);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // Validate coordinates
    private static void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Default latitude and longitude must be provided together"
            );
        }

        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default latitude must be between -90 and 90");
        }

        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default longitude must be between -180 and 180");
        }
    }
}