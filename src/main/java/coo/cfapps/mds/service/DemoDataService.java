package coo.cfapps.mds.service;

import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.repository.DemoDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DemoDataService {

    private final DemoDataRepository demoDataRepository;


    public DemoDataService(DemoDataRepository demoDataRepository, ApplicationContext applicationContext) {
        this.demoDataRepository = demoDataRepository;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Iterable<DemoData> fetchDemoData(String username) {
        // Log and perform any other service-related logic
        log.info("Fetching data for user: {}", username);
        Iterable<DemoData> data = demoDataRepository.findAll();
        return data;
    }
}
