package ro.teamnet.ldap.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ro.teamnet.bootstrap.security.CustomBaseAuthenticationProvider;

/**
 * Created by Marian.Spoiala on 9/22/2015.
 */
@Component
public class LDAPAuthenticationProvider extends CustomBaseAuthenticationProvider {

    private LDAPUserDetailsService ldapUserDetailsService;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"), null);
        }
    }

    @Override
    protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails loadedUser;
        String typedPassword = authentication.getCredentials().toString();

        try {
            loadedUser = ldapUserDetailsService.loadUserDetails(username, typedPassword);
        } catch (UsernameNotFoundException notFound) {
            throw notFound;
        } catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }

    public void setLdapUserDetailsService(LDAPUserDetailsService ldapUserDetailsService) {
        this.ldapUserDetailsService = ldapUserDetailsService;
    }
}
