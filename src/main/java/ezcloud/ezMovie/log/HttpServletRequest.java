package ezcloud.ezMovie.log;

import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    public HttpServletRequest(jakarta.servlet.http.HttpServletRequest request) throws IOException {
        super(request);
        InputStream inputStream = request.getInputStream();
        this.body = StreamUtils.copyToByteArray(inputStream);
    }

    @Override
    public jakarta.servlet.ServletInputStream getInputStream() {
        return new ServletInputStream(body);
    }

    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
