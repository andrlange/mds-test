package coo.cfapps.mds.service;

import coo.cfapps.mds.config.SchemaAware;
import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.repository.DemoDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class DemoDataService {

    private final DemoDataRepository demoDataRepository;

    public DemoDataService(DemoDataRepository demoDataRepository) {
        this.demoDataRepository = demoDataRepository;

    }

    @SchemaAware
    public Iterable<DemoData> fetchDemoData(String username) {
        // Log and perform any other service-related logic
        log.info("Fetching data for user: {}", username);
        Iterable<DemoData> data = demoDataRepository.findAll();
        log.info("Data fetched from user and schema: {}", demoDataRepository.getSchema());
        return data;
    }
}
