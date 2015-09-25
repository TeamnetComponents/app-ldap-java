package ro.teamnet.ldap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.teamnet.bootstrap.domain.Account;
import ro.teamnet.bootstrap.domain.RoleBase;
import ro.teamnet.bootstrap.repository.AccountRepository;
import ro.teamnet.bootstrap.service.AccountService;
import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ldap.service.LDAPAccountService;
import ro.teamnet.ldap.util.LDAPLoginFailedException;
import ro.teamnet.ldap.util.UserAuthenticationFailedException;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Marian.Spoiala on 9/23/2015.
 */
@Service("ldapUserDetailsServiceImpl")
@Order(0)
@Transactional
public class LDAPUserDetailsServiceImpl implements LDAPUserDetailsService {


    private final Logger log = LoggerFactory.getLogger(LDAPUserDetailsServiceImpl.class);

    @Inject
    LDAPAccountService ldapAccountService;

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountService accountService;

    private Account storeUserDB(LDAPAccount ldapAccount) {
        Account account = accountService.createUserInformation(ldapAccount.getUsername().toLowerCase(), ldapAccount.getPassword(), ldapAccount.getFirstName(), ldapAccount.getLastName(),
                ldapAccount.getEmail(), null, null);

        System.out.println(account);
        account.setActivated(true);
        return accountRepository.save(account);
    }

    private Set<GrantedAuthority> loadGrantedAuthorities(LDAPAccount ldapAccount) {

        String userName = ldapAccount.getUsername().toLowerCase();

        Account accountFromDB = accountRepository.findAllByLogin(userName);
        if (accountFromDB == null) {
            accountFromDB = storeUserDB(ldapAccount);

            if (accountFromDB == null) {
                throw new UserAuthenticationFailedException("New user cannot be saved in database!");
            }
        }

        if (!accountFromDB.isActivated()) {
            throw new UserAuthenticationFailedException("User " + userName + " was not activated!");
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        grantedAuthorities.addAll(accountFromDB.getRoles());
        grantedAuthorities.addAll(accountFromDB.getModuleRights());
        for (RoleBase applicationRole : accountFromDB.getRoles()) {
            grantedAuthorities.addAll(applicationRole.getModuleRights());
        }
        grantedAuthorities.addAll(accountFromDB.getModuleRights());

        return grantedAuthorities;
    }

    @Override
    public UserDetails loadUserDetails(String userName, String password)  throws AuthenticationException {

        log.debug("Authenticating via LDAP {}", userName);
        String lowercaseUserName = userName.toLowerCase();

        LDAPAccount ldapAccount = null;

        try {
            ldapAccount = ldapAccountService.authenticateGetDetails(userName, password);
        } catch (LDAPLoginFailedException e) {
            throw new UsernameNotFoundException("What");
        }

        if (ldapAccount == null) {
            throw new UsernameNotFoundException("User" + lowercaseUserName + " was not found in LDAP!");
        }

        ldapAccount.setPassword(password);
        Set<GrantedAuthority> grantedAuthorities = loadGrantedAuthorities(ldapAccount);

        return new User(lowercaseUserName, password, grantedAuthorities);
    }
}
