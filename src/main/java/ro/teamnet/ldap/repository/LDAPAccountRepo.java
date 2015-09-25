package ro.teamnet.ldap.repository;

import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.stereotype.Repository;
import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ldap.util.LDAPLoginFailedException;
import ro.teamnet.ldap.util.PropertyUtil;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

/**
 * Created by Marian.Spoiala on 9/21/2015.
 */
@Repository
public class LDAPAccountRepo {

    private final int PAGE_SIZE = 800;

    @Inject
    PropertyUtil propertyUtil;

    @Inject
    private LdapTemplate ldapTemplate;

    public class AccountAttributesMapper implements AttributesMapper<LDAPAccount> {
        public LDAPAccount mapFromAttributes(Attributes attrs) throws NamingException {
            LDAPAccount account = new LDAPAccount();

            try {
                account.setUsername((String) attrs.get(propertyUtil.getProperty("ldap.userName")).get());
                account.setLastName((String) attrs.get((propertyUtil.getProperty("ldap.lastName"))).get());
                account.setFirstName((String) attrs.get((propertyUtil.getProperty("ldap.firstName"))).get());
                account.setEmail((String) attrs.get((propertyUtil.getProperty("ldap.mail"))).get());
            } catch (NullPointerException e) {
                account.setUsername(null);
            }
            return account;
        }
    }

    public LDAPAccount findByDn(String dn) {
        return ldapTemplate.lookup(dn, new AccountAttributesMapper());
    }



    public List<LDAPAccount> findAll() {

        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(PAGE_SIZE, null);
        List<LDAPAccount> finalAccountsList = new ArrayList();

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person"));

        do {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            List<LDAPAccount> accountsList = ldapTemplate.search("", filter.toString(), controls, new AccountAttributesMapper(), processor);
            finalAccountsList.addAll(accountsList);
            processor = new PagedResultsDirContextProcessor(PAGE_SIZE, processor.getCookie());
        } while (processor.getCookie().getCookie() != null);

        return finalAccountsList;
    }

    public List<LDAPAccount> findAll(PagedResultsCookie pagedResultsCookie) {
        return null;
    }

    public List<LDAPAccount> getByUserName(String userName) {

        LdapQuery query = query()
                .where("objectclass").is("person")
                .and(propertyUtil.getProperty("ldap.userName")).is(userName);

        return ldapTemplate.search(query, new AccountAttributesMapper());
    }

    public void authenticateUser(String userName, String password) throws LDAPLoginFailedException {

        /*AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person"));
        filter.and(new EqualsFilter(propertyUtil.getProperty("ldap.userName"), userName));

        return ldapTemplate.authenticate("", filter.toString(), password);*/

        LdapQuery query = query()
                .where("objectclass").is("person")
                .and(propertyUtil.getProperty("ldap.userName")).is(userName);

        try {
            ldapTemplate.authenticate(query, password);
        }
        catch(Exception e) {
            throw new LDAPLoginFailedException("LDAP Login failed with error: ", e.getCause());
        }
    }
}
