package com.globalvibe.arbitrage.domain.report.repository;

import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;

import java.util.List;
import java.util.Optional;

public interface ReportAggregateRepository {

    ReportAggregate save(ReportAggregate reportAggregate);

    Optional<ReportAggregate> findByTaskId(String taskId);

    Optional<ReportAggregate> findByReportId(String reportId);

    List<ReportAggregate> findAll();
}
