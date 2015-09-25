package ro.teamnet.ldap.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by Marian.Spoiala on 9/23/2015.
 */
public interface LDAPUserDetailsService {

    public UserDetails loadUserDetails(String userName, String password);
}
