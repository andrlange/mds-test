package cool.cfapps.mds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MultiDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiDataApplication.class, args);
    }

}
