package de.mhid.opensource.cwadetails.diagkeys;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import at.favre.lib.crypto.HKDF;
import de.mhid.opensource.cwadetails.database.CwaDiagKey;
import de.mhid.opensource.cwadetails.database.CwaToken;
import de.mhid.opensource.cwadetails.utils.HexString;

public class DiagKeyCrypto {
    class CryptoError extends Exception {
        public CryptoError(Throwable t) { super(t); }
    }

    private CwaDiagKey cwaDiagKey;
    private byte[] rpiKey = null;

    private Long calculatedRollingTimestamp = null;
    private byte[] calculatedRpi = null;

    public DiagKeyCrypto(CwaDiagKey cwaDiagKey) {
        this.cwaDiagKey = cwaDiagKey;
    }

    public CwaDiagKey getDiagKey() {
        return cwaDiagKey;
    }

    private byte[] getRpiKey() {
        if(rpiKey == null) {
            HKDF hkdf = HKDF.fromHmacSha256();
            rpiKey = hkdf.extractAndExpand((byte[])null, HexString.toByteArray(cwaDiagKey.keyData), "EN-RPIK".getBytes(), 16);
        }
        return rpiKey;
    }

    private byte[] aesEncrypt(byte[] key, byte[] data) throws CryptoError {
        Key aesKey = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new CryptoError(e);
        }
    }

    private byte[] getRpiForRollingTimestamp(long rollingTimestamp) throws CryptoError {
        if(calculatedRollingTimestamp == null || calculatedRollingTimestamp != rollingTimestamp) {
            // recalculate
            byte[] data = ByteBuffer.allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("EN-RPI".getBytes())
                .put(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                .putInt((int) (rollingTimestamp & 0xffffffffL))
                .array();

            calculatedRpi = aesEncrypt(getRpiKey(), data);
            calculatedRollingTimestamp = rollingTimestamp;
        }

        return calculatedRpi;
    }

    public boolean isTokenMatching(CwaToken cwaToken) throws CryptoError {
        byte[] rpiFromToken = HexString.toByteArray(cwaToken.token.substring(0, 16*2));
//        byte[] aem = HexString.toByteArray(cwaToken.token.substring(16*2));
        byte[] rpiFromDiagKey = getRpiForRollingTimestamp(cwaToken.rollingTimestamp);

        return Arrays.equals(rpiFromToken, rpiFromDiagKey);
    }
}
