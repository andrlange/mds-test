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
    private final String defaultUserName;

    @Value("${mds.security.database.allow-default-login:false}")
    private boolean allowDefaultLogin;
    @Value("${mds.security.roles.default-role:}")
    private String defaultRole;
    @Value("${mds.security.roles.role-table:}")
    private String roleTable;
    @Value("${mds.security.roles.role-column:}")
    private String roleColumn;
    @Value("${mds.security.roles.user-column:}")
    private String userColumn;


    public DbUserDetailsService(
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String defaultUserName,
            @Value("${spring.datasource.hikari.minimum-idle:1}") int minPoolSize,
            @Value("${spring.datasource.hikari.maximum-pool-size:2}") int maxPoolSize,
            RoutingDataSource routingDataSource) {
        this.driver = driver;
        this.url = url;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.routingDataSource = routingDataSource;
        this.defaultUserName = defaultUserName;
        this.defaultRole = defaultRole != null ? defaultRole : "NA";
        this.roleTable = roleTable != null ? roleTable : "";
        this.roleColumn = roleColumn != null ? roleColumn : "";
        this.userColumn = userColumn != null ? userColumn : "";
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

        if (!allowDefaultLogin && defaultUserName.equals(username)) {
            log.info("Default user login is not allowed");
            return null;
        }

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

        String role = defaultRole;
        // Check user role from db
        if (!roleTable.isEmpty() && !roleColumn.isEmpty()) {
            String sql = "SELECT " + roleColumn + "," + userColumn +
                         " FROM " + roleTable +
                         " WHERE " + userColumn + " = '" + username + "'";

            try {
                JdbcTemplate jdbc = new JdbcTemplate(RoutingDataSource.getDataSourceByKey(username));
                String result = jdbc.query(sql,
                        (rs, col) -> {
                            return rs.getString(1);
                        }).getFirst();
                if (result != null) {
                    role = result;
                }
                log.info("User role for this user: {}", role);

            } catch (Exception e) {
                log.info("Failed to gather role: {}", e.getMessage());
            }

        }

        log.info("Creating userDetails: {} - {}", username, role);

        return User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles(role.split(","))
                .build();

    }

    private boolean checkUserAgainstDb(String un, String pw) {

        boolean existingDataSource = false;
        DataSource dataSource;
        if (routingDataSource.hasDataSource(un)) {
            existingDataSource = true;
            dataSource = RoutingDataSource.getDataSourceByKey(un);
        } else {
            dataSource = createDataSource(un, pw);
        }


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

            if (!existingDataSource && result.get() == 1) {
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
