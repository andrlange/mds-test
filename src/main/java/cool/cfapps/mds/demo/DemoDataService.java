package cool.cfapps.mds.demo;

import org.springframework.context.annotation.Primary;

import java.util.List;


public interface DemoDataService {

    List<DemoData> fetchDemoData();
    String type();
}
