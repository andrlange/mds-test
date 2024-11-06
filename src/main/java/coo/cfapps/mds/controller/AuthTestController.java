package coo.cfapps.mds.controller;


import coo.cfapps.mds.config.DynamicDataSourceRegistry;
import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.repository.DemoDataRepository;
import coo.cfapps.mds.service.DemoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthTestController {

    private final DynamicDataSourceRegistry dynamicDataSourceRegistry;
    private final DemoDataService demoDataService;

    public AuthTestController(DynamicDataSourceRegistry dynamicDataSourceRegistry, DemoDataService demoDataService) {
        this.dynamicDataSourceRegistry = dynamicDataSourceRegistry;
        this.demoDataService = demoDataService;
    }


    @GetMapping
    public String testAuth(Authentication authentication) {
        log.info("fetching beans for: {}",authentication.getName());
        dynamicDataSourceRegistry.logDataSourceBeans();
        return "Hello from authenticated user!";
    }

    @GetMapping("/demo")
    public Iterable<DemoData> testDemoData(Authentication authentication) {
        return demoDataService.fetchDemoData(authentication.getName());

    }
}
