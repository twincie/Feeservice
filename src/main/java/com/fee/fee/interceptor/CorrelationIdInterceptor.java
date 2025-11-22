package com.fee.fee.interceptor;

import com.fee.fee.Util.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdInterceptor implements HandlerInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String correlationId = getCorrelationIdFromHeader(request);
        CorrelationIdContext.setCorrelationId(correlationId);

        log.info("Incoming request [{}] {} {} - Correlation ID: {}",
                request.getMethod(), request.getRequestURI(),
                getClientInfo(request), correlationId);

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        log.info("Completed request [{}] {} - Status: {} - Correlation ID: {}",
                request.getMethod(), request.getRequestURI(),
                response.getStatus(), CorrelationIdContext.getCorrelationId());

        CorrelationIdContext.clear();
    }

    private String getCorrelationIdFromHeader(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getClientInfo(HttpServletRequest request) {
        return String.format("(Client: %s)", request.getRemoteAddr());
    }
}