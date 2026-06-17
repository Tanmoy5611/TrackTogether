package TrackTogether.security;

import TrackTogether.domain.Member;
import TrackTogether.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountStatusAccessFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;

    public AccountStatusAccessFilter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = requestPath(request);

        if (path.equals("/banned") && isCurrentUserActive()) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        if (isAllowedWhileBanned(request) || isCurrentUserActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (wantsJson(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account has been disabled.");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/banned");
    }

    private boolean isCurrentUserActive() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        if (!(authentication.getPrincipal() instanceof OidcUser user)) {
            return true;
        }

        String originalId = "GOOGLE-" + user.getSubject();

        return memberRepository.findByOriginalId(originalId)
                .map(Member::getStatus)
                .orElse(true);
    }

    private boolean isAllowedWhileBanned(HttpServletRequest request) {
        String path = requestPath(request);

        return path.equals("/banned")
                || path.equals("/logout")
                || path.equals("/login")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/");
    }

    private boolean wantsJson(HttpServletRequest request) {
        String path = requestPath(request);
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return path.startsWith("/api/")
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}