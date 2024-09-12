package ezcloud.ezMovie.log;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream inputStream = request.getInputStream();
        this.body = StreamUtils.copyToByteArray(inputStream);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(body);
    }

    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
