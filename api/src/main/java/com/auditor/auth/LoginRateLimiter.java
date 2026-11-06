package com.auditor.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class LoginRateLimiter {

    private final int maxEmailFailures;
    private final int maxIpFailures;
    private final long windowMillis;
    private final int maxKeys;
    private final Clock clock;
    private final Map<String, Window> emailWindows = new LinkedHashMap<>();
    private final Map<String, Window> ipWindows = new LinkedHashMap<>();
    private int operationsSinceCleanup;

    @Autowired
    public LoginRateLimiter(
            @Value("${app.security.login-rate-limit.email-failures:5}")
            int maxEmailFailures,
            @Value("${app.security.login-rate-limit.ip-failures:30}")
            int maxIpFailures,
            @Value("${app.security.login-rate-limit.window-seconds:900}")
            long windowSeconds,
            @Value("${app.security.login-rate-limit.max-keys:10000}")
            int maxKeys) {
        this(
                maxEmailFailures,
                maxIpFailures,
                Duration.ofSeconds(windowSeconds),
                maxKeys,
                Clock.systemUTC());
    }

    LoginRateLimiter(
            int maxEmailFailures,
            int maxIpFailures,
            Duration window,
            int maxKeys,
            Clock clock) {
        if (maxEmailFailures < 1
                || maxIpFailures < 1
                || window.isZero()
                || window.isNegative()
                || maxKeys < 1) {
            throw new IllegalArgumentException(
                    "Login rate-limit configuration must be positive.");
        }

        this.maxEmailFailures = maxEmailFailures;
        this.maxIpFailures = maxIpFailures;
        this.windowMillis = window.toMillis();
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    public synchronized Attempt beginAttempt(
            String normalizedEmail,
            String clientIp) {
        long now = clock.millis();
        cleanupOccasionally(now);

        String emailKey = normalizedEmail;
        String ipKey = clientIp == null || clientIp.isBlank()
                ? "unknown"
                : clientIp;

        ensureCapacity(emailWindows, emailKey, now);
        ensureCapacity(ipWindows, ipKey, now);

        Window emailWindow =
                emailWindows.computeIfAbsent(emailKey, ignored -> new Window());
        Window ipWindow =
                ipWindows.computeIfAbsent(ipKey, ignored -> new Window());
        emailWindow.prune(now, windowMillis);
        ipWindow.prune(now, windowMillis);

        long emailAvailableAt = emailWindow.availableAt(
                now, windowMillis, maxEmailFailures);
        long ipAvailableAt = ipWindow.availableAt(
                now, windowMillis, maxIpFailures);
        if (emailAvailableAt > now || ipAvailableAt > now) {
            throw new LoginRateLimitExceededException(
                    retryAfterSeconds(Math.max(
                            emailAvailableAt, ipAvailableAt), now));
        }

        UUID reservationId = UUID.randomUUID();
        emailWindow.reservations.add(reservationId);
        ipWindow.reservations.add(reservationId);
        return new Attempt(
                this, emailKey, ipKey, reservationId);
    }

    private void cleanupOccasionally(long now) {
        operationsSinceCleanup++;
        if (operationsSinceCleanup >= 256) {
            pruneMap(emailWindows, now);
            pruneMap(ipWindows, now);
            operationsSinceCleanup = 0;
        }
    }

    private void ensureCapacity(
            Map<String, Window> windows,
            String key,
            long now) {
        Window existing = windows.get(key);
        if (existing != null) {
            existing.prune(now, windowMillis);
            return;
        }

        if (windows.size() >= maxKeys) {
            pruneMap(windows, now);
        }

        if (windows.size() >= maxKeys) {
            throw new LoginRateLimitExceededException(
                    retryAfterSeconds(nextKeyExpiry(windows, now), now));
        }
    }

    private void pruneMap(Map<String, Window> windows, long now) {
        Iterator<Window> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next();
            window.prune(now, windowMillis);
            if (window.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private long nextKeyExpiry(Map<String, Window> windows, long now) {
        long earliestEmptyAt = Long.MAX_VALUE;
        for (Window window : windows.values()) {
            long lastExpiry = window.lastExpiry(windowMillis, now);
            if (lastExpiry < earliestEmptyAt) {
                earliestEmptyAt = lastExpiry;
            }
        }
        return earliestEmptyAt;
    }

    private long retryAfterSeconds(long availableAt, long now) {
        if (availableAt == Long.MAX_VALUE) {
            return Math.max(1, Duration.ofMillis(windowMillis).toSeconds());
        }
        long remainingMillis = Math.max(1, availableAt - now);
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    private synchronized void finish(Attempt attempt, Outcome outcome) {
        long now = clock.millis();
        Window emailWindow = emailWindows.get(attempt.emailKey);
        Window ipWindow = ipWindows.get(attempt.ipKey);

        if (emailWindow == null || ipWindow == null) {
            throw new IllegalStateException(
                    "Login rate-limit reservation is no longer available.");
        }

        boolean emailReservationRemoved =
                emailWindow.reservations.remove(attempt.reservationId);
        boolean ipReservationRemoved =
                ipWindow.reservations.remove(attempt.reservationId);
        if (!emailReservationRemoved || !ipReservationRemoved) {
            throw new IllegalStateException(
                    "Login rate-limit reservation was already finalized.");
        }

        emailWindow.prune(now, windowMillis);
        ipWindow.prune(now, windowMillis);

        if (outcome == Outcome.FAILURE) {
            emailWindow.failures.addLast(now);
            ipWindow.failures.addLast(now);
        } else if (outcome == Outcome.SUCCESS) {
            emailWindow.failures.clear();
        }

        if (emailWindow.isEmpty()) {
            emailWindows.remove(attempt.emailKey);
        }
        if (ipWindow.isEmpty()) {
            ipWindows.remove(attempt.ipKey);
        }
    }

    synchronized int trackedEmailKeyCount() {
        return emailWindows.size();
    }

    synchronized int trackedIpKeyCount() {
        return ipWindows.size();
    }

    private enum Outcome {
        FAILURE,
        SUCCESS,
        CANCELLED
    }

    public static final class Attempt implements AutoCloseable {

        private final LoginRateLimiter limiter;
        private final String emailKey;
        private final String ipKey;
        private final UUID reservationId;
        private boolean completed;

        private Attempt(
                LoginRateLimiter limiter,
                String emailKey,
                String ipKey,
                UUID reservationId) {
            this.limiter = limiter;
            this.emailKey = emailKey;
            this.ipKey = ipKey;
            this.reservationId = reservationId;
        }

        public void recordFailure() {
            complete(Outcome.FAILURE);
        }

        public void recordSuccess() {
            complete(Outcome.SUCCESS);
        }

        private void complete(Outcome outcome) {
            if (completed) {
                throw new IllegalStateException(
                        "Login rate-limit attempt is already complete.");
            }
            limiter.finish(this, outcome);
            completed = true;
        }

        @Override
        public void close() {
            if (!completed) {
                limiter.finish(this, Outcome.CANCELLED);
                completed = true;
            }
        }
    }

    private static final class Window {

        private final Deque<Long> failures = new ArrayDeque<>();
        private final Set<UUID> reservations = new HashSet<>();

        private void prune(long now, long windowMillis) {
            long cutoff = now - windowMillis;
            while (!failures.isEmpty() && failures.peekFirst() <= cutoff) {
                failures.removeFirst();
            }
        }

        private long availableAt(
                long now,
                long windowMillis,
                int limit) {
            int currentCount = failures.size() + reservations.size();
            if (currentCount < limit) {
                return now;
            }

            List<Long> expiries = new ArrayList<>(failures.size());
            for (Long failure : failures) {
                expiries.add(failure + windowMillis);
            }
            expiries.sort(Long::compareTo);
            int requiredToExpire = currentCount - limit + 1;
            if (requiredToExpire > expiries.size()) {
                return now + 1000;
            }
            return expiries.get(requiredToExpire - 1);
        }

        private long lastExpiry(long windowMillis, long now) {
            long latestExpiry = Long.MIN_VALUE;
            for (Long failure : failures) {
                latestExpiry = Math.max(
                        latestExpiry, failure + windowMillis);
            }
            if (!reservations.isEmpty()) {
                if (failures.isEmpty()) {
                    // Polling hint only; a reservation may finish sooner,
                    // but this does not guarantee capacity will be free.
                    return now + 1000;
                }
            }
            return latestExpiry;
        }

        private boolean isEmpty() {
            return failures.isEmpty() && reservations.isEmpty();
        }
    }
}
