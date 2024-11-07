# Multi Data Source Demo

This Demo shows how we can use Spring Security with Database Users only and Multi-Schema in a DB for authentication and 
routing all 
DB Queries (CrudRepository + Service) using the right DB user context.

## using PostgreSQL 17 and PG Admin 4 (Docker-Compose)

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

For PostgreSQL three users are create using three schemas:
- user_one : Password: password_one Schema: schema_user_one
- user_two : Password: password_two Schema: schema_user_two
- user_three : Password: password_three Schema: schema_user_three


alle schemas have the same table: demo_data
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
```
curl -u user_one:password_one http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data one"}]


curl -u user_two:password_two http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data two"}]

curl -u user_three:password_three http://localhost:8080/auth/demo
# returns: [{"id":1,"field1":"demo","field2":"data three"}]

```

## Explanation:

### Class "DbUserDetailsService"
Uses the basic authentication credentials to create a DataSource using this credentials.
If a "SELECT 1" is possible the DataSource is stored in the AbstractRoutingDataSource, so the SecurityContext will 
determine the corresponding DataSource for this user.

### Class "TenantRoutingDataSource"
This Class is marked as ```@Primary``` so the Router will determine the right DataSource for the given 
SecurityContext and user.

