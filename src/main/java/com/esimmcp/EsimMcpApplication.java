package com.esimmcp;

import com.esimmcp.config.AppConfig;
import com.esimmcp.mcp.McpClientManager;
import com.esimmcp.schedule.DailyScheduler;
import com.esimmcp.service.DailyReportPipeline;
import com.esimmcp.service.EmailNotificationService;
import com.esimmcp.service.PlanDiscoveryService;
import com.esimmcp.service.PlanEvaluationService;
import com.esimmcp.service.ReportStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Standalone entry point for daily eSIM plan discovery via MCP.
 *
 * <pre>
 *   mvn -q exec:java                      # run once, then exit (default)
 *   mvn -q exec:java -Dexec.args=once     # same as default
 *   mvn -q exec:java -Dexec.args=schedule # keep running on daily schedule
 * </pre>
 */
public final class EsimMcpApplication {

    private static final Logger log = LoggerFactory.getLogger(EsimMcpApplication.class);

    public static void main(String[] args) throws Exception {
        boolean schedule = args.length > 0 && "schedule".equalsIgnoreCase(args[0]);
        AppConfig config = AppConfig.load();

        LocalDate today = LocalDate.now(config.timezone());
        if (today.isAfter(config.reportEndDate())) {
            log.info("Report end date {} already passed (today={}); exiting", config.reportEndDate(), today);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        try (McpClientManager mcp = McpClientManager.create(config)) {
            PlanDiscoveryService discovery = new PlanDiscoveryService(config, mcp, objectMapper);
            PlanEvaluationService evaluation = new PlanEvaluationService(config, mcp);
            ReportStorageService storage = new ReportStorageService(config, mcp, objectMapper);
            EmailNotificationService email = new EmailNotificationService(config);

            DailyReportPipeline pipeline = new DailyReportPipeline(
                    discovery,
                    evaluation,
                    storage,
                    email,
                    config.timezone()
            );

            if (!schedule) {
                log.info("Running one-shot daily report (will exit when finished)");
                pipeline.runOnce();
                log.info("One-shot report finished; exiting");
                return;
            }

            try (DailyScheduler scheduler = new DailyScheduler(config, pipeline)) {
                scheduler.runNow();
                scheduler.start();
                log.info("esim-mcp scheduler is running. Press Ctrl+C to stop.");
                Thread.currentThread().join();
            }
        }
    }

    private EsimMcpApplication() {
    }
}
