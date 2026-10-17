package com.aayushatharva.brotli4j.encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class BrotliEncoderChannel {

    public BrotliEncoderChannel(WritableByteChannel destination, Encoder.Parameters parameters) {
    }

    public int write(ByteBuffer src) throws IOException {
        return 0;
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }
}
