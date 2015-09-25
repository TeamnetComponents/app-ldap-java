package ro.teamnet.ldap.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Marian.Spoiala on 9/22/2015.
 */
public class PropertyUtil extends PropertyPlaceholderConfigurer {
    private static Map propertiesMap;

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
                                     Properties props) throws BeansException {
        super.processProperties(beanFactory, props);

        propertiesMap = new HashMap<String, String>();
        for (Object key : props.keySet()) {
            String keyStr = key.toString();
            propertiesMap.put(keyStr, parseStringValue(props.getProperty(keyStr),
                    props, new HashSet()));
        }
    }

    public static String getProperty(String name) {
        return propertiesMap.get(name).toString();
    }
}