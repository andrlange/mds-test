package cool.cfapps.mds.jdbc;

import cool.cfapps.mds.demo.DemoData;
import cool.cfapps.mds.demo.DemoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Service
@Profile("jdbc")
@Slf4j
@Transactional
@Primary
public class JdbcDemoDataService implements DemoDataService {

    public static final String type="JDBC";

    private final JdbcDemoDataRepository repository;

    public JdbcDemoDataService(JdbcDemoDataRepository demoDataRepository) {
        log.info("JDBC JpaDemoDataService started.");
        this.repository = demoDataRepository;

    }

    @Override
    public List<DemoData> fetchDemoData() {
        Iterator<JdbcDemoData> data =repository.findAll().iterator();
        List<DemoData> list = new ArrayList<>();
        while (data.hasNext()) {
            list.add(data.next());
        }
        return  list;
    }

    @Override
    public String type() {
        return type;
    }
}
