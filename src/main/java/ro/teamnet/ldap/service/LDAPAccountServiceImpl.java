package ro.teamnet.ldap.service;

import org.springframework.stereotype.Service;
import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ldap.repository.LDAPAccountRepo;
import ro.teamnet.ldap.util.LDAPLoginFailedException;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by Marian.Spoiala on 9/21/2015.
 */
@Service
public class LDAPAccountServiceImpl implements LDAPAccountService {

    @Inject
    private LDAPAccountRepo ldapAccountRepo;

    public List<LDAPAccount> findAll() {
        return ldapAccountRepo.findAll();
    }

    public LDAPAccount findByDn(String dn) {
        return ldapAccountRepo.findByDn(dn);
    }

    public List<LDAPAccount> getByUserName(String userName) {
        return ldapAccountRepo.getByUserName(userName);
    }

    public void authenticateUser(String userName, String password) throws LDAPLoginFailedException {
        ldapAccountRepo.authenticateUser(userName, password);
    }

    public LDAPAccount authenticateGetDetails(String userName, String password) throws LDAPLoginFailedException {

        List<LDAPAccount> accountList;

        authenticateUser(userName, password);
        accountList = getByUserName(userName);

        if (accountList.size() == 1)
            return accountList.get(0);

        return null;
    }
}
