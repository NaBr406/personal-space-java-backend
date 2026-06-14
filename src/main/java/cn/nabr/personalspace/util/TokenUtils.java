package cn.nabr.personalspace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * token 相关工具。
 * 负责生成随机 token 和计算 SHA-256 摘要。
 */
public final class TokenUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private TokenUtils() {}

    /**
     * 生成 32 字节随机 token，再转成十六进制字符串。
     */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /**
     * session 入库前统一做 hash，避免明文 token 落库。
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成 token hash", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
