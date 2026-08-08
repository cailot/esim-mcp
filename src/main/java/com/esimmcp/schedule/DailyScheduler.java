package com.esimmcp.schedule;

import com.esimmcp.config.AppConfig;
import com.esimmcp.service.DailyReportPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the report pipeline once per day until the configured end date (Korea arrival).
 */
public final class DailyScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DailyScheduler.class);

    private final AppConfig config;
    private final DailyReportPipeline pipeline;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public DailyScheduler(AppConfig config, DailyReportPipeline pipeline) {
        this.config = config;
        this.pipeline = pipeline;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "esim-daily-scheduler");
            t.setDaemon(false);
            return t;
        });
    }

    public void start() {
        LocalDate end = config.reportEndDate();
        LocalDate today = LocalDate.now(config.timezone());
        if (today.isAfter(end)) {
            log.info("Report end date {} already passed; scheduler will not start", end);
            return;
        }

        long initialDelaySeconds = secondsUntilNextRun();
        log.info("Scheduling daily reports until {} at {}:{} ({}); first run in {}s",
                end,
                config.reportHour(),
                config.reportMinute(),
                config.timezone(),
                initialDelaySeconds);

        executor.scheduleAtFixedRate(this::safeRun, initialDelaySeconds, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    /** Immediate one-shot execution (useful for CLI / testing). */
    public void runNow() {
        safeRun();
    }

    private void safeRun() {
        if (stopped.get()) {
            return;
        }
        LocalDate today = LocalDate.now(config.timezone());
        if (today.isAfter(config.reportEndDate())) {
            log.info("Reached end date {}; shutting down scheduler", config.reportEndDate());
            close();
            return;
        }
        try {
            pipeline.runOnce();
        } catch (Exception e) {
            log.error("Daily pipeline failed: {}", e.getMessage(), e);
        }
    }

    private long secondsUntilNextRun() {
        ZonedDateTime now = ZonedDateTime.now(config.timezone());
        LocalTime runTime = LocalTime.of(config.reportHour(), config.reportMinute());
        ZonedDateTime next = LocalDateTime.of(now.toLocalDate(), runTime).atZone(config.timezone());
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Math.max(1, Duration.between(now, next).getSeconds());
    }

    @Override
    public void close() {
        if (stopped.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }
}
