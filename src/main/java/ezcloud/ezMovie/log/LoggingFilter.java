//package ezcloud.ezMovie.log;
//
//import jakarta.servlet.Filter;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.FilterConfig;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import jakarta.servlet.annotation.WebFilter;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.time.Instant;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Component
//@WebFilter("/*")
//public class LoggingFilter implements Filter {
//
//    private final LoggingService loggingService;
//    List<String> excludedApis = Arrays.asList("/api/health", "/swagger-ui", "/v3/api-docs");
//
//    @Autowired
//    public LoggingFilter(LoggingService loggingService) {
//        this.loggingService = loggingService;
//    }
//
//    @Override
//    public void init(FilterConfig filterConfig) {
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        jakarta.servlet.http.HttpServletRequest httpRequest = (jakarta.servlet.http.HttpServletRequest) request;
//        jakarta.servlet.http.HttpServletResponse httpResponse = (jakarta.servlet.http.HttpServletResponse) response;
//
//        String requestURI = httpRequest.getRequestURI();
//
//        boolean shouldExclude = excludedApis.stream().anyMatch(uri -> requestURI.startsWith(uri));
//        if (shouldExclude) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        String traceId = httpRequest.getHeader("X-B3-TraceId");
//        if (traceId == null || traceId.isEmpty()) {
//            traceId = UUID.randomUUID().toString();
//        }
//
//        MDC.put("traceId", traceId);
//
//        try {
//            HttpServletRequest wrappedRequest = new HttpServletRequest(httpRequest);
//            HttpServletResponse wrappedResponse = new HttpServletResponse(httpResponse);
//
//            chain.doFilter(wrappedRequest, wrappedResponse);
//
//            String method = wrappedRequest.getMethod();
//            String url = wrappedRequest.getRequestURI();
//            String clientIp = wrappedRequest.getRemoteAddr();
//            String requestHeaders = Collections.list(wrappedRequest.getHeaderNames()).stream()
//                    .map(header -> header + ": " + wrappedRequest.getHeader(header))
//                    .collect(Collectors.joining(", "));
//
//            String requestBody = url.contains("/api/v1/auth") ? "authen" : wrappedRequest.getBody();
//
//            int statusCode = wrappedResponse.getStatus();
//            String responseHeaders = wrappedResponse.getHeaderNames().stream()
//                    .map(header -> header + ": " + wrappedResponse.getHeader(header))
//                    .collect(Collectors.joining(", "));
//            String responseBody = wrappedResponse.getBody();
//
//            String timestamp = Instant.now().toString();
//
//            loggingService.logToElasticsearch(
//                    method, url, clientIp, requestHeaders, requestBody, statusCode, responseHeaders, responseBody, timestamp
//            );
//
//            httpResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
//        } finally {
//            MDC.remove("traceId");
//        }
//    }
//
//    @Override
//    public void destroy() {
//    }
//}
