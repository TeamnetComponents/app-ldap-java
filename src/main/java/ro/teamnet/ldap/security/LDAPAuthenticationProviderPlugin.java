package ro.teamnet.ldap.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Service;
import ro.teamnet.bootstrap.plugin.security.AuthProviderType;
import ro.teamnet.bootstrap.plugin.security.AuthenticationProviderPlugin;
import ro.teamnet.bootstrap.security.CustomUserDetailsDecoratorService;

import javax.inject.Inject;

/**
 * Created by Marian.Spoiala on 9/22/2015.
 */
@Service
public class LDAPAuthenticationProviderPlugin implements AuthenticationProviderPlugin {

    @Inject
    @Qualifier(value = "ldapUserDetailsServiceImpl")
    private LDAPUserDetailsService ldapUserDetailsService;

    @Inject
    @Qualifier("customUserDetailsDecoratorService")
    private CustomUserDetailsDecoratorService customUserDetailsDecoratorService;

    @Override
    public boolean supports(AuthProviderType authProviderType) {
        return authProviderType == AuthProviderType.AUTH_OTHER;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        LDAPAuthenticationProvider ldapAuthenticationProvider = new LDAPAuthenticationProvider();
        ldapAuthenticationProvider.setLdapUserDetailsService(ldapUserDetailsService);
        ldapAuthenticationProvider.setCustomUserDetailsDecoratorService(customUserDetailsDecoratorService);
        return ldapAuthenticationProvider;
    }
}
