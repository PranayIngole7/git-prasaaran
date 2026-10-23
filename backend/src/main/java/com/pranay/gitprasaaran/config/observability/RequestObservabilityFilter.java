package com.pranay.gitprasaaran.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestObservabilityFilter.class);

    private final MeterRegistry meterRegistry;

    public RequestObservabilityFilter(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || !traceId.matches("[A-Za-z0-9._-]{1,64}")) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        String method = normalizeMethod(request.getMethod());
        String endpoint = normalizeEndpoint(request.getRequestURI());
        int status = HttpServletResponse.SC_OK;
        long start = System.nanoTime();

        boolean requestFailed = false;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            requestFailed = true;
            status = response.getStatus();
            if (status < 400) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            throw exception;
        } finally {
            if (!requestFailed) {
                status = response.getStatus();
            }
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            meterRegistry.counter(
                            "git_prasaaran.http.requests.total",
                            "method", method,
                            "endpoint", endpoint,
                            "status", String.valueOf(status))
                    .increment();

            meterRegistry.timer(
                            "git_prasaaran.http.request.latency",
                            "method", method,
                            "endpoint", endpoint)
                    .record(latencyMs, TimeUnit.MILLISECONDS);

            if (status >= 500) {
                meterRegistry.counter(
                                "git_prasaaran.http.errors.total",
                                "method", method,
                                "endpoint", endpoint)
                        .increment();
            }

            LOGGER.info(
                    "event=request_complete method={} endpoint={} status={} trace_id={} latency_ms={}",
                    method,
                    endpoint,
                    status,
                    traceId,
                    latencyMs
            );

            MDC.remove("traceId");
        }
    }

    private String normalizeMethod(String method) {
        if (method == null) {
            return "OTHER";
        }

        return switch (method) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD" -> method;
            default -> "OTHER";
        };
    }

    private String normalizeEndpoint(String uri) {
        if (uri == null || uri.isBlank()) {
            return "OTHER";
        }

        String safe = uri.replaceAll("/\\d+", "/{id}");
        if (safe.matches("^/api/v1/documents/.+$")) {
            return "/api/v1/documents/{slug}";
        }
        if (safe.matches("^/api/v1/private-documents/.+$")) {
            return "/api/v1/private-documents/{id}";
        }
        if (safe.matches("^/api/v1/repositories/.+$")) {
            return "/api/v1/repositories/{id}";
        }
        if (safe.matches("^/api/v1/activity/.+$")) {
            return "/api/v1/activity/{id}";
        }
        return "OTHER";
    }
}
