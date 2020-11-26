package de.mhid.opensource.socialdistancemeter.diagkeys;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.mhid.opensource.socialdistancemeter.database.CwaDiagKey;
import de.mhid.opensource.socialdistancemeter.utils.HexString;

public class DiagKeyCrypto {
    class CryptoError extends Exception {
        public CryptoError(Throwable t) { super(t); }
    }

    private final CwaDiagKey cwaDiagKey;

    private byte[] rpiKey = null;
    private final HashMap<Long, byte[]> calculatedRollingTimestamps = new HashMap<>();

    public DiagKeyCrypto(CwaDiagKey cwaDiagKey) {
        this.cwaDiagKey = cwaDiagKey;
    }

    public CwaDiagKey getDiagKey() {
        return cwaDiagKey;
    }

    private byte[] hmacSHA256(byte[] key, byte[] message) throws CryptoError {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoError(e);
        }
    }

    // algorithm inspired by https://en.wikipedia.org/wiki/HKDF#Example:_Python_implementation
    private byte[] hkdf(byte[] key, byte[] salt, byte[] info, int outputLength) throws CryptoError {
        final int HASH_LEN = 32;
        if(salt == null) salt = new byte[outputLength];
        byte[] prk = hmacSHA256(salt, key);
        int times = outputLength / HASH_LEN + (outputLength % HASH_LEN == 0 ? 0 : 1);
        byte[] t = new byte[0];
        ByteBuffer okm = ByteBuffer.allocate(outputLength);
        for(int i=0; i<times; i++) {
            byte[] imsg = ByteBuffer.allocate(t.length + info.length + 1)
                    .put(t)
                    .put(info)
                    .put((byte)(i+1))
                    .array();
            t = hmacSHA256(prk, imsg);
            if(outputLength-okm.position() >= HASH_LEN) {
                okm.put(t);
            } else {
                okm.put(t, 0, outputLength-okm.position());
            }
        }
        return okm.array();
    }


    private byte[] getRpiKey() throws CryptoError {
        if(rpiKey == null) {
            rpiKey = hkdf(HexString.toByteArray(cwaDiagKey.keyData), null, "EN-RPIK".getBytes(StandardCharsets.UTF_8), 16);
        }
        return rpiKey;
    }

    private byte[] aesEncrypt(byte[] key, byte[] data) throws CryptoError {
        Key aesKey = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new CryptoError(e);
        }
    }

    private byte[] getRpiForRollingTimestamp(long rollingTimestamp) throws CryptoError {
        if(!calculatedRollingTimestamps.containsKey(rollingTimestamp)) {
            // recalculate
            byte[] data = ByteBuffer.allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("EN-RPI".getBytes(StandardCharsets.UTF_8))
                .put(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                .putInt((int) (rollingTimestamp & 0xffffffffL))
                .array();

            calculatedRollingTimestamps.put(rollingTimestamp, aesEncrypt(getRpiKey(), data));
        }

        return calculatedRollingTimestamps.get(rollingTimestamp);
    }

    public boolean isTokenMatching(String token, long rollingTimestamp) throws CryptoError {
        String rpi = token.substring(0, 32);
//        String aem = token.substring(32);

        byte[] rpiFromToken = HexString.toByteArray(rpi);
//        byte[] aemKey = HexString.toByteArray(aem);

        // compare with rpi of a rolling timestamp diff ~ 30mins
        for(long rti=rollingTimestamp-3; rti<=rollingTimestamp+3; rti++) {
            if(rti < cwaDiagKey.rollingStartIntervalNumber) continue;
            if(rti >= cwaDiagKey.rollingStartIntervalNumber + cwaDiagKey.rollingPeriod) break;

            byte[] rpiFromDiagKey = getRpiForRollingTimestamp(rti);
            if(Arrays.equals(rpiFromToken, rpiFromDiagKey)) return true;
        }

        return false;
    }
}
