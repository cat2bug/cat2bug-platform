package com.cat2bug.web.captcha;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * 无 AWT 的验证码 PNG 渲染，供 Native 镜像避免拉入 font/2D 子系统。
 * 移动端仍通过 {@code img} 字段（Base64 PNG）展示验证码。
 */
public final class CaptchaPngRenderer {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int BACKGROUND = 0xFFFFFF;
    private static final int FOREGROUND = 0x409EFF;

    /** 5×7 点阵，行内从左到右为高位到低位（仅覆盖验证码字符集）。 */
    private static final Map<Character, int[]> GLYPHS = Map.ofEntries(
            entry('A', rows("01110", "10001", "10001", "11111", "10001", "10001", "10001")),
            entry('B', rows("11110", "10001", "10001", "11110", "10001", "10001", "11110")),
            entry('C', rows("01111", "10000", "10000", "10000", "10000", "10000", "01111")),
            entry('D', rows("11110", "10001", "10001", "10001", "10001", "10001", "11110")),
            entry('E', rows("11111", "10000", "10000", "11110", "10000", "10000", "11111")),
            entry('F', rows("11111", "10000", "10000", "11110", "10000", "10000", "10000")),
            entry('G', rows("01111", "10000", "10000", "10011", "10001", "10001", "01111")),
            entry('H', rows("10001", "10001", "10001", "11111", "10001", "10001", "10001")),
            entry('J', rows("00111", "00010", "00010", "00010", "00010", "10010", "01100")),
            entry('K', rows("10001", "10010", "10100", "11000", "10100", "10010", "10001")),
            entry('M', rows("10001", "11011", "10101", "10001", "10001", "10001", "10001")),
            entry('N', rows("10001", "11001", "10101", "10011", "10001", "10001", "10001")),
            entry('P', rows("11110", "10001", "10001", "11110", "10000", "10000", "10000")),
            entry('Q', rows("01110", "10001", "10001", "10001", "10101", "10010", "01101")),
            entry('R', rows("11110", "10001", "10001", "11110", "10100", "10010", "10001")),
            entry('S', rows("01111", "10000", "10000", "01110", "00001", "00001", "11110")),
            entry('T', rows("11111", "00100", "00100", "00100", "00100", "00100", "00100")),
            entry('U', rows("10001", "10001", "10001", "10001", "10001", "10001", "01110")),
            entry('V', rows("10001", "10001", "10001", "10001", "10001", "01010", "00100")),
            entry('W', rows("10001", "10001", "10001", "10001", "10101", "11011", "10001")),
            entry('X', rows("10001", "10001", "01010", "00100", "01010", "10001", "10001")),
            entry('Y', rows("10001", "10001", "01010", "00100", "00100", "00100", "00100")),
            entry('Z', rows("11111", "00001", "00010", "00100", "01000", "10000", "11111")),
            entry('a', rows("00000", "00000", "01110", "00001", "01111", "10001", "01111")),
            entry('b', rows("10000", "10000", "10110", "11001", "10001", "10001", "11110")),
            entry('c', rows("00000", "00000", "01110", "10000", "10000", "10001", "01110")),
            entry('d', rows("00001", "00001", "01101", "10011", "10001", "10011", "01101")),
            entry('e', rows("00000", "00000", "01110", "10001", "11111", "10000", "01110")),
            entry('f', rows("00110", "01001", "01000", "11100", "01000", "01000", "01000")),
            entry('g', rows("00000", "00000", "01101", "10011", "10001", "01111", "00001", "01110")),
            entry('h', rows("10000", "10000", "10110", "11001", "10001", "10001", "10001")),
            entry('j', rows("00010", "00000", "00110", "00010", "00010", "10010", "01100")),
            entry('k', rows("10000", "10000", "10010", "10100", "11000", "10100", "10010")),
            entry('m', rows("00000", "00000", "11010", "10101", "10101", "10001", "10001")),
            entry('n', rows("00000", "00000", "10110", "11001", "10001", "10001", "10001")),
            entry('p', rows("00000", "00000", "11110", "10001", "10001", "11110", "10000")),
            entry('q', rows("00000", "00000", "01101", "10011", "10001", "01111", "00001")),
            entry('r', rows("00000", "00000", "10110", "11001", "10000", "10000", "10000")),
            entry('s', rows("00000", "00000", "01110", "10000", "01100", "00010", "11100")),
            entry('t', rows("01000", "01000", "11110", "01000", "01000", "01001", "00110")),
            entry('u', rows("00000", "00000", "10001", "10001", "10001", "10011", "01101")),
            entry('v', rows("00000", "00000", "10001", "10001", "10001", "01010", "00100")),
            entry('w', rows("00000", "00000", "10001", "10001", "10101", "10101", "01010")),
            entry('x', rows("00000", "00000", "10001", "01010", "00100", "01010", "10001")),
            entry('y', rows("00000", "00000", "10001", "10001", "10001", "01111", "00001", "11110")),
            entry('z', rows("00000", "00000", "11111", "00010", "00100", "01000", "11111")),
            entry('2', rows("01110", "10001", "00001", "00010", "00100", "01000", "11111")),
            entry('3', rows("01110", "10001", "00001", "00110", "00001", "10001", "01110")),
            entry('4', rows("00010", "00110", "01010", "10010", "11111", "00010", "00010")),
            entry('5', rows("11111", "10000", "11110", "00001", "00001", "10001", "01110")),
            entry('6', rows("01110", "10000", "10000", "11110", "10001", "10001", "01110")),
            entry('7', rows("11111", "00001", "00010", "00100", "01000", "01000", "01000")),
            entry('8', rows("01110", "10001", "10001", "01110", "10001", "10001", "01110")),
            entry('9', rows("01110", "10001", "10001", "01111", "00001", "00001", "01110"))
    );

