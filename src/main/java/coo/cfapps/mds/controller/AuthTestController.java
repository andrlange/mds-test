package coo.cfapps.mds.controller;


import coo.cfapps.mds.entity.DemoData;
import coo.cfapps.mds.service.DemoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthTestController {

    private final DemoDataService demoDataService;

    public AuthTestController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }


    @GetMapping
    public String testAuth(Authentication authentication) {
        return "Hello "+authentication.getName()+" from authenticated user!";
    }

    @GetMapping("/demo")
    public Iterable<DemoData> testDemoData() {
        return demoDataService.fetchDemoData();

    }
}
