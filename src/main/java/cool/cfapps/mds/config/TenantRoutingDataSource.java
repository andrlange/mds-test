package cool.cfapps.mds.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
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

    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private static final Map<Object, Object> lookup = new ConcurrentHashMap<>();

    TenantRoutingDataSource(DataSource defaultDataSource) {
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(lookup);
        setDataSourceLookup(new LookUp());
    }


    public static void addDataSource(String un, DataSource ds) {
        lookup.computeIfAbsent(un, k -> un);
        dataSources.computeIfAbsent(un, k -> ds);
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


}

