package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.DailyReport;
import com.esimmcp.mcp.McpClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
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
        String textBody = report.toEmailBody();
        String htmlBody = report.toHtmlEmailBody();

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
            Map<String, Object> args = new HashMap<>();
            args.put("to", List.of(to));
            args.put("subject", subject);
            args.put("body", textBody);
            args.put("htmlBody", htmlBody);
            args.put("mimeType", "multipart/alternative");
            var result = mcp.callTool("gmail", tool, args);
            log.info("Gmail send result: {}", mcp.extractText(result));
        } catch (Exception e) {
            log.error("Failed to send Gmail report: {}", e.getMessage());
        }
    }
}
