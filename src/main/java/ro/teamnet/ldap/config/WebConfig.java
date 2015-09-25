package ro.teamnet.ldap.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import ro.teamnet.ldap.util.PropertyUtil;

/**
 * Created by Marian.Spoiala on 9/20/2015.
 */
@Configuration
@ComponentScan({"ro.teamnet.ldap"})
@PropertySource("classpath:ldap.properties")
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    Environment env;

    @Bean
    public LdapContextSource contextSource () {
        LdapContextSource contextSource= new LdapContextSource();
        contextSource.setUrl(env.getRequiredProperty("ldap.url"));
        contextSource.setBase(env.getRequiredProperty("ldap.base"));
        contextSource.setUserDn(env.getRequiredProperty("ldap.user"));
        contextSource.setPassword(env.getRequiredProperty("ldap.password"));
        contextSource.setReferral(env.getRequiredProperty("ldap.referral"));
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource());
    }

    @Bean
    public static PropertyUtil propertyUtil() {
        PropertyUtil propertyUtil = new PropertyUtil();
        propertyUtil.setLocations(new Resource[] {new ClassPathResource("ldap.properties")});
        return propertyUtil;
    }
}