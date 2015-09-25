package ro.teamnet.ldap.service;

import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ldap.util.LDAPLoginFailedException;

import java.util.List;

/**
 * Created by Marian.Spoiala on 9/21/2015.
 */
public interface LDAPAccountService {

    public List<LDAPAccount> findAll();

    public LDAPAccount findByDn(String dn);

    public List<LDAPAccount> getByUserName(String userName);

    public void authenticateUser(String userName, String password) throws LDAPLoginFailedException;

    public LDAPAccount authenticateGetDetails(String userName, String password)  throws LDAPLoginFailedException;
}
