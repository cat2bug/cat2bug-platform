package com.cat2bug.admin.support;

import com.cat2bug.common.web.BinaryHttpResponse;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓冲写出后转为 JAX-RS {@link StreamingOutput}（含 Content-Type / 附件头）。
 */
public final class JaxrsBinaryHttpResponse implements BinaryHttpResponse {

    private String contentType = "application/octet-stream";
    private final Map<String, String> headers = new LinkedHashMap<>();
    private Long contentLength;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void setContentLengthLong(long length) {
        this.contentLength = length;
    }

    @Override
    public OutputStream getOutputStream() {
        return buffer;
    }

    public String getContentType() {
        return contentType;
    }

    public void applyHeaders(MultivaluedMap<String, Object> target) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            target.putSingle(e.getKey(), e.getValue());
        }
        if (contentLength != null) {
            target.putSingle("Content-Length", contentLength);
        }
    }

    public StreamingOutput toStreamingOutput() {
        byte[] bytes = buffer.toByteArray();
        return output -> {
            output.write(bytes);
            output.flush();
        };
    }

    public byte[] toByteArray() {
        return buffer.toByteArray();
    }
}
