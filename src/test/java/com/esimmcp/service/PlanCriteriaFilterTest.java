package com.esimmcp.service;

import com.esimmcp.domain.EsimPlan;
import com.esimmcp.domain.PlanAvailabilitySignals;
import com.esimmcp.domain.PlanCriteria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanCriteriaFilterTest {

    private final PlanCriteriaFilter filter = new PlanCriteriaFilter(PlanCriteria.DEFAULT);

    @Test
    void selectsCheapestStableEsimWithIntlSms() {
        EsimPlan winner = EsimPlan.builder()
                .provider("A")
                .planName("Good")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .available(true)
                .monthlyPrice(new BigDecimal("8.00"))
                .promotionalPrice(false)
                .build();

        EsimPlan promo = EsimPlan.builder()
                .provider("B")
                .planName("Promo")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .available(true)
                .monthlyPrice(new BigDecimal("1.00"))
                .promotionalPrice(true)
                .regularMonthlyPrice(new BigDecimal("30.00"))
                .build();

        EsimPlan noSms = EsimPlan.builder()
                .provider("C")
                .planName("Data")
                .esimSupported(true)
                .internationalSmsReceive(false)
                .available(true)
                .monthlyPrice(new BigDecimal("2.00"))
                .promotionalPrice(false)
                .build();

        Optional<EsimPlan> best = filter.bestOf(List.of(promo, noSms, winner));
        assertTrue(best.isPresent());
        assertEquals("Good", best.get().planName());
    }

    @Test
    void rejectsPhysicalSimOnly() {
        EsimPlan physical = EsimPlan.builder()
                .provider("D")
                .planName("Classic")
                .esimSupported(false)
                .internationalSmsReceive(true)
                .available(true)
                .monthlyPrice(new BigDecimal("1.00"))
                .promotionalPrice(false)
                .build();

        assertTrue(filter.filter(List.of(physical)).isEmpty());
    }

    @Test
    void rejectsUnavailableClosedPlans() {
        EsimPlan closed = EsimPlan.builder()
                .provider("KT스카이라이프")
                .planName("초슬림 500M/60분")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .available(false)
                .monthlyPrice(new BigDecimal("1900"))
                .currency("KRW")
                .promotionalPrice(false)
                .notes("해당 요금제는 마감되었습니다.")
                .sourceUrl("https://weayo.com/mobile/plan/1000014573")
                .build();

        EsimPlan open = EsimPlan.builder()
                .provider("LG헬로모바일")
                .planName("슬림 유심 500MB 50분")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .available(true)
                .monthlyPrice(new BigDecimal("1700"))
                .currency("KRW")
                .promotionalPrice(false)
                .sourceUrl("https://weayo.com/mobile/plan/1000001726")
                .build();

        List<EsimPlan> matching = filter.filter(List.of(closed, open));
        assertEquals(1, matching.size());
        assertEquals("슬림 유심 500MB 50분", matching.get(0).planName());
    }

    @Test
    void detectsClosedDialogText() {
        assertTrue(PlanAvailabilitySignals.looksUnavailable("해당 요금제는 마감되었습니다."));
        assertFalse(PlanAvailabilitySignals.looksUnavailable("월 1,900원 해외 로밍 가능"));
    }
}
