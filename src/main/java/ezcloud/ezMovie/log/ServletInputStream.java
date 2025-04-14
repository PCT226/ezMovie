package ezcloud.ezMovie.log;

import jakarta.servlet.ReadListener;

import java.io.ByteArrayInputStream;

public class ServletInputStream extends jakarta.servlet.ServletInputStream {
    private final ByteArrayInputStream byteArrayInputStream;

    public ServletInputStream(byte[] body) {
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
    public int read() {
        return byteArrayInputStream.read();
    }
}
