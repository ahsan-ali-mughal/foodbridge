package com.foodbridge.document.quartz;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * See listing-service's {@code QuartzDataSourceConfig} for the rationale.
 * document-service's Quartz JobStore lives in {@code foodbridge_document_quartz},
 * separate from its MongoDB business data.
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
    public DataSource quartzDataSource(@Qualifier("quartzDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
