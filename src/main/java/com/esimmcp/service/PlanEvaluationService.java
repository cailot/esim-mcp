package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.EsimPlan;
import com.esimmcp.domain.PlanCriteria;
import com.esimmcp.mcp.McpClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates candidates with hard criteria, optionally using Sequential Thinking MCP.
 */
public final class PlanEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PlanEvaluationService.class);

    private final AppConfig config;
    private final McpClientManager mcp;
    private final PlanCriteriaFilter filter;

    public PlanEvaluationService(AppConfig config, McpClientManager mcp) {
        this.config = config;
        this.mcp = mcp;
        this.filter = new PlanCriteriaFilter(PlanCriteria.DEFAULT);
    }

    public EvaluationResult evaluate(List<EsimPlan> candidates) {
        List<EsimPlan> matching = filter.filter(candidates);
        Optional<EsimPlan> best = matching.stream().findFirst();

        StringBuilder notes = new StringBuilder();
        notes.append("Criteria: available/joinable=yes, eSIM=yes, international SMS receive=yes, lowest ongoing keep fee (lifetime 특가 allowed; intro-then-hike rejected).\n");
        notes.append("Candidates: ").append(candidates.size())
                .append(", matching: ").append(matching.size()).append('\n');
        long unavailable = candidates.stream().filter(p -> !p.available()).count();
        if (unavailable > 0) {
            notes.append("Rejected unavailable/sold-out candidates: ").append(unavailable).append('\n');
        }

        mcp.client("sequential").ifPresent(client -> {
            try {
                String tool = config.toolName("mcp.tool.sequential.thinking", "sequentialthinking");
                String thought = """
                        Step-by-step evaluate Korean keep-number eSIM plans until 2026-09-15.
                        Hard rules: currently available/joinable (reject 마감/sold-out/signup-blocked);
                        eSIM supported; can receive SMS abroad; lowest ongoing monthly keep fee;
                        lifetime 특가 is OK even with signup deadline or usage-to-keep-discount rules;
                        reject only promotional intro pricing that rises after a term.
                        Matching plans: %s
                        Best so far: %s
                        """.formatted(
                        matching.stream().map(EsimPlan::toString).toList(),
                        best.map(EsimPlan::toString).orElse("none")
                );
                var result = mcp.callTool("sequential", tool, Map.of(
                        "thought", thought,
                        "nextThoughtNeeded", false,
                        "thoughtNumber", 1,
                        "totalThoughts", 1
                ));
                notes.append("Sequential Thinking:\n").append(mcp.extractText(result)).append('\n');
            } catch (Exception e) {
                log.error("Sequential Thinking MCP failed: {}", e.getMessage());
                notes.append("Sequential Thinking failed: ").append(e.getMessage()).append('\n');
            }
        });

        best.ifPresent(plan -> notes.append("Selected best plan: ").append(plan).append('\n'));
        return new EvaluationResult(matching, best.orElse(null), notes.toString());
    }

    public record EvaluationResult(List<EsimPlan> matchingPlans, EsimPlan bestPlan, String notes) {
    }
}
