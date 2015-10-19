package ro.teamnet.ldap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.teamnet.bootstrap.domain.Account;
import ro.teamnet.bootstrap.service.AccountService;
import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ou.domain.jpa.AccountFunction;
import ro.teamnet.ou.domain.jpa.Organization;
import ro.teamnet.ou.mapper.AccountMapper;
import ro.teamnet.ou.service.AccountFunctionService;
import ro.teamnet.ou.service.FunctionService;
import ro.teamnet.ou.service.OrganizationAccountService;
import ro.teamnet.ou.service.OrganizationService;
import ro.teamnet.ou.web.rest.dto.AccountDTO;
import ro.teamnet.ou.web.rest.dto.FunctionDTO;
import ro.teamnet.ou.web.rest.dto.OrganizationDTO;
import ro.teamnet.ou.web.rest.dto.PerspectiveDTO;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Marian.Spoiala on 10/8/2015.
 */
@Service
@Transactional
public class LDAPSynchronizationService {

    private final Logger log = LoggerFactory.getLogger(LDAPSynchronizationService.class);

    @Inject
    LDAPAccountService ldapAccountService;

    @Inject
    AccountService accountService;

    @Inject
    FunctionService functionService;

    @Inject
    OrganizationService organizationService;

    @Inject
    OrganizationAccountService organizationAccountService;

    @Inject
    AccountFunctionService accountFunctionService;

    public FunctionDTO createFunction(String code) {
        FunctionDTO functionDTO = new FunctionDTO();
        functionDTO.setActive(true);
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        try {
            functionDTO.setValidFrom(ft.parse("2000-10-01"));
            functionDTO.setValidTo(ft.parse("2020-10-01"));
        } catch (ParseException e) {
            log.debug("Error parsing data for FunctionDTO!");
            return null;
        }
        functionDTO.setCode(code);
        functionDTO.setDescription(code);
        functionDTO = functionService.save(functionDTO);

        return functionDTO;
    }

    private void createOrganization() {

    }

    /**
     * Function that synchronizes LDAP accounts to application accounts.
     * PostConstruct should only be uncommented if the synchronization is needed.
     */
    //@PostConstruct
    private void synchronizeAccounts() {
        List<LDAPAccount> ldapAccounts = ldapAccountService.findAll();

        for (LDAPAccount ldapAccount : ldapAccounts) {
            if (ldapAccount.getUsername() != null && ldapAccount.getGroup() != null
                    && (ldapAccount.getGroup().contains("_Admins") || ldapAccount.getGroup().contains("_ReportUsers"))) {
                Account existingAccount = accountService.findByLogin(ldapAccount.getUsername().toLowerCase());

                if (existingAccount == null && ldapAccount.getUsername() != null) {
                    existingAccount = accountService.createUserInfoNoPassword(ldapAccount.getUsername().toLowerCase(),
                            ldapAccount.getFirstName(), ldapAccount.getLastName(), ldapAccount.getEmail(), null, null, true);
                }

                if (existingAccount != null) {
                    int groupNameSize = ldapAccount.getGroup().length();
                    String groupName = ldapAccount.getGroup();
                    String organizationCode = groupName.split("CN=")[1].split("_")[0];

                    OrganizationDTO organizationDTO = organizationService.findByCode(organizationCode);
                    if (organizationDTO == null || organizationDTO.getId() == null) {
                        organizationDTO = new OrganizationDTO();
                        organizationDTO.setCode(organizationCode);
                        organizationDTO.setActive(true);
                        organizationDTO.setDescription(organizationCode);
                        organizationDTO = organizationService.save(organizationDTO);
                    }

                    PerspectiveDTO perspectiveDTO = new PerspectiveDTO();

                    Set<Organization> accountOrganizations = organizationAccountService.getOrgsByAccountId(existingAccount.getId());
                    Boolean exists = false;
                    for (Organization accountOrganization : accountOrganizations) {
                        if (accountOrganization.getId().equals(organizationDTO.getId())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        Set<AccountDTO> accountDTOs = new HashSet<>();
                        accountDTOs.add(AccountMapper.toDTO(existingAccount));
                        organizationAccountService.createOrUpdateOrgAccounts(organizationDTO.getId(), accountDTOs);
                    }

                    FunctionDTO functionDTO = null;
                    if (groupName.toLowerCase().contains("admin")) {
                        functionDTO = functionService.findOneByCode("INSTITUTION_ADMIN");
                        if (functionDTO == null) {
                            functionDTO = createFunction("INSTITUTION_ADMIN");
                        }
                    } else if (groupName.toLowerCase().contains("user")) {
                        functionDTO = functionService.findOneByCode("INSTITUTION_USER");
                        if (functionDTO == null) {
                            functionDTO = createFunction("INSTITUTION_USER");
                        }
                    }

                    AccountFunction accountFunction = accountFunctionService.findByAccountIdAndFunctionId(existingAccount.getId(), functionDTO.getId());

                    if (accountFunction == null && functionDTO != null && functionDTO.getId() != null) {
                        functionService.addToAccount(existingAccount.getId(), functionDTO);
                    }
                }
            }
        }
    }
}
