package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.DailyReport;
import com.esimmcp.mcp.McpClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Sends the daily report through Gmail MCP.
 */
public final class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final AppConfig config;
    private final McpClientManager mcp;

    public EmailNotificationService(AppConfig config, McpClientManager mcp) {
        this.config = config;
        this.mcp = mcp;
    }

    public void send(DailyReport report) {
        String to = config.reportEmailTo();
        String subject = config.reportSubjectPrefix() + " " + report.reportDate();
        String body = report.toEmailBody();

        if (mcp.dryRun()) {
            log.info("Dry-run email to '{}'\nSubject: {}\n{}",
                    to.isBlank() ? "(not configured)" : to,
                    subject,
                    body);
            return;
        }

        if (to.isBlank()) {
            log.warn("report.email.to is empty; skipping Gmail send");
            return;
        }

        if (mcp.client("gmail").isEmpty()) {
            log.warn("Gmail MCP not connected; email not sent");
            return;
        }

        try {
            String tool = config.toolName("mcp.tool.gmail.send", "send_email");
            var result = mcp.callTool("gmail", tool, Map.of(
                    "to", to,
                    "subject", subject,
                    "body", body
            ));
            log.info("Gmail send result: {}", mcp.extractText(result));
        } catch (Exception e) {
            log.error("Failed to send Gmail report: {}", e.getMessage());
        }
    }
}
