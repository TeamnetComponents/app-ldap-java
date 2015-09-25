package ro.teamnet.ldap.util;

import org.springframework.security.core.AuthenticationException;

/**
 * Created by Marian.Spoiala on 9/22/2015.
 */
public class UserAuthenticationFailedException extends AuthenticationException {

    public UserAuthenticationFailedException(String message) {
        super(message);
    }

    public UserAuthenticationFailedException(String message, Throwable t) {
        super(message, t);
    }
}
