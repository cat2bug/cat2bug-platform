package com.aayushatharva.brotli4j.encoder;

public class Encoder {

    public enum Mode {
        GENERIC,
        TEXT,
        FONT
    }

    public static final class Parameters {

        public Parameters setQuality(int quality) {
            return this;
        }

        public Parameters setWindow(int window) {
            return this;
        }

        public Parameters setMode(Mode mode) {
            return this;
        }
    }
}
