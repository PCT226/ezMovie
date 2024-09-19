package ezcloud.ezMovie.log.service;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoggingService {

    private final RestHighLevelClient restHighLevelClient;

    @Autowired
    public LoggingService(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    public void logToElasticsearch(String method, String url, String clientIp,
                                   String requestHeaders, String requestBody, int statusCode,
                                   String responseHeaders, String responseBody, String timestamp) {

        // Lấy traceId từ MDC
        String traceId = MDC.get("traceId");

        Map<String, Object> log = new HashMap<>();
        log.put("traceId", traceId);  // Gán traceId vào log
        log.put("method", method);
        log.put("url", url);
        log.put("client_ip", clientIp);
        log.put("request_headers", requestHeaders);
        log.put("request_body", requestBody);
        log.put("status_code", statusCode);
        log.put("response_headers", responseHeaders);
        log.put("response_body", responseBody);
        log.put("timestamp", timestamp);

        IndexRequest indexRequest = new IndexRequest("logs")
                .source(log);

        try {
            IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println("Log indexed with ID: " + indexResponse.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
