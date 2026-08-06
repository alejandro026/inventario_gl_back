package com.guerrero.Inventario.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(Integer.MIN_VALUE + 1) // Ejecutar justo despues de TraceIdFilter para que el MDC ya este inicializado
public class RequestLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String method = httpRequest.getMethod();
            String uri = httpRequest.getRequestURI();
            String clientIp = getClientIP(httpRequest);

            boolean isSwagger = uri.contains("swagger") || uri.contains("api-docs") || uri.contains("webjars");
            
            if (!isSwagger) {
                // Sanitizar endpoints de autenticacion (no loguear parametros del query string por privacidad)
                if (uri.contains("auth/login") || uri.contains("auth/register") || uri.contains("auth/refresh") || uri.contains("auth/logout")) {
                    log.info("[START] {} {} | IP: {}", method, uri, clientIp);
                } else {
                    String query = httpRequest.getQueryString();
                    String fullPath = (query != null && !query.isEmpty()) ? uri + "?" + query : uri;
                    log.info("[START] {} {} | IP: {}", method, fullPath, clientIp);
                }
            }

            long startTime = System.currentTimeMillis();
            try {
                chain.doFilter(request, response);
            } finally {
                if (!isSwagger) {
                    long duration = System.currentTimeMillis() - startTime;
                    int status = httpResponse.getStatus();
                    log.info("[END] {} {} | Status: {} | Duration: {}ms", method, uri, status, duration);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
