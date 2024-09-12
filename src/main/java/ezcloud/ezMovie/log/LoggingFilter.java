package ezcloud.ezMovie.log;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@WebFilter("/*")
public class LoggingFilter implements Filter {

    private final ElasticsearchLogger elasticsearchLogger;

    @Autowired
    public LoggingFilter(ElasticsearchLogger elasticsearchLogger) {
        this.elasticsearchLogger = elasticsearchLogger;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Danh sách các API muốn bỏ qua
        List<String> excludedApis = Arrays.asList("/api/health", "/swagger-ui", "/v3/api-docs");

        String requestURI = httpRequest.getRequestURI();

        boolean shouldExclude = excludedApis.stream().anyMatch(uri -> requestURI.startsWith(uri));

        if (shouldExclude) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
        CachedBodyHttpServletResponse wrappedResponse = new CachedBodyHttpServletResponse(httpResponse);

        chain.doFilter(wrappedRequest, wrappedResponse);

        // Lấy thông tin từ request
        String method = wrappedRequest.getMethod();
        String url = wrappedRequest.getRequestURI();
        String clientIp = wrappedRequest.getRemoteAddr();
        String requestHeaders = Collections.list(wrappedRequest.getHeaderNames()).stream()
                .map(header -> header + ": " + wrappedRequest.getHeader(header))
                .collect(Collectors.joining(", "));
        String requestBody = wrappedRequest.getBody();

        // Lấy thông tin từ response
        int statusCode = wrappedResponse.getStatus();
        String responseHeaders = wrappedResponse.getHeaderNames().stream()
                .map(header -> header + ": " + wrappedResponse.getHeader(header))
                .collect(Collectors.joining(", "));
        String responseBody = wrappedResponse.getBody();

        // Lấy thời gian hiện tại (timestamp)
        String timestamp = Instant.now().toString();

        // Ghi log vào Elasticsearch với các thông tin chi tiết
        elasticsearchLogger.logToElasticsearch(
                method, url, clientIp, requestHeaders, requestBody, statusCode, responseHeaders, responseBody, timestamp
        );

        // Copy nội dung đã ghi trong wrappedResponse sang httpResponse thực tế
        httpResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
    }
}
