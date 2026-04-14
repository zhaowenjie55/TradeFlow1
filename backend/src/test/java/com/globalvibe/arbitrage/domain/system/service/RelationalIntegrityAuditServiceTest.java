package com.globalvibe.arbitrage.domain.system.service;

import com.globalvibe.arbitrage.domain.system.dto.IntegrityAuditResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationalIntegrityAuditServiceTest {

    @Test
    void shouldSummarizeOrphansAndConstraintValidation() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RelationalIntegrityAuditService service = new RelationalIntegrityAuditService(jdbcTemplate);

        AtomicInteger orphanCalls = new AtomicInteger();
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenAnswer(invocation -> switch (orphanCalls.getAndIncrement()) {
            case 0 -> 2L;
            case 1 -> 0L;
            case 2 -> 0L;
            case 3 -> 1L;
            case 4 -> 0L;
            case 5 -> 0L;
            case 6 -> 0L;
            case 7 -> 0L;
            case 8 -> 0L;
            case 9 -> 0L;
            default -> 0L;
        });

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString())).thenAnswer(invocation -> {
            String constraintName = invocation.getArgument(2, String.class);
            return !"fk_gv_candidate_match_candidate".equals(constraintName);
        });

        IntegrityAuditResponse response = service.audit();

        assertEquals(3L, response.totalOrphanCount());
        assertFalse(response.allClear());
        assertFalse(response.allConstraintsValidated());
        assertEquals(10, response.orphanChecks().size());
        assertEquals(10, response.constraintStatuses().size());
        assertTrue(response.constraintStatuses().stream()
                .anyMatch(status -> status.constraintName().equals("fk_gv_search_run_result_run") && status.validated()));
        assertTrue(response.constraintStatuses().stream()
                .anyMatch(status -> status.constraintName().equals("fk_gv_candidate_match_candidate") && !status.validated()));
    }
}
