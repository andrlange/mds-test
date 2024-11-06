package coo.cfapps.mds.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class TenantRoutingConfig {

    public static class TenantAwareRoutingConfiguration extends AbstractRoutingDataSource {

        private static final Map<Object, Object> dbs = new ConcurrentHashMap<>();

        @Bean
        @Primary
        TenantAwareRoutingConfiguration allDataSources(ApplicationContext applicationContext) {
            DataSource defaultDataSource = (DataSource) applicationContext.getBean("DataSource_Default");
            return new TenantAwareRoutingConfiguration(new HashMap<>(), defaultDataSource);
        }


        public TenantAwareRoutingConfiguration(Map<String, DataSource> dataSourceMap, DataSource defaultDataSource) {
            dbs.putAll(dataSourceMap);
            this.setTargetDataSources(dbs);
            this.setDefaultTargetDataSource(defaultDataSource);
        }

        public void updateDataSources(Map<Object, Object> dataSources) {
            dbs.clear();
            dbs.putAll(dataSources);
            log.info("Updated DataSources: {}", dataSources.size());
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
}
