package com.esimmcp.service;

import com.esimmcp.domain.EsimPlan;
import com.esimmcp.domain.PlanCriteria;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Filters and ranks plans by project selection rules.
 */
public final class PlanCriteriaFilter {

    private final PlanCriteria criteria;

    public PlanCriteriaFilter(PlanCriteria criteria) {
        this.criteria = criteria;
    }

    public List<EsimPlan> filter(List<EsimPlan> candidates) {
        return candidates.stream()
                .filter(criteria::matches)
                .sorted(Comparator.comparing(EsimPlan::ongoingMonthlyPrice)
                        .thenComparing(EsimPlan::provider))
                .toList();
    }

    public Optional<EsimPlan> bestOf(List<EsimPlan> candidates) {
        List<EsimPlan> matching = filter(candidates);
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matching.get(0));
    }
}
