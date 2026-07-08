package com.courierapp.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Wraps the request body and strips HTML tags / dangerous script patterns from all string values.
 * Applied to all non-multipart POST/PUT/PATCH requests.
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] sanitizedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        byte[] raw = request.getInputStream().readAllBytes();
        String body = new String(raw, StandardCharsets.UTF_8);
        String clean = sanitize(body);
        this.sanitizedBody = clean.getBytes(StandardCharsets.UTF_8);
    }

    static String sanitize(String value) {
        if (value == null) return null;
        // Remove script tags and event handlers
        String result = value
                .replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("(?i)<[^>]*(on\\w+)\\s*=\\s*[\"'][^\"']*[\"'][^>]*>", "")
                .replaceAll("(?i)javascript\\s*:", "")
                .replaceAll("(?i)vbscript\\s*:", "")
                .replaceAll("(?i)expression\\s*\\(", "")
                .replaceAll("(?i)<iframe[^>]*>", "")
                .replaceAll("(?i)</iframe>", "")
                .replaceAll("(?i)<object[^>]*>", "")
                .replaceAll("(?i)<embed[^>]*>", "");
        return result;
    }

    @Override
    public String getParameter(String name) {
        String val = super.getParameter(name);
        return val != null ? sanitize(val) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = sanitize(values[i]);
        }
        return sanitized;
    }

    @Override
    public String getHeader(String name) {
        String val = super.getHeader(name);
        // Don't sanitize Authorization or Content-Type headers
        if ("authorization".equalsIgnoreCase(name) || "content-type".equalsIgnoreCase(name)) {
            return val;
        }
        return val != null ? sanitize(val) : null;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(sanitizedBody);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener rl) {}
            @Override public int read() { return bais.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
