
## How to run MySQL in a docker container

```
docker run --name=mysql01 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:latest
docker exec -it mysql01 mysql -uroot -p

ALTER USER 'root'@'%' IDENTIFIED BY 'password';
```

## Integration test with Testcontainers

```java

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DbAutoReconnectApplicationTests {
    @Test
    void contextLoads() throws InterruptedException {
        log.info("Starting tests on port {}.", port);

        log.info("Saving two rows in the database...");
        repo.save(new DbEntity("first"));
        repo.save(new DbEntity("second"));
        log.info(" ... done: {}", repo.findAll());

        assertThat(isHealthy(), is(true));
        assertThat(getEntityIdsFromDb(), hasSize(2));

        pause(db);

        assertThat(isHealthy(), is(false));
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        IntStream.range(1, 20).parallel().forEach(
                i -> assertThat(getEntityIdsFromDb(), is(nullValue()))
        );
        assertThat(isHealthy(), is(false));

        unpause(db);

        assertThat(isHealthy(), is(true));
        assertThat(getEntityIdsFromDb(), hasSize(2));
        assertThat(isHealthy(), is(true));
    }
    ...

    @SneakyThrows
    private boolean isHealthy() {
        var healthEntity = template.getForEntity("http://localhost:{port}/actuator/health", StatusWrapper.class, port);
        log.info("Health check http status is {} and status is {}", healthEntity.getStatusCode(), healthEntity.getBody().getStatus());
        if (healthEntity.getBody().getComponent() != null) {
            log.info(" ... and database details: {}", healthEntity.getBody().getComponent().getDb().getDetails());
        }
        return healthEntity.getStatusCode().is2xxSuccessful();
    }

    @SneakyThrows
    private List<String> getEntityIdsFromDb() {
        var idsEntity = template.getForEntity("http://localhost:{port}/entities", String.class, port);
        log.info("Query for database entities has status {}", idsEntity.getStatusCode());
        log.info(" ... and entity body: {}", idsEntity.getBody());
        TypeReference<List<String>> listOfStringType = new TypeReference<>() {
        };
        return idsEntity.getStatusCode().is2xxSuccessful() ? objectMapper.readValue(idsEntity.getBody(), listOfStringType) : null;
    }
}
```

### Test output:

