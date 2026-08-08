package com.esimmcp.service;

import com.esimmcp.domain.DailyReport;
import com.esimmcp.domain.EsimPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * End-to-end daily flow: discover -> evaluate -> store -> email.
 */
public final class DailyReportPipeline {

    private static final Logger log = LoggerFactory.getLogger(DailyReportPipeline.class);

    private final PlanDiscoveryService discoveryService;
    private final PlanEvaluationService evaluationService;
    private final ReportStorageService storageService;
    private final EmailNotificationService emailService;
    private final ZoneId zoneId;

    public DailyReportPipeline(
            PlanDiscoveryService discoveryService,
            PlanEvaluationService evaluationService,
            ReportStorageService storageService,
            EmailNotificationService emailService,
            ZoneId zoneId) {
        this.discoveryService = discoveryService;
        this.evaluationService = evaluationService;
        this.storageService = storageService;
        this.emailService = emailService;
        this.zoneId = zoneId;
    }

    public DailyReport runOnce() {
        LocalDate today = LocalDate.now(zoneId);
        log.info("Starting daily eSIM report pipeline for {}", today);

        List<EsimPlan> candidates = discoveryService.discover();
        PlanEvaluationService.EvaluationResult evaluation = evaluationService.evaluate(candidates);

        DailyReport report = new DailyReport(
                today,
                Instant.now(),
                candidates,
                evaluation.matchingPlans(),
                evaluation.bestPlan(),
                evaluation.notes()
        );

        storageService.save(report);
        emailService.send(report);

        log.info("Daily report complete. best={}",
                report.bestPlan().map(EsimPlan::toString).orElse("none"));
        return report;
    }
}
