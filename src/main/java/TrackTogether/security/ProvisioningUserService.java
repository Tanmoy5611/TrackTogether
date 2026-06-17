package TrackTogether.security;

import TrackTogether.domain.Member;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.service.AdminService;
import TrackTogether.service.MemberService;
import TrackTogether.service.SuperAdminService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ProvisioningUserService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final MemberService memberService;
    private final AdminService adminService;
    private final SuperAdminService superAdminService;
    private final ModeratorRepository moderatorRepository;

    public ProvisioningUserService(MemberService memberService,
                                   AdminService adminService,
                                   SuperAdminService superAdminService,
                                   ModeratorRepository moderatorRepository) {
        this.delegate = new OidcUserService();
        this.memberService = memberService;
        this.adminService = adminService;
        this.superAdminService = superAdminService;
        this.moderatorRepository = moderatorRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {

        OidcUser user = delegate.loadUser(userRequest);

        String googleId = user.getSubject();
        String originalId = "GOOGLE-" + googleId;

        String email = user.getEmail();
        String name = user.getFullName();

        if (email == null || !email.contains("@")) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_email"),
                    "Invalid email: " + email
            );
        }

        Member member = memberService.findByOriginalId(originalId)
                .orElseGet(() -> {
                    Member newMember = new Member();
                    newMember.setOriginalId(originalId);
                    newMember.setStatus(true);
                    return newMember;
                });

        member.setEmail(email);
        member.setName(name);

        memberService.save(member);

        boolean isAdmin = adminService.existsByUserId(member.getUserId());
        boolean isSuperAdmin = superAdminService.existsByUserId(member.getUserId());
        boolean isModerator = moderatorRepository.existsByUserId(member.getUserId());

        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (isModerator || isAdmin || isSuperAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MODERATOR"));
        }

        if (isAdmin || isSuperAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        if (isSuperAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        return new DefaultOidcUser(
                authorities,
                user.getIdToken(),
                user.getUserInfo()
        );
    }
}