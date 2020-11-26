package de.mhid.opensource.socialdistancemeter.utils;

public class HexString {
    private HexString() {}

    private final static char[] HEX_CONVERT_ARRAY = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] b) {
        char[] hexChars = new char[b.length * 2];
        for (int j = 0; j < b.length; j++) {
            int v = b[j] & 0xFF;
            hexChars[j * 2] = HEX_CONVERT_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_CONVERT_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] toByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}
