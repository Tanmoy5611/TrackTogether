package TrackTogether.controller;

import TrackTogether.domain.Member;
import TrackTogether.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CurrentUserService currentUserService;
    private final long inactivityLogoutTimeoutMillis;
    private final LocaleResolver localeResolver;

    public GlobalControllerAdvice(
            CurrentUserService currentUserService,
            @Value("${tracktogether.security.inactivity-logout-timeout-millis:1800000}") long inactivityLogoutTimeoutMillis,
            LocaleResolver localeResolver
    ) {
        this.currentUserService = currentUserService;
        this.inactivityLogoutTimeoutMillis = inactivityLogoutTimeoutMillis;
        this.localeResolver = localeResolver;
    }

    @ModelAttribute("currentUser")
    public Member addCurrentUserToModel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof OidcUser)) {
            return null;
        }

        try {
            return currentUserService.getCurrentUser();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @ModelAttribute("showDashboardNav")
    public boolean showDashboardNav() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    @ModelAttribute("inactivityLogoutTimeoutMillis")
    public long inactivityLogoutTimeoutMillis() {
        return inactivityLogoutTimeoutMillis;
    }

    @ModelAttribute("currentLanguage")
    public String currentLanguage(HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        return locale.getLanguage();
    }

    @ModelAttribute("currentRequestUri")
    public String currentRequestUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}