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

/**
 * Standalone entry point for daily eSIM plan discovery via MCP.
 *
 * <pre>
 *   mvn -q exec:java                 # schedule until 2026-09-15
 *   mvn -q exec:java -Dexec.args=once  # run one report immediately
 * </pre>
 */
public final class EsimMcpApplication {

    private static final Logger log = LoggerFactory.getLogger(EsimMcpApplication.class);

    public static void main(String[] args) throws Exception {
        boolean once = args.length > 0 && "once".equalsIgnoreCase(args[0]);
        AppConfig config = AppConfig.load();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        try (McpClientManager mcp = McpClientManager.create(config)) {
            PlanDiscoveryService discovery = new PlanDiscoveryService(config, mcp, objectMapper);
            PlanEvaluationService evaluation = new PlanEvaluationService(config, mcp);
            ReportStorageService storage = new ReportStorageService(config, mcp, objectMapper);
            EmailNotificationService email = new EmailNotificationService(config, mcp);

            DailyReportPipeline pipeline = new DailyReportPipeline(
                    discovery,
                    evaluation,
                    storage,
                    email,
                    config.timezone()
            );

            if (once) {
                log.info("Running one-shot daily report");
                pipeline.runOnce();
                return;
            }

            try (DailyScheduler scheduler = new DailyScheduler(config, pipeline)) {
                // Run once at startup so the first report is available immediately,
                // then continue on the daily schedule until the end date.
                scheduler.runNow();
                scheduler.start();
                log.info("esim-mcp is running. Press Ctrl+C to stop.");
                Thread.currentThread().join();
            }
        }
    }

    private EsimMcpApplication() {
    }
}
