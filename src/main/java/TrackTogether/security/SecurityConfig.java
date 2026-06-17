package TrackTogether.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ProvisioningUserService userService,
            AccountStatusAccessFilter accountStatusAccessFilter,
            GoogleCloudOrganizationDomainAuthorizationRequestResolver resolver,
            SessionRegistry sessionRegistry
    ) {

        return http
                .oauth2Login(oauth2 ->
                        oauth2.userInfoEndpoint(u -> u.oidcUserService(userService))
                                .authorizationEndpoint(authz ->
                                        authz.authorizationRequestResolver(resolver)
                                )
                                .defaultSuccessUrl("/", true)
                )
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                                .requestMatchers("/moderator/**", "/api/moderators/**").hasRole("MODERATOR")
                                .anyRequest().authenticated()
                )
                .addFilterAfter(accountStatusAccessFilter, AnonymousAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .expiredUrl("/oauth2/authorization/google")
                        .sessionRegistry(sessionRegistry)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/oauth2/authorization/google")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .build();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}