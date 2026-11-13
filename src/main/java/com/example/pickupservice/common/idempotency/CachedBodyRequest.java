package com.example.pickupservice.common.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class CachedBodyRequest extends HttpServletRequestWrapper {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = isMultipart(request) ? null : request.getInputStream().readAllBytes();
    }

    boolean multipart() {
        return body == null;
    }

    byte[] body() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (multipart()) {
            return super.getInputStream();
        }
        return new CachedBodyInputStream(new ByteArrayInputStream(body));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (multipart()) {
            return super.getReader();
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding()));
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(MULTIPART_FORM_DATA);
    }

    private Charset characterEncoding() {
        String encoding = getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    private static final class CachedBodyInputStream extends ServletInputStream {

        private final ByteArrayInputStream source;

        private CachedBodyInputStream(ByteArrayInputStream source) {
            this.source = source;
        }

        @Override
        public int read() {
            return source.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            return source.read(buffer, offset, length);
        }

        @Override
        public boolean isFinished() {
            return source.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

    }

}