    private CaptchaPngRenderer() {
    }

    public static String renderBase64Png(String code) {
        int[] pixels = new int[WIDTH * HEIGHT];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = BACKGROUND;
        }
        int x = 14;
        for (char ch : code.toCharArray()) {
            drawGlyph(pixels, x, 10, ch);
            x += 24;
        }
        return Base64.getEncoder().encodeToString(encodePng(WIDTH, HEIGHT, pixels));
    }

    private static void drawGlyph(int[] pixels, int originX, int originY, char ch) {
        int[] rows = GLYPHS.get(ch);
        if (rows == null) {
            return;
        }
        for (int row = 0; row < rows.length; row++) {
            int bits = rows[row];
            for (int col = 0; col < 5; col++) {
                if (((bits >> (4 - col)) & 1) == 1) {
                    setPixel(pixels, originX + col * 2, originY + row * 2, FOREGROUND);
                    setPixel(pixels, originX + col * 2 + 1, originY + row * 2, FOREGROUND);
                    setPixel(pixels, originX + col * 2, originY + row * 2 + 1, FOREGROUND);
                    setPixel(pixels, originX + col * 2 + 1, originY + row * 2 + 1, FOREGROUND);
                }
            }
        }
    }

    private static void setPixel(int[] pixels, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
            return;
        }
        pixels[y * WIDTH + x] = rgb;
    }

    private static byte[] encodePng(int width, int height, int[] rgb) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        writeBytes(out, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        writeChunk(out, "IHDR", ihdr(width, height));
        writeChunk(out, "IDAT", deflateImage(width, height, rgb));
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static byte[] ihdr(int width, int height) {
        byte[] data = new byte[13];
        putInt(data, 0, width);
        putInt(data, 4, height);
        data[8] = 8; // bit depth
        data[9] = 2; // truecolor RGB
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        return data;
    }

    private static byte[] deflateImage(int width, int height, int[] rgb) {
        byte[] raw = new byte[(width * 3 + 1) * height];
        int pos = 0;
        for (int y = 0; y < height; y++) {
            raw[pos++] = 0;
            for (int x = 0; x < width; x++) {
                int color = rgb[y * width + x];
                raw[pos++] = (byte) ((color >> 16) & 0xFF);
                raw[pos++] = (byte) ((color >> 8) & 0xFF);
                raw[pos++] = (byte) (color & 0xFF);
            }
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream(raw.length / 4);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            compressed.write(buffer, 0, count);
        }
        deflater.end();
        return compressed.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        writeInt(out, data.length);
        writeBytes(out, typeBytes);
        writeBytes(out, data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(out, (int) crc.getValue());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void putInt(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }

    private static Map.Entry<Character, int[]> entry(char ch, int[] rows) {
        return Map.entry(ch, rows);
    }

    private static int[] rows(String... pattern) {
        int[] rows = new int[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            rows[i] = bits(pattern[i]);
        }
        return rows;
    }

    private static int bits(String row) {
        int value = 0;
        for (int i = 0; i < row.length(); i++) {
            value <<= 1;
            if (row.charAt(i) == '1') {
                value |= 1;
            }
        }
        return value;
    }
}
