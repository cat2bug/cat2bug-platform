package com.aayushatharva.brotli4j;

/**
 * 替代 brotli4j。Netty 的 HTTP 压缩器在类签名里引用了这些类型，
 * 但默认压缩只有 gzip/deflate，运行时不加载原生 brotli 库。
 */
public final class Brotli4jLoader {

    private static final Throwable UNAVAILABLE = new UnsatisfiedLinkError("brotli disabled");

    private Brotli4jLoader() {
    }

    public static boolean isAvailable() {
        return false;
    }

    public static void ensureAvailability() {
        throw new UnsatisfiedLinkError("brotli disabled");
    }

    public static Throwable getUnavailabilityCause() {
        return UNAVAILABLE;
    }
}
