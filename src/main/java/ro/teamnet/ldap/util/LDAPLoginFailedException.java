package ro.teamnet.ldap.util;

import org.springframework.security.core.AuthenticationException;

/**
 * Created by Marian.Spoiala on 9/22/2015.
 */
public class LDAPLoginFailedException extends AuthenticationException {

    public LDAPLoginFailedException(String message) {
        super(message);
    }

    public LDAPLoginFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
