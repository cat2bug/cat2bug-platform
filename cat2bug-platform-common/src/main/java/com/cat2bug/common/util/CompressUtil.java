package com.cat2bug.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 打包工具（项目云端同步等场景）。
 */
public class CompressUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ZipOutputStream outputStream;
    private final Set<String> outputStreamFileNames = new HashSet<>();

    public static CompressUtil createStream(String fileName) throws FileNotFoundException {
        return new CompressUtil(fileName);
    }

    private CompressUtil(String fileName) throws FileNotFoundException {
        this.outputStream = new ZipOutputStream(new FileOutputStream(fileName));
    }

    public void build() throws IOException {
        outputStream.close();
    }

    public CompressUtil addJsonFile(String fileName, Object obj) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        outputStream.putNextEntry(entry);
        byte[] buffer = MAPPER.writeValueAsBytes(obj);
        outputStream.write(buffer, 0, buffer.length);
        outputStream.closeEntry();
        return this;
    }

    public CompressUtil addUrlFile(String fileName, String url) throws IOException {
        if (outputStreamFileNames.contains(fileName)) {
            return this;
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return this;
        }
        byte[] bytes = response.body();
        ZipEntry entry = new ZipEntry(fileName);
        outputStream.putNextEntry(entry);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.closeEntry();
        outputStreamFileNames.add(fileName);
        return this;
    }
}
