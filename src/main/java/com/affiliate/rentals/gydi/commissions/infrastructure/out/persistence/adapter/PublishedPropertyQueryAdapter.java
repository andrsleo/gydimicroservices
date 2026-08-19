package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.ports.PublishedPropertyQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter that queries the properties bounded context to check for published properties.
 * <p>
 * Cross-context read: commissions → properties (read-only, no domain event needed).
 * Uses native SQL via JdbcTemplate to avoid coupling with the properties JPA model.
 * </p>
 */
@Component
public class PublishedPropertyQueryAdapter implements PublishedPropertyQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public PublishedPropertyQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true if the user has at least one property with status 'PUBLISHED'.
     * Affiliates with a published property earn 6%; others earn 4%.
     */
    @Override
    public boolean hasPublishedProperty(Long userId) {
        String sql = "SELECT COUNT(*) FROM properties.properties WHERE host_id = ? AND status = 'PUBLISHED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }
}
