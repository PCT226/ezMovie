package ezcloud.ezMovie.log;

import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class HttpServletResponse extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final jakarta.servlet.ServletOutputStream servletOutputStream;

    public HttpServletResponse(jakarta.servlet.http.HttpServletResponse response) {
        super(response);
        this.servletOutputStream = new ServletOutputStream(byteArrayOutputStream);
    }

    @Override
    public jakarta.servlet.ServletOutputStream getOutputStream() {
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8), true);
    }

    public String getBody() {
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }
}

