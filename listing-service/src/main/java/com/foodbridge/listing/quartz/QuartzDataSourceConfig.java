package com.foodbridge.listing.quartz;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Quartz's JDBC JobStore is intentionally backed by a <b>separate</b> MySQL
 * database/schema from the service's business data (see
 * {@code foodbridge_listing_quartz} vs {@code foodbridge_listings} in
 * docker-compose.yml). This keeps scheduler bookkeeping (QRTZ_* tables:
 * triggers, fired-trigger locks, misfire state) fully decoupled from
 * business schema migrations and lets the scheduler database be scaled,
 * backed up, or reset independently — clearing a stuck trigger should never
 * require touching business data, and vice versa.
 *
 * <p>Spring Boot's {@code QuartzAutoConfiguration} automatically wires
 * whichever {@link DataSource} bean is annotated {@link QuartzDataSource}
 * into the scheduler's JobStore, instead of reusing the primary
 * {@code @Primary} business DataSource.</p>
 */
@Configuration
public class QuartzDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.quartz-datasource")
    public DataSourceProperties quartzDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource(
            @org.springframework.beans.factory.annotation.Qualifier("quartzDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
