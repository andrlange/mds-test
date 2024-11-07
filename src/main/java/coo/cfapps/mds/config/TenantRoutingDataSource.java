package coo.cfapps.mds.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static final Map<Object, Object> dbs = new ConcurrentHashMap<>();

    TenantRoutingDataSource(ApplicationContext applicationContext) {
        DataSource defaultDataSource = (DataSource) applicationContext.getBean("DataSource_Default");
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(dbs);
    }

    public static void setCurrentUser(String username) {
        log.info("Setting current user: {}", username);
        currentUser.set(username);
    }

    public static void clearCurrentUser() {
        log.info("Removing current user: {}", currentUser.get());
        currentUser.remove();
    }


    public static void addDataSource(String key, DataSource dataSource) {
        log.info("Before Update DataSources: {}", dbs.size());
        try{log.info("Adding DataSources: {}:{}",key, dataSource.getConnection().getSchema());}catch (Exception e){}
        Map<Object, Object> newDs = Map.of(key, dataSource);
        dbs.computeIfAbsent(key, k -> newDs);
        log.info("After Updated DataSources: {}", dbs.size());
    }


    @Override
    protected Object determineCurrentLookupKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String key = "DataSource_"+authentication.getName();
            boolean r = dbs.get(key) != null;
            log.info("determineCurrentLookupKey: {} for 1 DataSource of {}: {}", key, dbs.size(), r);
            log.info("dbs beans: {}", dbs);
            return key;
        }
        return null;
    }


}

