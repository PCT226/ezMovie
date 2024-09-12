package ezcloud.ezMovie.log;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;

public class CachedBodyServletOutputStream extends ServletOutputStream {
    private final OutputStream outputStream;

    public CachedBodyServletOutputStream(OutputStream outputStream) {
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
