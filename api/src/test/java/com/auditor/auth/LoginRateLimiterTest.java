package com.auditor.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    private static final Instant START =
            Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void limitsFailuresByNormalizedEmailAndReturnsRetryAfter() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(5, 30, 100, clock);

        for (int attempt = 0; attempt < 5; attempt++) {
            failAttempt(
                    limiter,
                    "user@example.com",
                    "192.0.2." + (attempt + 1));
        }

        LoginRateLimitExceededException exception = assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "user@example.com", "192.0.2.20"));

        assertEquals(900, exception.getRetryAfterSeconds());
    }

    @Test
    void limitsFailuresByClientIp() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(100, 30, 100, clock);

        for (int attempt = 0; attempt < 30; attempt++) {
            failAttempt(
                    limiter,
                    "user" + attempt + "@example.com",
                    "192.0.2.10");
        }

        LoginRateLimitExceededException exception = assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "another@example.com", "192.0.2.10"));

        assertEquals(900, exception.getRetryAfterSeconds());
    }

    @Test
    void rollingWindowAllowsAttemptAfterOldFailureExpires() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(5, 30, 100, clock);

        for (int attempt = 0; attempt < 5; attempt++) {
            failAttempt(
                    limiter, "user@example.com", "192.0.2.10");
        }

        clock.advance(Duration.ofMinutes(15).minusMillis(1));
        LoginRateLimitExceededException exception = assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "user@example.com", "192.0.2.10"));
        assertEquals(1, exception.getRetryAfterSeconds());

        clock.advance(Duration.ofMillis(1));
        try (LoginRateLimiter.Attempt ignored =
                     limiter.beginAttempt(
                             "user@example.com", "192.0.2.10")) {
            ignored.recordSuccess();
        }
    }

    @Test
    void successfulLoginClearsEmailFailuresButPreservesIpFailures() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(5, 5, 100, clock);

        failAttempt(limiter, "target@example.com", "192.0.2.20");
        for (int attempt = 0; attempt < 5; attempt++) {
            failAttempt(
                    limiter,
                    "other" + attempt + "@example.com",
                    "192.0.2.10");
        }

        try (LoginRateLimiter.Attempt successful =
                     limiter.beginAttempt(
                             "target@example.com", "192.0.2.20")) {
            successful.recordSuccess();
        }

        try (LoginRateLimiter.Attempt recoveredEmail =
                     limiter.beginAttempt(
                             "target@example.com", "192.0.2.20")) {
            recoveredEmail.recordSuccess();
        }

        assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "another@example.com", "192.0.2.10"));
    }

    @Test
    void concurrentAttemptsCannotExceedEitherQuota() throws Exception {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(5, 30, 100, clock);
        int workers = 40;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();

        try {
            for (int i = 0; i < workers; i++) {
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting to start.");
                    }

                    try (LoginRateLimiter.Attempt attempt =
                                 limiter.beginAttempt(
                                         "same@example.com",
                                         "192.0.2.10")) {
                        admitted.incrementAndGet();
                        attempt.recordFailure();
                    } catch (LoginRateLimitExceededException expected) {
                        // The configured quota rejected this attempt.
                    }
                    return null;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (var task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(5, admitted.get());
        assertEquals(1, limiter.trackedEmailKeyCount());
        assertEquals(1, limiter.trackedIpKeyCount());
    }

    @Test
    void keyStateRemainsBoundedAndRecoversAfterWindowExpires() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(100, 100, 2, clock);
        failAttempt(limiter, "one@example.com", "192.0.2.1");
        failAttempt(limiter, "two@example.com", "192.0.2.2");

        assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "three@example.com", "192.0.2.3"));

        assertTrue(limiter.trackedEmailKeyCount() <= 2);
        assertTrue(limiter.trackedIpKeyCount() <= 2);

        clock.advance(Duration.ofMinutes(15));
        try (LoginRateLimiter.Attempt attempt =
                     limiter.beginAttempt(
                             "three@example.com", "192.0.2.3")) {
            attempt.recordSuccess();
        }

        assertTrue(limiter.trackedEmailKeyCount() <= 2);
        assertTrue(limiter.trackedIpKeyCount() <= 2);
    }

    @Test
    void activeReservationsAtKeyCapacityReturnShortRetryHint() {
        MutableClock clock = new MutableClock(START);
        LoginRateLimiter limiter = limiter(100, 100, 1, clock);
        LoginRateLimiter.Attempt active =
                limiter.beginAttempt("one@example.com", "192.0.2.1");

        LoginRateLimitExceededException exception = assertThrows(
                LoginRateLimitExceededException.class,
                () -> limiter.beginAttempt(
                        "two@example.com", "192.0.2.2"));
        assertEquals(1, exception.getRetryAfterSeconds());

        active.recordSuccess();
        try (LoginRateLimiter.Attempt admitted =
                     limiter.beginAttempt(
                             "two@example.com", "192.0.2.2")) {
            admitted.recordSuccess();
        }
    }

    private static LoginRateLimiter limiter(
            int emailLimit,
            int ipLimit,
            int maxKeys,
            MutableClock clock) {
        return new LoginRateLimiter(
                emailLimit,
                ipLimit,
                Duration.ofMinutes(15),
                maxKeys,
                clock);
    }

    private static void failAttempt(
            LoginRateLimiter limiter,
            String email,
            String ip) {
        try (LoginRateLimiter.Attempt attempt =
                     limiter.beginAttempt(email, ip)) {
            attempt.recordFailure();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public long millis() {
            return instant.toEpochMilli();
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
