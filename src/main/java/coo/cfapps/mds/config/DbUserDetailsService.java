package coo.cfapps.mds.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class DbUserDetailsService extends InMemoryUserDetailsManager {

    private final DynamicDataSourceRegistry dynamicDataSourceRegistry;
    private final String driver;
    private final String url;
    private final ApplicationContext applicationContext;

    public DbUserDetailsService(ApplicationContext applicationContext,
                                DynamicDataSourceRegistry dynamicDataSourceRegistry,
                                @Value("${spring.datasource.driver-class-name}") String driver,
                                @Value("${spring.datasource.url}") String url,
                                @Value("${spring.datasource.username}") String username,
                                @Value("${spring.datasource.password}") String password) {
        this.applicationContext = applicationContext;
        this.dynamicDataSourceRegistry = dynamicDataSourceRegistry;
        createUser(createUserDetails(username, password));
        dynamicDataSourceRegistry.registerDataSource("DataSource_Default", username, password, url, driver);
        this.driver = driver;
        this.url = url;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String auth = request.getHeader("Authorization");
        String password = "";
        if (auth != null && !auth.isEmpty()) {
            log.info("Authorization header: {}", auth);
            if (auth.toLowerCase().startsWith("basic ")) {
                String encodedUserPassword =
                        new String(Base64.getDecoder().decode(auth.substring("Basic ".length()).trim()), StandardCharsets.UTF_8);
                if (encodedUserPassword.contains(":")) password = encodedUserPassword.split(":")[1];
            }
        }

        log.info("try to get user: {} : {}", username, password);

        try {
            UserDetails checkUser = super.loadUserByUsername(username);
            if (checkUser != null) {
                log.info("User found in map: {}", checkUser);
                return checkUser;
            }
        } catch (UsernameNotFoundException e) {
            log.info("User not found in memory: {}", username);
        }


        if (checkUserAgainstDb(username, password)) {
            log.info("User found in database: {}", username);
            createUser(createUserDetails(username, password));

            return super.loadUserByUsername(username);
        }
        log.info("User not found in database: {}", username);
        return null;
    }

    private UserDetails createUserDetails(String username, String password) {


        UserDetails ud = User.builder()
                .username(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();

        log.info("Created userDetails: {}", ud);
        return ud;
    }

    private boolean checkUserAgainstDb(String un, String pw) {

        DataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driver)
                .username(un)
                .password(pw)
                .url(url)
                .build();

        log.info("Checking user against database: {}:{} - {} - {}", un, pw, url, driver);


        AtomicInteger result = new AtomicInteger(0);
        try {
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            jdbc.query("SELECT 1 AS RESULT",
                    (rs, col) -> {
                        int i = rs.getInt("RESULT");
                        result.set(i);
                        return i;
                    });

            log.info("Schema for this user: {} is {}",un,ds.getConnection().getSchema());

            if (result.get() == 1) {
                dynamicDataSourceRegistry.registerDataSource("DataSource_" + un,un,
                        pw, url, driver);

                DataSource addThis = (DataSource) applicationContext.getBean("DataSource_" + un);
                TenantRoutingConfig.addDataSource("DataSource_"+un,addThis);


            }
            ds.getConnection().close();
        } catch (Exception e) {
            log.info("Failed to capture connection: {}", e.getMessage());
        }

        log.info("result: {}", result);

        return result.get() == 1;
    }

}
