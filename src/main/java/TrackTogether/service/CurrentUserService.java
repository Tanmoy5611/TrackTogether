package TrackTogether.service;

import TrackTogether.domain.Member;
import TrackTogether.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final MemberRepository memberRepository;

    public CurrentUserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof OidcUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }

        String originalId = "GOOGLE-" + user.getSubject();

        return memberRepository.findByOriginalId(originalId)
                .orElseGet(() -> createMemberFromAuthenticatedUser(user, originalId));
    }

    private Member createMemberFromAuthenticatedUser(OidcUser user, String originalId) {
        Member member = new Member();
        member.setOriginalId(originalId);
        member.setEmail(user.getEmail());
        member.setName(user.getFullName());
        member.setStatus(true);

        return memberRepository.save(member);
    }
}