package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.port.out.RateLimitDecision;
import com.norbertfila.hashtune.application.port.out.RequestRateLimiter;
import com.norbertfila.hashtune.application.service.TooManyRequestsException;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RequestRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(true);
        RateLimitDecision decision = rateLimiter.check(
                new ClientSessionId(session.getId()), request.getRemoteAddr(), isExpensiveRequest(request));
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
