package val.util;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import val.Constants;
import val.ValException;
import val.crypto.Crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

public final class Convert {

    public static final BigInteger two64 = new BigInteger("18446744073709551616");
    public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final long[] multipliers = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
    private static final char ENCODED_ZERO = ALPHABET[0];
    private static final int[] INDEXES = new int[128];

    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    private Convert() {
    } // never

    // Base58 编码
    public static String base58Encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        // 统计前导0
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }
        // 复制一份进行修改
        input = Arrays.copyOf(input, input.length);
        // 最大编码数据长度
        char[] encoded = new char[input.length * 2];
        int outputStart = encoded.length;
        // Base58编码正式开始
        for (int inputStart = zeros; inputStart < input.length; ) {
            encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58)];
            if (input[inputStart] == 0) {
                ++inputStart;
            }
        }
        // 输出结果中有0,去掉输出结果的前端0
        while (outputStart < encoded.length && encoded[outputStart] == ENCODED_ZERO) {
            ++outputStart;
        }
        // 处理前导0
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO;
        }
        // 返回Base58
        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    public static byte[] base58Decode(String input) {
        if (input.length() == 0) {
            return new byte[0];
        }
        // 将BASE58编码的ASCII字符转换为BASE58字节序列
        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int digit = c < 128 ? INDEXES[c] : -1;
            if (digit < 0) {
                String msg = "Invalid characters,c=" + c;
                throw new RuntimeException(msg);
            }
            input58[i] = (byte) digit;
        }
        // 统计前导0
        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) {
            ++zeros;
        }
        // Base58 编码转 字节序（256进制）编码
        byte[] decoded = new byte[input.length()];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) {
                ++inputStart;
            }
        }
        // 忽略在计算过程中添加的额外超前零点。
        while (outputStart < decoded.length && decoded[outputStart] == 0) {
            ++outputStart;
        }
        // 返回原始的字节数据
        return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
    }

    // 进制转换代码
    private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }

    public static byte[] parseHexString(String hex) {
        if (hex == null)
            return null;
        try {
            if (hex.length() % 2 != 0) {
                hex = hex.substring(0, hex.length() - 1);
            }
            return Hex.decode(hex);
        } catch (DecoderException e) {
            throw new RuntimeException("Could not parse hex string " + hex, e);
        }
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null)
            return null;
        return Hex.toHexString(bytes);
    }

    public static String toUnsignedLong(long objectId) {
        return Long.toUnsignedString(objectId);
    }

    public static boolean isNumberic(String number) {
        try {
            long num = Long.parseUnsignedLong(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isNumber(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        if (str.indexOf(".") > 0) {
            if (str.indexOf(".") == str.lastIndexOf(".") && str.split("\\.").length == 2) {
                return pattern.matcher(str.replace(".", "")).matches();
            } else {
                return false;
            }
        } else {
            return pattern.matcher(str).matches();
        }
    }

    public static long parseUnsignedLong(String number) {
        if (number == null) {
            return 0;
        }
        return Long.parseUnsignedLong(number);
    }

    public static long parseSignedLong(String number) {
        if ("".equals(number) || number == null) {
            return 0;
        }
        BigDecimal data = new BigDecimal(number);
        long l = data.subtract(new BigDecimal(Long.MAX_VALUE)).subtract(BigDecimal.valueOf(1)).longValue();
        return l | Long.MIN_VALUE;
    }

    public static int parseInt(String number) {
        int result;
        if (number == null) {
            return 0;
        }
        try {
            result = Integer.parseInt(number);
        } catch (Exception e) {
            return 0;
        }
        return result;
    }

    public static long parseAccountId(String account) {
        if (account == null) {
            return 0;
        }
        account = account.toUpperCase();
        if (account.startsWith("VAL-") || account.startsWith("VOL-")) {
            return Crypto.rsDecode(account.substring(4));
        } else {
            return parseUnsignedLong(account);
        }
    }

    public static String rsAccount(long accountId) {
        return "VAL-" + Crypto.rsEncode(accountId);
    }

    public static long fullHashToId(byte[] hash) {
        int heightByte = 19; // 8 for v1, 32 for v2 userID
        if (hash == null || hash.length < heightByte) {
            throw new IllegalArgumentException("Invalid hash: " + Arrays.toString(hash));
        }
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (hash[heightByte - 1 - i] & 0xFF);
        }
        return result;
    }

    public static long fullHashToId(String hash) {
        if (hash == null) {
            return 0;
        }
        return fullHashToId(Convert.parseHexString(hash));
    }

    public static Date fromEpochTime(int epochTime) {
        return new Date(epochTime * 1000L + Constants.EPOCH_BEGINNING - 500L);
    }

    public static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static byte[] emptyToNull(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        for (byte b : bytes) {
            if (b != 0) {
                return bytes;
            }
        }
        return null;
    }

    public static byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String readString(ByteBuffer buffer, int numBytes, int maxLength)
            throws ValException.NotValidException {
        if (numBytes > 3 * maxLength) {
            throw new ValException.NotValidException("Max parameter length exceeded");
        }
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return Convert.toString(bytes);
    }

    public static String truncate(String s, String replaceNull, int limit, boolean dots) {
        return s == null ? replaceNull
                : s.length() > limit ? (s.substring(0, dots ? limit - 3 : limit) + (dots ? "..." : "")) : s;
    }

    public static long parseNXT(String nxt) {
        return parseStringFraction(nxt, 8, Constants.MAX_BALANCE_CHAIN);
    }

    private static long parseStringFraction(String value, int decimals, long maxValue) {
        String[] s = value.trim().split("\\.");
        if (s.length == 0 || s.length > 2) {
            throw new NumberFormatException("Invalid number: " + value);
        }
        long wholePart = Long.parseLong(s[0]);
        if (wholePart > maxValue) {
            throw new IllegalArgumentException("Whole part of value exceeds maximum possible");
        }
        if (s.length == 1) {
            return wholePart * multipliers[decimals];
        }
        long fractionalPart = Long.parseLong(s[1]);
        if (fractionalPart >= multipliers[decimals] || s[1].length() > decimals) {
            throw new IllegalArgumentException("Fractional part exceeds maximum allowed divisibility");
        }
        for (int i = s[1].length(); i < decimals; i++) {
            fractionalPart *= 10;
        }
        return wholePart * multipliers[decimals] + fractionalPart;
    }

    // overflow checking based on
    // https://www.securecoding.cert.org/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
    public static long safeAdd(long left, long right) throws ArithmeticException {
        if (right > 0 ? left > Long.MAX_VALUE - right : left < Long.MIN_VALUE - right) {
            throw new ArithmeticException("Integer overflow");
        }
        return left + right;
    }

    public static long safeSubtract(long left, long right) throws ArithmeticException {
        if (right > 0 ? left < Long.MIN_VALUE + right : left > Long.MAX_VALUE + right) {
            throw new ArithmeticException("Integer overflow");
        }
        return left - right;
    }

    public static long safeMultiply(long left, long right) throws ArithmeticException {
        if (right > 0 ? left > Long.MAX_VALUE / right || left < Long.MIN_VALUE / right
                : (right < -1 ? left > Long.MIN_VALUE / right || left < Long.MAX_VALUE / right
                : right == -1 && left == Long.MIN_VALUE)) {
            throw new ArithmeticException("Integer overflow");
        }
        return left * right;
    }

    public static long safeDivide(long left, long right) throws ArithmeticException {
        if ((left == Long.MIN_VALUE) && (right == -1)) {
            throw new ArithmeticException("Integer overflow");
        }
        return left / right;
    }

    public static long safeNegate(long a) throws ArithmeticException {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Integer overflow");
        }
        return -a;
    }

    public static long safeAbs(long a) throws ArithmeticException {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Integer overflow");
        }
        return Math.abs(a);
    }

    public static boolean isNullorEmpty(String str) {
        return "".equals(str) || str == null ? true : false;
    }

}
