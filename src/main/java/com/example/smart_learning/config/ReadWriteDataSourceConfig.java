package com.example.smart_learning.config;

import com.example.smart_learning.common.datasource.Route;
import com.example.smart_learning.common.datasource.RoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class ReadWriteDataSourceConfig {

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource(DataSourceProperties properties) {
        return build(properties.getMaster(), "master");
    }

    @Bean(name = "readDataSource")
    public DataSource readDataSource(DataSourceProperties properties) {
        return build(properties.getRead(), "read");
    }

    @Bean(name = "routingDataSource")
    public RoutingDataSource routingDataSource(@Qualifier("masterDataSource") DataSource master,
                                               @Qualifier("readDataSource") DataSource read) {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(Route.MASTER, master, Route.READ, read));
        routing.setDefaultTargetDataSource(master);
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(@Qualifier("routingDataSource") RoutingDataSource routing) {
        // The proxy asks for a connection only when the first statement runs, after the
        // @ReadOnly/@Master aspect and the transaction readOnly flag have been evaluated.
        return new LazyConnectionDataSourceProxy(routing);
    }

    @Bean(name = "flyway", initMethod = "migrate")
    public Flyway flyway(@Qualifier("masterDataSource") DataSource master) {
        return Flyway.configure()
                .dataSource(master)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    private DataSource build(DataSourceProperties.Pool pool, String role) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(pool.getUrl());
        dataSource.setUsername(pool.getUsername());
        dataSource.setPassword(pool.getPassword());
        dataSource.setDriverClassName(pool.getDriverClassName());
        dataSource.setMaximumPoolSize(pool.getMaximumPoolSize());
        dataSource.setMinimumIdle(pool.getMinimumIdle());
        dataSource.setConnectionTimeout(pool.getConnectionTimeout());
        dataSource.setPoolName(role + "-pool");
        return dataSource;
    }
}
