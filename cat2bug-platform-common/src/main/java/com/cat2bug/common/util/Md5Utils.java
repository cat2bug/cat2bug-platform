package com.cat2bug.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Md5Utils {

    private Md5Utils() {
    }

    public static String hash(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(value.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xff) < 0x10) {
                    builder.append('0');
                }
                builder.append(Integer.toHexString(b & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            return value;
        }
    }
}
