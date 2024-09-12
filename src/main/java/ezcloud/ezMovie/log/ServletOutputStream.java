package ezcloud.ezMovie.log;

import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;

public class ServletOutputStream extends jakarta.servlet.ServletOutputStream {
    private final OutputStream outputStream;

    public ServletOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
    }
}
