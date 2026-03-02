package np.com.nepalupi.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Wraps HttpServletRequest to cache the body so it can be read multiple times —
 * required for HMAC signature verification (body is read for signature check,
 * then again by Spring for deserialization).
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream is = request.getInputStream();
        this.cachedBody = is.readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8));
    }

    public String getCachedBody() {
        return new String(this.cachedBody, StandardCharsets.UTF_8);
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // Not needed for cached body
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
