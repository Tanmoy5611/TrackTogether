package TrackTogether.security;

import TrackTogether.domain.Member;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

    private final SessionRegistry sessionRegistry;

    public UserSessionService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void expireSessionsFor(Member member) {
        if (member == null) {
            return;
        }

        expireSessionsForOriginalId(member.getOriginalId());
    }

    public void expireSessionsForOriginalId(String originalId) {
        if (originalId == null || originalId.isBlank()) {
            return;
        }

        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> originalId.equals(originalIdFor(principal)))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    private String originalIdFor(Object principal) {
        if (principal instanceof OidcUser oidcUser) {
            return "GOOGLE-" + oidcUser.getSubject();
        }

        return null;
    }
}