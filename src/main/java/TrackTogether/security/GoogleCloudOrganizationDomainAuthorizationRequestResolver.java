package TrackTogether.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class GoogleCloudOrganizationDomainAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

    public GoogleCloudOrganizationDomainAuthorizationRequestResolver(
            ClientRegistrationRepository clients) {

        this.delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clients,
                        "/oauth2/authorization"
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                              String clientRegistrationId) {
        return customize(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request) {

        if (request == null) return null;

        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(params -> params.put("hd", "kdg.be"))
                .build();
    }
}