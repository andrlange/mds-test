package cool.cfapps.mds.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final HikariDataSource defaultDataSource;

    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private static final Map<String, DataSource> removedDataSources = new ConcurrentHashMap<>();
    private static final Map<Object, Object> lookup = new ConcurrentHashMap<>();

    TenantRoutingDataSource(@Value("${spring.datasource.driver-class-name}") String driver,
                            @Value("${spring.datasource.url}") String url,
                            @Value("${spring.datasource.username}") String username,
                            @Value("${spring.datasource.password}") String password,
                            @Value("${spring.datasource.hikari.minimum-idle}") int minPoolSize,
                            @Value("${spring.datasource.hikari.maximum-pool-size}") int maxPoolSize) {


        defaultDataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driver)
                .url(url)
                .username(username)
                .password(password)
                .build();

        defaultDataSource.setMaximumPoolSize(maxPoolSize);
        defaultDataSource.setMinimumIdle(minPoolSize);

        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(lookup);
        setDataSourceLookup(new LookUp());
    }

    @Bean
    public DataSource defaultDataSource() {
        return defaultDataSource;
    }

    public void replaceDataSource(String key, DataSource ds) {
        DataSource old = dataSources.get(key);
        if (old != null) {
            log.info("DataSource scheduled for removal: {}", key);
            removedDataSources.put(key, old);
        }

        dataSources.replace(key, ds);
        afterPropertiesSet();
    }

    public void removeDataSource(String key) {
        DataSource old = dataSources.get(key);
        if (old != null) {
            log.info("DataSource scheduled for removal: {}", key);
            removedDataSources.put(key, old);
        }
        dataSources.remove(key);
        afterPropertiesSet();
    }



    public static DataSource getDataSourceByKey(String key) {
        return dataSources.get(key);
    }


    public void addDataSource(String un, DataSource ds) {
        lookup.computeIfAbsent(un, k -> un);
        dataSources.computeIfAbsent(un, k -> ds);
        afterPropertiesSet();
    }


    @Override
    protected Object determineCurrentLookupKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (getResolvedDataSources().size() != dataSources.size()) afterPropertiesSet();

            log.info("resolvedDataSources:{}", getResolvedDataSources().size());
            String key = authentication.getName();
            boolean r = lookup.get(key) != null;
            log.info("determineCurrentLookupKey: {} for 1 DataSource of {}: {}", key, lookup.size(), r);
            log.info("DataSources available: {}", lookup);
            return key;
        }
        return null;
    }


    private static class LookUp implements DataSourceLookup {
        @Override
        public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
            log.info("Looking up DataSource: {}", dataSourceName);
            return dataSources.get(dataSourceName);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Destroying dataSources");
        dataSources.forEach((k, v) -> {
            try {
                shutdownGracefully((HikariDataSource) v);
            } catch (Exception e) {
                log.error("Error closing DataSource: {}", k, e);
            } finally {
                dataSources.clear();
                lookup.clear();
                log.info("DataSources destroyed");
            }
        });
    }

    private static void shutdownGracefully(HikariDataSource hikariDataSource) {
        try {
            log.info("Starting graceful shutdown of HikariCP pool...");

            // 1. Get current active connections count
            int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
            log.info("Active connections before shutdown: {}", activeConnections);

            // 2. Wait a bit for active queries to complete (optional)
            if (activeConnections > 0) {
                log.info("Waiting for active connections to complete...");
                Thread.sleep(500); // Adjust this timeout as needed
            }
            // 3. Finally close the datasource
            hikariDataSource.close();
            log.info("HikariCP pool shutdown completed successfully");

        } catch (Exception e) {
            log.error("Error during HikariCP shutdown", e);
            throw new RuntimeException("Failed to shutdown HikariCP properly", e);
        }
    }

    @Scheduled(fixedRate = 5000) // 5 minutes
    private void checkRemovedDataSources() {
        removedDataSources.entrySet().removeIf(entry -> {
            log.info("Checking removed DataSource: {}", entry.getKey());
            DataSource old = entry.getValue();
            try {
                shutdownGracefully((HikariDataSource) old);
                return true;
            } catch (Exception e) {
                log.error("Error closing removed DataSource: {}", entry.getKey(), e);
                return false;
            }
        });
    }
}

