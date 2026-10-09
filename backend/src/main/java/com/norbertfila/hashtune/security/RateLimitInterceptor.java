package com.norbertfila.hashtune.security;

import com.norbertfila.hashtune.security.AuthenticatedIdentityResolver;
import com.norbertfila.hashtune.security.RateLimitDecision;
import com.norbertfila.hashtune.security.RequestRateLimiter;
import com.norbertfila.hashtune.exceptions.application.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RequestRateLimiter rateLimiter;
    private final AuthenticatedIdentityResolver identityResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        var identity = identityResolver.resolveOptional(SecurityContextHolder.getContext().getAuthentication());
        RateLimitDecision decision = rateLimiter.check(identity, request.getRemoteAddr(), isExpensiveRequest(request));
        if (!decision.allowed()) {
            throw new TooManyRequestsException(decision.retryAfter());
        }
        return true;
    }

    private boolean isExpensiveRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/api/v1/recognitions");
    }
}
