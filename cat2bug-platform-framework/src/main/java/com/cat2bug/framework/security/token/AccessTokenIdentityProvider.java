package com.cat2bug.framework.security.token;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.security.AccessTokenResolver;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class AccessTokenIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    @Inject
    Instance<AccessTokenResolver> resolver;

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request, AuthenticationRequestContext context) {
        return context.runBlocking(() -> {
            String clientIp = (String) request.getAttribute(AccessTokenAuthMechanism.CLIENT_IP_ATTRIBUTE);
            String resource = (String) request.getAttribute(AccessTokenAuthMechanism.RESOURCE_ATTRIBUTE);
            AccessTokenResolver.Authenticated authenticated = resolver.isResolvable()
                    ? resolver.get().authenticate(request.getToken().getToken(), clientIp, resource)
                    : null;
            if (authenticated == null) {
                throw new AuthenticationFailedException();
            }
            LoginUser loginUser = authenticated.loginUser();
            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(loginUser.getUsername()))
                    .addCredential(request.getToken())
                    .addAttribute(AccessTokenIdentities.LOGIN_USER_ATTRIBUTE, loginUser)
                    .addAttribute(AccessTokenIdentities.PROJECT_ID_ATTRIBUTE, authenticated.projectId())
                    .build();
        });
    }
}
