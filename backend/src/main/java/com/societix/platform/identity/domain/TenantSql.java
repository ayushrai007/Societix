package com.societix.platform.identity.domain;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Applies the transaction-local tenant required by PostgreSQL RLS. */
@Component
public class TenantSql {

    private final JdbcTemplate jdbcTemplate;

    public TenantSql(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTenant(UUID tenantId) {
        jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
    }
}
