package TrackTogether.security;

import TrackTogether.domain.Member;
import TrackTogether.repository.MemberRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountStatusAccessFilterTest {

    private static final String SUBJECT = "123";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AccountStatusAccessFilter filter = new AccountStatusAccessFilter(memberRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsInactiveUsersAwayFromApplicationPages() throws ServletException, IOException {
        authenticateInactiveUser();
        MockHttpServletResponse response = doFilter("/travelgroups", "text/html");

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/banned");
    }

    @Test
    void allowsInactiveUsersToSeeBannedPage() throws ServletException, IOException {
        authenticateInactiveUser();
        MockHttpServletResponse response = doFilter("/banned", "text/html");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void redirectsActiveUsersAwayFromBannedPage() throws ServletException, IOException {
        authenticateUser(true);
        MockHttpServletResponse response = doFilter("/banned", "text/html");

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void returnsForbiddenForInactiveApiRequests() throws ServletException, IOException {
        authenticateInactiveUser();
        MockHttpServletResponse response = doFilter("/api/travelgroups", "application/json");

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsActiveUsersThrough() throws ServletException, IOException {
        authenticateUser(true);
        MockHttpServletResponse response = doFilter("/travelgroups", "text/html");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void authenticateInactiveUser() {
        authenticateUser(false);
    }

    private void authenticateUser(boolean active) {
        Member member = new Member();
        member.setStatus(active);
        when(memberRepository.findByOriginalId("GOOGLE-" + SUBJECT)).thenReturn(Optional.of(member));

        OidcIdToken token = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("sub", SUBJECT)
        );
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(), token);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(oidcUser, "", List.of())
        );
    }

    private MockHttpServletResponse doFilter(String path,
                                             String accept) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader("Accept", accept);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}