package coo.cfapps.mds.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration("tenantRoutingConfig")
@Primary
@Slf4j
public class TenantRoutingConfig extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static final Map<Object, Object> dbs = new ConcurrentHashMap<>();

    TenantRoutingConfig(ApplicationContext applicationContext) {
        DataSource defaultDataSource = (DataSource) applicationContext.getBean("DataSource_Default");
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(dbs);
    }

    public static void setCurrentUser(String username) {
        currentUser.set(username);
    }

    public static void clearCurrentUser() {
        currentUser.remove();
    }


    public void addDataSource(String key, DataSource dataSource) {
        Map<Object, Object> newDs = Map.of(key, dataSource);
        dbs.computeIfAbsent(key, k -> newDs);//.putAll(dataSources);
        log.info("Updated DataSources: {}", dbs.size());
        setDefaultTargetDataSource(dbs);
    }


    @Override
    protected Object determineCurrentLookupKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String key = authentication.getName();
            boolean r = dbs.get(key) != null;
            dbs.forEach((k, v) -> log.info("DataSources for key: {}", k));
            log.info("determineCurrentLookupKey: {} for {} DataSources : {}", key, dbs.size(), r);
            //log.info("thread local key: {}",CURRENT_USER_NAME.get());
            return key;
        }
        return null;
    }
}

