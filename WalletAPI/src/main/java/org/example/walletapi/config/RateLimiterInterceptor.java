package org.example.walletapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterInterceptor() {
        Duration duration = Duration.ofHours(1);
        long capacity = 1000;

        Bandwidth limit = Bandwidth.simple(capacity, duration);
        this.buckets.put("default", Bucket4j.builder()
                .addLimit(limit)
                .build());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ipAddress = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> {
            Bandwidth limit = Bandwidth.simple(1000, Duration.ofHours(1));
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        });

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return true;
        } else {
            response.setStatus(429);
            return false;
        }
    }
}