package com.lukala.ledger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate} on
 * {@link com.lukala.ledger.common.domain.BaseEntity} are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
