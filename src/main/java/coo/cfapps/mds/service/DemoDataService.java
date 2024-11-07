package coo.cfapps.mds.service;

import coo.cfapps.mds.config.SchemaAware;
import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.repository.DemoDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@Transactional
public class DemoDataService {

    private final DemoDataRepository demoDataRepository;
    private final ApplicationContext applicationContext;

    public DemoDataService(DemoDataRepository demoDataRepository, ApplicationContext applicationContext) {
        this.demoDataRepository = demoDataRepository;

        this.applicationContext = applicationContext;
    }

    @SchemaAware
    public Iterable<DemoData> fetchDemoData(String username) {
        // Log and perform any other service-related logic
        log.info("Fetching data for user: {}", username);


        AtomicInteger i = new AtomicInteger(0);
        log.info("---------- LIST ALL DATA SOURCES ----------");
        Arrays.stream(applicationContext.getBeanDefinitionNames()).toList().forEach(beanName -> {
            try{
                Object bean = applicationContext.getBean(beanName);
                if (bean instanceof DataSource) {
                    log.info("{}: DataSource({}) for Schema: {}",i.incrementAndGet(),beanName,
                            ((DataSource) bean).getConnection().getSchema());
                }

            }catch (Exception e) {}
        });

        log.info("------------------- END -------------------");

        try{ DataSource ds = applicationContext.getBean(DataSource.class);
            log.info("Current database schema: {}", ds.getConnection().getSchema());
        } catch (Exception e) {}

        Iterable<DemoData> data = demoDataRepository.findAll();
        log.info("Data fetched from schema - user: {}", demoDataRepository.getSchema());
        return data;
    }
}
