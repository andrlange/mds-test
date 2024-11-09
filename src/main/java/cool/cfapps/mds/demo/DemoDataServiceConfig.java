package cool.cfapps.mds.demo;

import cool.cfapps.mds.jdbc.JdbcDemoDataService;
import cool.cfapps.mds.jpa.JpaDemoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class DemoDataServiceConfig {

    @Bean
    @Profile("jpa")
    public DemoDataService jpaDataService(JpaDemoDataService service) {
        log.info("Configuring JPA data service");
        return service;
    }

    @Bean
    @Profile("jdbc")
    public DemoDataService jdbcDataService(JdbcDemoDataService service) {
        log.info("Configuring JDBC data service");
        return service;
    }
}
