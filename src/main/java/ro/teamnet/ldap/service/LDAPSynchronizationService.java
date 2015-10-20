package ro.teamnet.ldap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.teamnet.bootstrap.domain.Account;
import ro.teamnet.bootstrap.service.AccountService;
import ro.teamnet.ldap.domain.LDAPAccount;
import ro.teamnet.ldap.util.PropertyUtil;
import ro.teamnet.ou.domain.jpa.AccountFunction;
import ro.teamnet.ou.domain.jpa.Organization;
import ro.teamnet.ou.mapper.AccountMapper;
import ro.teamnet.ou.service.*;
import ro.teamnet.ou.web.rest.dto.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Inject
    PerspectiveService perspectiveService;

    @Inject
    private OUAccountService ouAccountService;

    @Inject
    PropertyUtil propertyUtil;

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

    private OrganizationDTO createOrganization(String organizationCode) {
        OrganizationDTO organizationDTO = organizationService.findByCode(organizationCode);
        if (organizationDTO == null || organizationDTO.getId() == null) {
            organizationDTO = new OrganizationDTO();
            organizationDTO.setCode(organizationCode);
            organizationDTO.setActive(true);
            organizationDTO.setDescription(organizationCode);
            organizationDTO = organizationService.save(organizationDTO);
        }

        return organizationDTO;
    }

    private OrganizationalUnitDTO createOrganizationalUnit(OrganizationDTO organizationDTO) {
        Set<PerspectiveDTO> perspectiveDTOs = perspectiveService.findPerspectivesByOrganizationId(organizationDTO.getId());
        PerspectiveDTO perspectiveDTO = null;
        OrganizationalUnitDTO organizationalUnitDTO = null;
        if (perspectiveDTOs == null || perspectiveDTOs.size() == 0) {
            perspectiveDTO = new PerspectiveDTO();
            perspectiveDTO.setCode(organizationDTO.getCode() + "_Perspective_1");
            perspectiveDTO.setDescription(organizationDTO.getCode() + "_Perspective_1");
            perspectiveDTO.setOrganization(organizationDTO);
            perspectiveDTO = perspectiveService.save(perspectiveDTO);
            organizationalUnitDTO = perspectiveDTO.getOuTreeRoot();
        } else {
            perspectiveDTO = perspectiveDTOs.iterator().next();
            organizationalUnitDTO = perspectiveDTO.getOuTreeRoot();
        }

        return organizationalUnitDTO;
    }

    /**
     * Checks if ldap.properties file contains the ldap.synchronize=true property.
     */
    @PostConstruct
    private void ifSyncAccountsAllowed() {
        try {
            if (propertyUtil != null && propertyUtil.getProperty("ldap.synchronize").equals("true")) {
                synchronizeAccounts();
            }
        } catch (NullPointerException e) {
            log.debug("Ldap synchronize property does not exist in the configuration file! Users will not be synchronized!");
        }
    }

    /**
     * Method that synchronizes LDAP accounts to application accounts.
     * Creates accounts, organizations, perspectives, root organizational units, functions.
     * Adds functions to accounts, functions to organizational unit and accounts to organizational units.
     */
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

                    //Create organization, perspective and organizational unit root if they don't exist
                    OrganizationDTO organizationDTO = createOrganization(organizationCode);

                    OrganizationalUnitDTO organizationalUnitDTO = createOrganizationalUnit(organizationDTO);

                    //Add account to the organization
                    Set<Organization> accountOrganizations = organizationAccountService.getOrgsByAccountId(existingAccount.getId());
                    Boolean exists = false;
                    for (Organization accountOrganization : accountOrganizations) {
                        if (accountOrganization.getId().equals(organizationDTO.getId())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        Set<AccountDTO> accountDTOs = organizationAccountService.getAccountsByOrgId(organizationDTO.getId());
                        //Stim ca nu exista contul in organizatie datorita verificarii anterioare
                        accountDTOs.add(AccountMapper.toDTO(existingAccount));
                        organizationAccountService.createOrUpdateOrgAccounts(organizationDTO.getId(), accountDTOs);
                    }

                    //If it doesn't exist, create function INSTITUTION_ADMIN or INSTITUTION_USER depending on user group
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

                    //Adaugare functie la organizational unit. Verificarea pt duplicat se face in functionService
                    functionService.addToOrganizationalUnit(organizationalUnitDTO.getId(), functionDTO);

                    //Adaugare functie la cont
                    AccountFunction accountFunction = accountFunctionService.findByAccountIdAndFunctionId(existingAccount.getId(), functionDTO.getId());

                    if (accountFunction == null && functionDTO != null && functionDTO.getId() != null) {
                        functionService.addToAccount(existingAccount.getId(), functionDTO);
                    }

                    //Adaugare account la organizational unit
                    ArrayList<AccountDTO> accounts = new ArrayList<>(ouAccountService.getAccountsInOrganizationalUnit(organizationalUnitDTO.getId()));
                    Boolean accExists = false;
                    for (AccountDTO accountDTO : accounts) {
                        if (accountDTO.getId() == existingAccount.getId()) {
                            accExists = true;
                            break;
                        }
                    }
                    if (!accExists) {
                        AccountDTO finalAccount = AccountMapper.toDTO(existingAccount);
                        Set<FunctionDTO> functionDTOs = functionService.findAllByAccountId(finalAccount.getId());
                        finalAccount.setFunctions(functionDTOs);
                        accounts.add(finalAccount);
                        ouAccountService.createOrUpdateOUAccountRelationships(organizationalUnitDTO.getId(), accounts);
                    }
                }
            }
        }
    }
}
