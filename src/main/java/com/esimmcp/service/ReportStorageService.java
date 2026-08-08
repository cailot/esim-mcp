package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.DailyReport;
import com.esimmcp.mcp.McpClientManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists daily reports through Supabase MCP.
 */
public final class ReportStorageService {

    private static final Logger log = LoggerFactory.getLogger(ReportStorageService.class);

    private final AppConfig config;
    private final McpClientManager mcp;
    private final ObjectMapper objectMapper;

    public ReportStorageService(AppConfig config, McpClientManager mcp, ObjectMapper objectMapper) {
        this.config = config;
        this.mcp = mcp;
        this.objectMapper = objectMapper;
    }

    public void save(DailyReport report) {
        if (mcp.client("supabase").isEmpty()) {
            log.warn("Supabase MCP not connected; report not stored");
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(toRow(report));
            String sql = """
                    insert into daily_esim_reports (
                      report_date, generated_at, best_plan, matching_plans, candidates, evaluation_notes
                    ) values (
                      '%s'::date,
                      '%s'::timestamptz,
                      '%s'::jsonb,
                      '%s'::jsonb,
                      '%s'::jsonb,
                      %s
                    )
                    on conflict (report_date) do update set
                      generated_at = excluded.generated_at,
                      best_plan = excluded.best_plan,
                      matching_plans = excluded.matching_plans,
                      candidates = excluded.candidates,
                      evaluation_notes = excluded.evaluation_notes;
                    """.formatted(
                    report.reportDate(),
                    report.generatedAt(),
                    escapeSql(objectMapper.writeValueAsString(report.bestPlan().orElse(null))),
                    escapeSql(objectMapper.writeValueAsString(report.matchingPlans())),
                    escapeSql(objectMapper.writeValueAsString(report.candidates())),
                    sqlString(report.evaluationNotes())
            );

            String tool = config.toolName("mcp.tool.supabase.execute_sql", "execute_sql");
            var result = mcp.callTool("supabase", tool, Map.of("query", sql));
            log.info("Stored report in Supabase: {}", mcp.extractText(result));
            log.debug("Stored payload preview: {}", payload.length() > 200 ? payload.substring(0, 200) : payload);
        } catch (Exception e) {
            log.error("Failed to store report in Supabase: {}", e.getMessage());
        }
    }

    private Map<String, Object> toRow(DailyReport report) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("report_date", report.reportDate().toString());
        row.put("generated_at", report.generatedAt().toString());
        row.put("best_plan", report.bestPlan().orElse(null));
        row.put("matching_plans", report.matchingPlans());
        row.put("candidates", report.candidates());
        row.put("evaluation_notes", report.evaluationNotes());
        return row;
    }

    private static String escapeSql(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("'", "''");
    }

    private static String sqlString(String value) {
        if (value == null) {
            return "null";
        }
        return "'" + value.replace("'", "''") + "'";
    }
}
