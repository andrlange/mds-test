# Multi Data Source Demo

This Demo shows how we can use Spring Security with Database Users only and Multi-Schema in a DB for authentication and 
routing all 
DB Queries (CrudRepository + Service) using the right DB user context.

## using PostgreSQL 17 and PG Admin 4 (Docker-Compose)

All relevant data and configs from PostgreSQL and PGAdmin are exposed to the local File System ```/vols```

### Based on Spring Boot 3.3.5

- Spring Boot Starter JDBC
- Spring Boot Starter Security
- PostgreSQL
- Lombok
- Spring Boot Starter Web

### Start docker instances:

```bash
# this will create a new folder vols to expose all PostgreSQL and Admin data
#
# init-database.sh and serevrs.json are mapped to the containers localfs
docker compose up -d
```

For PostgreSQL three users are created using three schemas:
- user_one : Password: password_one Schema: schema_user_one
- user_two : Password: password_two Schema: schema_user_two
- user_three : Password: password_three Schema: schema_user_three


all schemas have the same table: demo_data
 demo_data:
- id
- field1
- field2

Data:
- in schema_one -> 1,"demo","data one"
- in schema_two -> 1,"demo","data two"
- in schema_three -> 1,"demo","data three"


### running the service 

```bash
mvn spring-boot:run
```

## Testing
```bash
curl -u user_one:password_one http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data one"}]
```
```bash
curl -u user_two:password_two http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data two"}]
```
```bash
curl -u user_three:password_three http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data three"}]
```

## Explanation:

### Class "DbUserDetailsService"
Uses the basic authentication credentials to create a DataSource using this credentials.
If a "SELECT 1" is possible the DataSource is stored in the AbstractRoutingDataSource, so the SecurityContext will 
determine the corresponding DataSource for this user.

```Java
@Configuration
@Slf4j
public class DbUserDetailsService extends InMemoryUserDetailsManager {

    private final String driver;
    private final String url;
    private final String defaultUsername;
    private final String defaultPassword;
    private final int minPoolSize;
    private final int maxPoolSize;

    /// inject config from application properties
    public DbUserDetailsService(
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.hikari.minimum-idle}") int minPoolSize,
            @Value("${spring.datasource.hikari.maximum-pool-size}") int maxPoolSize) {
        createUser(createUserDetails(username, password));
        this.driver = driver;
        this.url = url;
        this.defaultUsername = username;
        this.defaultPassword = password;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    /// we will define one default DataSource 
    @Bean
    public DataSource defaultDataSource() {
        HikariDataSource defaultDs = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driver)
                .url(url)
                .username(defaultUsername)
                .password(defaultPassword)
                .build();

        defaultDs.setMaximumPoolSize(maxPoolSize);
        defaultDs.setMinimumIdle(minPoolSize);

        return new LazyConnectionDataSourceProxy(defaultDs);
    }
    
    /// InMemoryUserDetails Implementation
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String auth = request.getHeader("Authorization");
        String password = "";
        if (auth != null && !auth.isEmpty()) {
            /// Decode the basic auth -> base64
            if (auth.toLowerCase().startsWith("basic ")) {
                String encodedUserPassword =
                        new String(Base64.getDecoder().decode(auth.substring("Basic ".length()).trim()), StandardCharsets.UTF_8);
                if (encodedUserPassword.contains(":")) password = encodedUserPassword.split(":")[1];
            }
        }

        /// check if we already have this user in memory UserDetails
        try {
            UserDetails checkUser = super.loadUserByUsername(username);
            if (checkUser != null) {
                return checkUser;
            }
        } catch (UsernameNotFoundException e) {
            log.info("User not found in memory: {}", username);
        }

        if (checkUserAgainstDb(username, password)) {
            createUser(createUserDetails(username, password));
            return super.loadUserByUsername(username);
        }
        return null;
    }

    /// create a new InMemoryUserDetails Object
    private UserDetails createUserDetails(String username, String password) {
        UserDetails ud = User.builder()
                .username(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();
        return ud;
    }

private boolean checkUserAgainstDb(String un, String pw) {

    HikariDataSource ds = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .driverClassName(driver)
            .username(un)
            .password(pw)
            .url(url)
            .build();

    /// we can minimize the amount of connections for each user
    ds.setMaximumPoolSize(maxPoolSize);
    ds.setMinimumIdle(minPoolSize);
    /// here we use a Lazy Proxy for the DataSource so connections are done on first usage
    LazyConnectionDataSourceProxy lcpDs = new LazyConnectionDataSourceProxy(ds);

    /// Here we check if we are able to anything on the database with the credentials given by http basic auth
    AtomicInteger result = new AtomicInteger(0);
    try {
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.query("SELECT 1 AS RESULT",
                (rs, col) -> {
                    int i = rs.getInt("RESULT");
                    result.set(i);
                    return i;
                });

        if (result.get() == 1) {
            TenantRoutingDataSource.addDataSource(un, lcpDs);
        }
    } catch (Exception e) {
        log.info("Failed to capture connection: {}", e.getMessage());
    }
    return result.get() == 1;
}
```

### Class "TenantRoutingDataSource"
This Class is marked as ```@Primary``` so the Router will determine the right DataSource for the given 
SecurityContext and user, and it is marked as the Primary DataSource Bean.

```Java
@Component
@Primary
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    // Keeps the DataSources and Keys for lookup
    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private static final Map<Object, Object> lookup = new ConcurrentHashMap<>();

    TenantRoutingDataSource(DataSource defaultDataSource) {
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(lookup);
        setDataSourceLookup(new LookUp());
    }
    
    public static void addDataSource(String un, DataSource ds) {
        lookup.computeIfAbsent(un, k -> un);
        dataSources.computeIfAbsent(un, k -> ds);
    }
    
    /// here we determine the right key based on the authentication (Security Context)
    @Override
    protected Object determineCurrentLookupKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            /// in the case we added a new DataSource and key we need to update the ResolvedDataSources using 
            /// initialize() called implicit from afterPropertiesSet()
            if (getResolvedDataSources().size() != dataSources.size()) afterPropertiesSet();
            return  authentication.getName();
        }
        return null;
    }

    /// our own implementation of a DataSourceLookup. Mapping the username to the right DataSource
    private static class LookUp implements DataSourceLookup {
        @Override
        public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
            return dataSources.get(dataSourceName);
        }
    }
}
```

## Consideration

### Closing Sessions
This demo does not reflect Spring Security or other Session Management. 
On Closing Sessions there also should be considered to:
- add DataSource removal from "TenantRoutingDataSource"
- removing the user from "DbUserDetailsService" InMemoryUserDetails provider

### Updating User Credentials e.g. Password
On Updating User Credentials like Passwords we should consider to:
- Update credentials in DB first
- Replace the DataSource with an updated one, so the Connection is working against the right credentials
- Updating the UserDetails in the InMemoryUserDetails Provider so the credentials are reflecting the new DB User in 
  our Spring Security User Details
- (optional) updating other Session details if necessary


happy coding - Andreas Lange