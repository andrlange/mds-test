package coo.cfapps.mds.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DynamicDataSourceRegistry {

    private final DefaultListableBeanFactory beanFactory;
    @Getter
    private final Map<String, String> registeredDataSources = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public DynamicDataSourceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

    /**
     * Registers a new DataSource bean dynamically
     *
     * @param beanName the name of the DataSource bean
     * @param url      the JDBC URL for the DataSource
     * @param username the username for the DataSource
     * @param password the password for the DataSource
     */
    public void registerDataSource(String beanName, String username, String password,String url, String driver) {
        if (beanFactory.containsBean(beanName)) {
            throw new IllegalArgumentException("A DataSource with bean name " + beanName + " already exists.");
        }

        // Define the DataSource bean properties
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(HikariDataSource.class);
        beanDefinitionBuilder.addPropertyValue("jdbcUrl", url);
        beanDefinitionBuilder.addPropertyValue("username", username);
        beanDefinitionBuilder.addPropertyValue("password", password);
        beanDefinitionBuilder.addPropertyValue("driverClassName", driver);

        // Register the DataSource bean
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        beanFactory.registerBeanDefinition(beanName, beanDefinition);

        // Keep track of registered beans
        registeredDataSources.put(beanName, url);
        log.info("Registered DataSource beans: {}", registeredDataSources);
    }

    /**
     * Deregisters an existing DataSource bean dynamically
     *
     * @param beanName the name of the DataSource bean to remove
     */
    public void deregisterDataSource(String beanName) {
        if (!beanFactory.containsBean(beanName)) {
            throw new IllegalArgumentException("No DataSource bean found with name " + beanName);
        }

        // Remove the DataSource bean
        beanFactory.removeBeanDefinition(beanName);

        // Remove from the tracking map
        registeredDataSources.remove(beanName);
    }

    /**
     * Check if a DataSource bean is registered
     *
     * @param beanName the name of the DataSource bean
     * @return true if the DataSource bean is registered, false otherwise
     */
    public boolean isDataSourceRegistered(String beanName) {
        return beanFactory.containsBean(beanName);
    }

    public void logDataSourceBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        log.info("Registered DataSource Beans:");
        // Filter beans starting with "DataSource"
        for (String beanName : beanNames) {
            if (beanName.startsWith("DataSource")) {
                Object bean = applicationContext.getBean(beanName);
                log.info("Bean Name: {}, Bean Type: {}", beanName, bean.getClass().getName());

                //DataSource myBean = (DataSource) bean;
                //JdbcTemplate myJdbc = new JdbcTemplate(myBean);
                //String sql = "SELECT field1,field2 FROM demo_data";
                //log.info("Query Result: {}", myJdbc.queryForList(sql));

            }
        }
    }
}
