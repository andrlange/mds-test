package coo.cfapps.mds.service;

import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.repository.DemoDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@Transactional
public class DemoDataService {

    private final DemoDataRepository demoDataRepository;


    public DemoDataService(DemoDataRepository demoDataRepository, ApplicationContext applicationContext) {
        this.demoDataRepository = demoDataRepository;

    }

    public Iterable<DemoData> fetchDemoData() {
        Iterable<DemoData> data = demoDataRepository.findAll();
        log.info("Data fetched from schema - user: {}", demoDataRepository.getSchema());
        return data;
    }
}
