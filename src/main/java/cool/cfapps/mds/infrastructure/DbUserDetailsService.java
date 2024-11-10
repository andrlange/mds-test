package cool.cfapps.mds.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class DbUserDetailsService extends InMemoryUserDetailsManager {

    private final String driver;
    private final String url;

    private final int minPoolSize;
    private final int maxPoolSize;

    private final RoutingDataSource routingDataSource;


    public DbUserDetailsService(
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.hikari.minimum-idle}") int minPoolSize,
            @Value("${spring.datasource.hikari.maximum-pool-size}") int maxPoolSize,
            RoutingDataSource routingDataSource) {
        this.driver = driver;
        this.url = url;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.routingDataSource = routingDataSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
                .password(passwordEncoder().encode(password))
                .roles("USER")
                .build();

        log.info("Created userDetails: {}", ud);
        return ud;
    }

    private boolean checkUserAgainstDb(String un, String pw) {


        DataSource dataSource = createDataSource(un, pw);

        log.info("Checking user against database: {}:{} - {} - {}", un, pw, url, driver);


        AtomicInteger result = new AtomicInteger(0);
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.query("SELECT 1 AS RESULT",
                    (rs, col) -> {
                        int i = rs.getInt("RESULT");
                        result.set(i);
                        return i;
                    });

            log.info("Schema for this user: {} is {}", un, dataSource.getConnection().getSchema());

            if (result.get() == 1) {
                routingDataSource.addDataSource(un, dataSource);
            }
        } catch (Exception e) {
            log.info("Failed to capture connection: {}", e.getMessage());
        }

        return result.get() == 1;
    }


    @Transactional
    public boolean changeUserPassword(String username, String oldPassword, String newPassword) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated and username matches
        if (authentication == null || !authentication.getName().equals(username)) {
            log.info("Not authorized to change this user's password for user: {}", username);
        } else {
            log.info("Authorized to change this user's password for user: {}", username);
        }

        // Get user details
        UserDetails existingUser = loadUserByUsername(username);

        // Verify old password
        if (!passwordEncoder().matches(oldPassword, existingUser.getPassword())) {
            log.info("Old password does not match for user: {}", username);
            return false;
        }

        if (newPassword.length() < 8) {
            log.info("New password is too short for user: {}", username);
            return false;
        }

        JdbcTemplate jdbc = new JdbcTemplate(RoutingDataSource.getDataSourceByKey(username));
        String sql = "ALTER ROLE " + username + " WITH PASSWORD '" + newPassword + "'; ";

        log.info("Updating user password for user in database: {}", username);
        jdbc.execute(sql);
        routingDataSource.replaceDataSource(username, createDataSource(username, newPassword));

        log.info("Updating user password for user in UserDetailsService: {}", username);
        updateUser(createUserDetails(username, newPassword));

        return true;
    }


    private DataSource createDataSource(String username, String password) {
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driver)
                .username(username)
                .password(password)
                .url(url)
                .build();

        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minPoolSize);

        return ds;
    }

    //@Transactional
    public void logOut(String username) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated and username matches
        if (authentication == null || !authentication.getName().equals(username)) {
            log.info("Not authorized to logOff this user: {}", username);
        } else {
            log.info("LogOff user: {}", username);
        }

        routingDataSource.removeDataSource(username);
        deleteUser(username);
        log.info("Removed user from UserDetailsService and DataSource: {}", username);
    }




}
