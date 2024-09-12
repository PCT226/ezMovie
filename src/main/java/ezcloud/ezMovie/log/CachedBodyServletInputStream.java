package ezcloud.ezMovie.log;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CachedBodyServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream byteArrayInputStream;

    public CachedBodyServletInputStream(byte[] body) {
        this.byteArrayInputStream = new ByteArrayInputStream(body);
    }

    @Override
    public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
    }

    @Override
    public int read() throws IOException {
        return byteArrayInputStream.read();
    }
}
