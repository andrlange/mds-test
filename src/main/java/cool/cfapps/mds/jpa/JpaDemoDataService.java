package cool.cfapps.mds.jpa;

import cool.cfapps.mds.demo.DemoData;
import cool.cfapps.mds.demo.DemoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("jpa")
@Slf4j
@Primary
public class JpaDemoDataService implements DemoDataService {

    public static final String type="JPA";

    private final JpaDemoDataRepository repository;

    public JpaDemoDataService(JpaDemoDataRepository repository) {
        this.repository = repository;
        log.info("JPA JpaDemoDataService started.");
    }

    @Override
    public List<DemoData> fetchDemoData() {
        return new ArrayList<>(repository.findAll());
    }

    @Override
    public String type() {
        return type;
    }
}
