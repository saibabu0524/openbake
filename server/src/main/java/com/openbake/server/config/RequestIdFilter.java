package com.openbake.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Mirrors main.py's request_middleware: request-id + timing headers, one log line per request. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private static final Logger log = LoggerFactory.getLogger("request");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        long start = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            double elapsedMs = Math.round((System.nanoTime() - start) / 1_000_00.0) / 10.0;
            response.setHeader("X-Request-ID", requestId);
            response.setHeader("X-Response-Time", elapsedMs + "ms");
            log.info("request_id={} method={} path={} status={} duration_ms={}",
                    requestId, request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }
}
