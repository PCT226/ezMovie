package ezcloud.ezMovie.log;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private ServletOutputStream servletOutputStream;

    public CachedBodyHttpServletResponse(HttpServletResponse response) throws IOException {
        super(response);
        this.servletOutputStream = new CachedBodyServletOutputStream(byteArrayOutputStream);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8), true);
    }

    public String getBody() {
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }
}

