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
            sb.append("Best plan: ")
                    .append(bestPlan.provider()).append(" / ").append(bestPlan.planName())
                    .append(" — ").append(bestPlan.monthlyPrice()).append(' ').append(bestPlan.currency())
                    .append('\n');
            if (bestPlan.sourceUrl() != null) {
                sb.append("Link: ").append(bestPlan.sourceUrl()).append('\n');
            }
            sb.append('\n');
        }

        sb.append("Top 3:\n");
        int rank = 1;
        for (EsimPlan plan : matchingPlans.stream().limit(3).toList()) {
            sb.append(rank++).append(". ")
                    .append(plan.provider()).append(" / ").append(plan.planName())
                    .append(" — ").append(plan.monthlyPrice()).append(' ').append(plan.currency());
            if (plan.sourceUrl() != null) {
                sb.append(" — ").append(plan.sourceUrl());
            }
            sb.append('\n');
        }

        sb.append("\nRejected / other candidates:\n");
        for (EsimPlan plan : candidates) {
            if (matchingPlans.stream().limit(3).anyMatch(p -> p.equals(plan))) {
                continue;
            }
            sb.append("- ").append(plan.provider()).append(" / ").append(plan.planName())
                    .append(" | available=").append(plan.available())
                    .append(" | ").append(plan.monthlyPrice()).append(' ').append(plan.currency());
            if (plan.sourceUrl() != null) {
                sb.append(" | ").append(plan.sourceUrl());
            }
            sb.append('\n');
        }

        sb.append("\nEvaluation notes:\n").append(evaluationNotes).append('\n');
        return sb.toString();
    }

    public String toHtmlEmailBody() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;")
                .append("line-height:1.5;color:#111\">");
        html.append("<h2>esim-mcp Daily eSIM Report</h2>");
        html.append("<p><b>Date:</b> ").append(escape(reportDate.toString()))
                .append(" (KST)<br/><b>Generated:</b> ").append(escape(generatedAt.toString())).append("</p>");
        html.append("<p>선정 조건: 가입가능 · eSIM · 해외 SMS 수신 · 최저 월 유지비 · 가격 안정성</p>");

        if (bestPlan == null) {
            html.append("<p><b>Best plan:</b> NONE (조건에 맞는 요금제 없음)</p>");
        } else {
            html.append("<p><b>Best plan:</b> ")
                    .append(planLink(bestPlan))
                    .append(" — <b>")
                    .append(escape(bestPlan.monthlyPrice().toPlainString()))
                    .append(' ')
                    .append(escape(bestPlan.currency()))
                    .append("</b></p>");
        }

        html.append("<h3>Top 3</h3>");
        html.append("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse\">");
        html.append("<thead><tr style=\"background:#f5f5f5\">")
                .append("<th>순위</th><th>요금제</th><th>월 요금</th><th>eSIM</th><th>가입가능</th><th>비고</th>")
                .append("</tr></thead><tbody>");

        List<EsimPlan> top3 = matchingPlans.stream().limit(3).toList();
        int rank = 1;
        for (EsimPlan plan : top3) {
            html.append("<tr>");
            html.append("<td>").append(rank++).append("</td>");
            html.append("<td>").append(planLink(plan)).append("</td>");
            html.append("<td>").append(escape(plan.monthlyPrice().toPlainString()))
                    .append(' ').append(escape(plan.currency())).append("</td>");
            html.append("<td>").append(plan.esimSupported() ? "지원" : "미지원").append("</td>");
            html.append("<td>").append(plan.available() ? "가능" : "불가").append("</td>");
            html.append("<td>").append(escape(shortNotes(plan))).append("</td>");
            html.append("</tr>");
        }
        if (top3.isEmpty()) {
            html.append("<tr><td colspan=\"6\">조건에 맞는 요금제 없음</td></tr>");
        }
        html.append("</tbody></table>");

        html.append("<h3>탈락 / 기타 후보</h3><ul>");
        boolean anyRejected = false;
        for (EsimPlan plan : candidates) {
            if (top3.stream().anyMatch(p -> p.equals(plan))) {
                continue;
            }
            anyRejected = true;
            html.append("<li>")
                    .append(planLink(plan))
                    .append(" — ")
                    .append(escape(plan.monthlyPrice().toPlainString()))
                    .append(' ')
                    .append(escape(plan.currency()))
                    .append(" | available=")
                    .append(plan.available())
                    .append(" | eSIM=")
                    .append(plan.esimSupported());
            String notes = shortNotes(plan);
            if (!notes.isBlank()) {
                html.append(" — ").append(escape(notes));
            }
            html.append("</li>");
        }
        if (!anyRejected) {
            html.append("<li>없음</li>");
        }
        html.append("</ul>");

        html.append("<h3>평가 메모</h3><pre style=\"white-space:pre-wrap;font-size:12px\">")
                .append(escape(evaluationNotes))
                .append("</pre>");
        html.append("</body></html>");
        return html.toString();
    }

    private static String planLink(EsimPlan plan) {
        String label = escape(plan.provider() + " " + plan.planName());
        if (plan.sourceUrl() == null || plan.sourceUrl().isBlank()) {
            return "<b>" + label + "</b>";
        }
        return "<a href=\"" + escapeAttr(plan.sourceUrl()) + "\"><b>" + label + "</b></a>";
    }

    private static String shortNotes(EsimPlan plan) {
        if (plan.notes() == null || plan.notes().isBlank()) {
            return "";
        }
        return plan.notes()
                .replace("Seed:", "")
                .replace("| Playwright: page opened", "")
                .replace("Playwright: page opened", "")
                .trim();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeAttr(String value) {
        return escape(value).replace("\"", "&quot;");
    }
}
