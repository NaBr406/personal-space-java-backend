package cn.nabr.personalspace.util;

import java.security.SecureRandom;

/**
 * 邀请码生成器。
 */
public final class InviteCodeGenerator {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodeGenerator() {}

    /**
     * 生成 8 位十六进制大写邀请码，短一些，手动输入也方便。
     */
    public static String generate() {
        char[] chars = new char[8];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = HEX[RANDOM.nextInt(HEX.length)];
        }
        return new String(chars);
    }
}