```
.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v2.4.2)

2021-02-23 13:31:23.732  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Setting up the datasource using url jdbc:postgresql://192.168.99.100:32825/test?loggerLevel=OFF.
2021-02-23 13:31:23.745  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Starting DbAutoReconnectApplicationTests using Java 15.0.1 on Omen with PID 18016 (started by Alberto in C:\dev\java-workspace\db-auto-reconnect)
2021-02-23 13:31:23.747  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : No active profile set, falling back to default profiles: default
2021-02-23 13:31:24.254  INFO 18016 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2021-02-23 13:31:24.292  INFO 18016 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 31 ms. Found 1 JPA repository interfaces.
2021-02-23 13:31:25.261  INFO 18016 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 0 (http)
2021-02-23 13:31:25.272  INFO 18016 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-02-23 13:31:25.273  INFO 18016 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.41]
2021-02-23 13:31:25.421  INFO 18016 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-02-23 13:31:25.421  INFO 18016 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1654 ms
2021-02-23 13:31:25.852  WARN 18016 --- [           main] com.zaxxer.hikari.HikariConfig           : HikariPool-1 - idleTimeout has been set but has no effect because the pool is operating as a fixed size pool.
2021-02-23 13:31:25.853  INFO 18016 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2021-02-23 13:31:25.953  INFO 18016 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2021-02-23 13:31:26.304  INFO 18016 --- [           main] liquibase.lockservice                    : Successfully acquired change log lock
2021-02-23 13:31:26.599  INFO 18016 --- [           main] liquibase.changelog                      : Creating database history table with name: public.databasechangelog
2021-02-23 13:31:26.605  INFO 18016 --- [           main] liquibase.changelog                      : Reading from public.databasechangelog
2021-02-23 13:31:26.669  INFO 18016 --- [           main] liquibase.changelog                      : Table SIMPLE_TABLE created
2021-02-23 13:31:26.670  INFO 18016 --- [           main] liquibase.changelog                      : ChangeSet db.changelog-1.0-xml::init-db::Alberto ran successfully in 7ms
2021-02-23 13:31:26.676  INFO 18016 --- [           main] liquibase.lockservice                    : Successfully released change log lock
2021-02-23 13:31:26.797  INFO 18016 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2021-02-23 13:31:26.859  INFO 18016 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 5.4.27.Final
2021-02-23 13:31:26.983  INFO 18016 --- [           main] o.hibernate.annotations.common.Version   : HCANN000001: Hibernate Commons Annotations {5.1.2.Final}
2021-02-23 13:31:27.089  INFO 18016 --- [           main] org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.PostgreSQL10Dialect
2021-02-23 13:31:27.676  INFO 18016 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2021-02-23 13:31:27.684  INFO 18016 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2021-02-23 13:31:28.168  INFO 18016 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2021-02-23 13:31:28.597  INFO 18016 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2021-02-23 13:31:28.692  INFO 18016 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 30859 (http) with context path ''
2021-02-23 13:31:28.704  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Started DbAutoReconnectApplicationTests in 5.295 seconds (JVM running for 10.124)

... Actual test starts here ...

2021-02-23 13:31:28.910  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Starting tests on port 30859.
2021-02-23 13:31:28.910  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Saving two rows in the database...
2021-02-23 13:31:29.085  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... done: [DbEntity[id='first'], DbEntity[id='second']]
2021-02-23 13:31:29.172  INFO 18016 --- [o-auto-1-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2021-02-23 13:31:29.172  INFO 18016 --- [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2021-02-23 13:31:29.173  INFO 18016 --- [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2021-02-23 13:31:29.242  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Health check http status is 200 OK and status is UP
2021-02-23 13:31:29.249  INFO 18016 --- [o-auto-1-exec-2] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:29.255  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 200 OK
2021-02-23 13:31:29.255  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: ["first","second"]
2021-02-23 13:31:29.259  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Pausing container postgres:11.1 ...
2021-02-23 13:31:31.345  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... paused.
2021-02-23 13:31:42.349  WARN 18016 --- [o-auto-1-exec-3] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@1dcaa173 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2021-02-23 13:31:42.353  WARN 18016 --- [o-auto-1-exec-3] o.s.b.a.jdbc.DataSourceHealthIndicator   : DataSource health check failed

org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection; nested exception is java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 11001ms.
at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(DataSourceUtils.java:82) ~[spring-jdbc-5.3.3.jar:5.3.3]
... (omitted)
at java.base/java.lang.Thread.run(Thread.java:832) ~[na:na]
Caused by: java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 11001ms.
at com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:695) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:197) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:162) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:128) ~[HikariCP-3.4.5.jar:na]
at org.springframework.jdbc.datasource.DataSourceUtils.fetchConnection(DataSourceUtils.java:158) ~[spring-jdbc-5.3.3.jar:5.3.3]
at org.springframework.jdbc.datasource.DataSourceUtils.doGetConnection(DataSourceUtils.java:116) ~[spring-jdbc-5.3.3.jar:5.3.3]
at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(DataSourceUtils.java:79) ~[spring-jdbc-5.3.3.jar:5.3.3]
... 78 common frames omitted
Caused by: org.postgresql.util.PSQLException: This connection has been closed.
at org.postgresql.jdbc.PgConnection.checkClosed(PgConnection.java:783) ~[postgresql-42.2.5.jar:42.2.5]
at org.postgresql.jdbc.PgConnection.setNetworkTimeout(PgConnection.java:1556) ~[postgresql-42.2.5.jar:42.2.5]
at com.zaxxer.hikari.pool.PoolBase.setNetworkTimeout(PoolBase.java:560) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.PoolBase.isConnectionAlive(PoolBase.java:173) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:186) ~[HikariCP-3.4.5.jar:na]
... 83 common frames omitted

2021-02-23 13:31:42.354  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Health check http status is 503 SERVICE_UNAVAILABLE and status is DOWN
2021-02-23 13:31:42.359  INFO 18016 --- [o-auto-1-exec-4] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.359  INFO 18016 --- [o-auto-1-exec-5] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.360  INFO 18016 --- [o-auto-1-exec-8] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.360  INFO 18016 --- [o-auto-1-exec-2] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.360  INFO 18016 --- [o-auto-1-exec-7] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.360  INFO 18016 --- [o-auto-1-exec-1] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.360  INFO 18016 --- [o-auto-1-exec-3] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.361  INFO 18016 --- [o-auto-1-exec-6] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.361  INFO 18016 --- [-auto-1-exec-10] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.362  INFO 18016 --- [o-auto-1-exec-9] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.362  INFO 18016 --- [-auto-1-exec-11] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.362  INFO 18016 --- [-auto-1-exec-12] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.863  WARN 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [o-auto-1-exec-3] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863  WARN 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-3] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 500ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 500ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 500ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-3] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 500ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 500ms.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.863 ERROR 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:42.871  INFO 18016 --- [nPool-worker-21] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.871  INFO 18016 --- [nPool-worker-27] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.871  INFO 18016 --- [onPool-worker-5] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.871  INFO 18016 --- [onPool-worker-3] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.872  INFO 18016 --- [onPool-worker-7] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-13] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.872  INFO 18016 --- [onPool-worker-5] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 500ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-27] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [onPool-worker-3] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-13] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 500ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-21] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 500ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [onPool-worker-7] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 500ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-31] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-31] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 500ms."}
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-19] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:42.872  INFO 18016 --- [nPool-worker-19] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:42.875  INFO 18016 --- [-auto-1-exec-10] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [o-auto-1-exec-9] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [-auto-1-exec-12] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [o-auto-1-exec-6] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [o-auto-1-exec-1] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [-auto-1-exec-11] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:42.875  INFO 18016 --- [o-auto-1-exec-7] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:43.361  WARN 18016 --- [o-auto-1-exec-5] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@6a8e88a (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2021-02-23 13:31:43.361  WARN 18016 --- [o-auto-1-exec-5] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.361 ERROR 18016 --- [o-auto-1-exec-5] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 1000ms.
2021-02-23 13:31:43.361 ERROR 18016 --- [o-auto-1-exec-5] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.362  WARN 18016 --- [o-auto-1-exec-4] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@5b5239ff (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2021-02-23 13:31:43.362  WARN 18016 --- [o-auto-1-exec-2] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@70bfc29d (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2021-02-23 13:31:43.362  WARN 18016 --- [o-auto-1-exec-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.362 ERROR 18016 --- [o-auto-1-exec-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 1001ms.
2021-02-23 13:31:43.362 ERROR 18016 --- [o-auto-1-exec-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.362  WARN 18016 --- [o-auto-1-exec-2] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.362 ERROR 18016 --- [o-auto-1-exec-2] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 1000ms.
2021-02-23 13:31:43.362 ERROR 18016 --- [o-auto-1-exec-2] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.363  INFO 18016 --- [nPool-worker-23] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.363  INFO 18016 --- [nPool-worker-23] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 1000ms."}
2021-02-23 13:31:43.363  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.363  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 1001ms."}
2021-02-23 13:31:43.363  INFO 18016 --- [nPool-worker-17] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.363  INFO 18016 --- [nPool-worker-17] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 1000ms."}
2021-02-23 13:31:43.378  WARN 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378  WARN 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378  WARN 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378  WARN 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-11] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378  WARN 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378  WARN 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-10] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378  WARN 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-9] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 501ms.
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378 ERROR 18016 --- [-auto-1-exec-12] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.378 ERROR 18016 --- [o-auto-1-exec-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-21] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-27] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-31] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-27] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-31] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.379  INFO 18016 --- [onPool-worker-3] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.379  INFO 18016 --- [nPool-worker-21] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.379  INFO 18016 --- [onPool-worker-3] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.379  INFO 18016 --- [onPool-worker-7] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.380  INFO 18016 --- [onPool-worker-7] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.380  INFO 18016 --- [nPool-worker-13] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.380  INFO 18016 --- [nPool-worker-19] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:43.380  INFO 18016 --- [nPool-worker-13] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:43.380  INFO 18016 --- [nPool-worker-19] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 501ms."}
2021-02-23 13:31:53.362  WARN 18016 --- [o-auto-1-exec-8] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7b728e68 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2021-02-23 13:31:53.362  WARN 18016 --- [o-auto-1-exec-8] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08003
2021-02-23 13:31:53.362 ERROR 18016 --- [o-auto-1-exec-8] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 11001ms.
2021-02-23 13:31:53.362 ERROR 18016 --- [o-auto-1-exec-8] o.h.engine.jdbc.spi.SqlExceptionHelper   : This connection has been closed.
2021-02-23 13:31:53.364  INFO 18016 --- [onPool-worker-9] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 503 SERVICE_UNAVAILABLE
2021-02-23 13:31:53.364  INFO 18016 --- [onPool-worker-9] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: {"message":"HikariPool-1 - Connection is not available, request timed out after 11001ms."}
2021-02-23 13:31:53.869  WARN 18016 --- [o-auto-1-exec-3] o.s.b.a.jdbc.DataSourceHealthIndicator   : DataSource health check failed

org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection; nested exception is java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 501ms.
at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(DataSourceUtils.java:82) ~[spring-jdbc-5.3.3.jar:5.3.3]
at org.springframework.jdbc.core.JdbcTemplate.execute(JdbcTemplate.java:330) ~[spring-jdbc-5.3.3.jar:5.3.3]
... (omitted)
at java.base/java.lang.Thread.run(Thread.java:832) ~[na:na]
Caused by: java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 501ms.
at com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:695) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:197) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:162) ~[HikariCP-3.4.5.jar:na]
at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:128) ~[HikariCP-3.4.5.jar:na]
at org.springframework.jdbc.datasource.DataSourceUtils.fetchConnection(DataSourceUtils.java:158) ~[spring-jdbc-5.3.3.jar:5.3.3]
at org.springframework.jdbc.datasource.DataSourceUtils.doGetConnection(DataSourceUtils.java:116) ~[spring-jdbc-5.3.3.jar:5.3.3]
at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(DataSourceUtils.java:79) ~[spring-jdbc-5.3.3.jar:5.3.3]
... 78 common frames omitted
Caused by: org.postgresql.util.PSQLException: This connection has been closed.
at org.postgresql.jdbc.PgConnection.checkClosed(PgConnection.java:783) ~[postgresql-42.2.5.jar:42.2.5]
at org.postgresql.jdbc.PgConnection.setNetworkTimeout(PgConnection.java:1556) ~[postgresql-42.2.5.jar:42.2.5]
... (omitted)
at it.minetti.dbautoreconnect.$Proxy123.findAll(Unknown Source) ~[na:na]
at it.minetti.dbautoreconnect.Controller.get(Controller.java:27) ~[classes/:na]
... 54 common frames omitted

2021-02-23 13:31:53.870  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Health check http status is 503 SERVICE_UNAVAILABLE and status is DOWN
2021-02-23 13:31:53.870  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Unpausing container postgres:11.1 ...
2021-02-23 13:31:55.909  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... unpaused.
2021-02-23 13:31:55.914  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Health check http status is 200 OK and status is UP
2021-02-23 13:31:55.915  INFO 18016 --- [o-auto-1-exec-4] it.minetti.dbautoreconnect.Controller    : Requested all Db Entities
2021-02-23 13:31:55.918  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Query for database entities has status 200 OK
2021-02-23 13:31:55.918  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    :  ... and entity body: ["first","second"]
2021-02-23 13:31:55.922  INFO 18016 --- [           main] i.m.d.DbAutoReconnectApplicationTests    : Health check http status is 200 OK and status is UP
2021-02-23 13:31:56.208  INFO 18016 --- [extShutdownHook] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'applicationTaskExecutor'
2021-02-23 13:31:56.209  INFO 18016 --- [extShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2021-02-23 13:31:56.210  INFO 18016 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2021-02-23 13:31:56.211  INFO 18016 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.

Process finished with exit code 0
```