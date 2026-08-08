package com.esimmcp.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Result of one daily discovery run.
 */
public final class DailyReport {

    private final LocalDate reportDate;
    private final Instant generatedAt;
    private final List<EsimPlan> candidates;
    private final List<EsimPlan> matchingPlans;
    private final EsimPlan bestPlan;
    private final String evaluationNotes;

    public DailyReport(
            LocalDate reportDate,
            Instant generatedAt,
            List<EsimPlan> candidates,
            List<EsimPlan> matchingPlans,
            EsimPlan bestPlan,
            String evaluationNotes) {
        this.reportDate = reportDate;
        this.generatedAt = generatedAt;
        this.candidates = List.copyOf(candidates);
        this.matchingPlans = List.copyOf(matchingPlans);
        this.bestPlan = bestPlan;
        this.evaluationNotes = evaluationNotes == null ? "" : evaluationNotes;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<EsimPlan> candidates() {
        return candidates;
    }

    public List<EsimPlan> matchingPlans() {
        return matchingPlans;
    }

    public Optional<EsimPlan> bestPlan() {
        return Optional.ofNullable(bestPlan);
    }

    public String evaluationNotes() {
        return evaluationNotes;
    }

    public String toEmailBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("esim-mcp daily report\n");
        sb.append("Date: ").append(reportDate).append('\n');
        sb.append("Generated at: ").append(generatedAt).append("\n\n");

        if (bestPlan == null) {
            sb.append("Best plan: NONE (no plan matched all criteria)\n\n");
        } else {
            sb.append("Best plan:\n");
            appendPlan(sb, bestPlan);
            sb.append('\n');
        }

        sb.append("Matching plans (").append(matchingPlans.size()).append("):\n");
        for (EsimPlan plan : matchingPlans) {
            appendPlan(sb, plan);
            sb.append('\n');
        }

        sb.append("Evaluation notes:\n").append(evaluationNotes).append('\n');
        return sb.toString();
    }

    private static void appendPlan(StringBuilder sb, EsimPlan plan) {
        sb.append("- ").append(plan.provider()).append(" / ").append(plan.planName()).append('\n');
        sb.append("  Monthly: ").append(plan.monthlyPrice()).append(' ').append(plan.currency()).append('\n');
        sb.append("  eSIM: ").append(plan.esimSupported())
                .append(", intl SMS receive: ").append(plan.internationalSmsReceive())
                .append(", promo: ").append(plan.promotionalPrice()).append('\n');
        if (plan.sourceUrl() != null) {
            sb.append("  Source: ").append(plan.sourceUrl()).append('\n');
        }
        if (plan.notes() != null && !plan.notes().isBlank()) {
            sb.append("  Notes: ").append(plan.notes()).append('\n');
        }
    }
}
