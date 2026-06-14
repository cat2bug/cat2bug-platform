package com.cat2bug.common.utils;

/**
 * GraalVM Native Image 运行时探测。
 */
public final class NativeImageSupport
{
    private NativeImageSupport()
    {
    }

    public static boolean isRunningNativeImage()
    {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }
}
