package com.aayushatharva.brotli4j.decoder;

import java.nio.ByteBuffer;

public class DecoderJNI {

    public enum Status {
        ERROR,
        DONE,
        NEEDS_MORE_INPUT,
        NEEDS_MORE_OUTPUT,
        OK
    }

    public static final class Wrapper {

        public Wrapper(int bufferSize) {
        }

        public void push(int length) {
        }

        public void destroy() {
        }

        public Status getStatus() {
            return Status.ERROR;
        }

        public boolean hasOutput() {
            return false;
        }

        public ByteBuffer pull(int length) {
            return ByteBuffer.allocate(0);
        }

        public ByteBuffer getInputBuffer() {
            return ByteBuffer.allocate(0);
        }
    }
}
