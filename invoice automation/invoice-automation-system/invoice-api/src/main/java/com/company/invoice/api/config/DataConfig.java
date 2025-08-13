package com.company.invoice.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for Data layer components.
 * Ensures proper scanning of entities and repositories from all modules.
 */
@Configuration
@EntityScan(basePackages = {
    "com.company.invoice.email.entity",
    "com.company.invoice.data.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.company.invoice.email.repository", 
    "com.company.invoice.data.repository"
})
public class DataConfig {
    // Configuration class to ensure proper entity and repository scanning
}
