/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020-2021  Mirko Hansen (baaazen@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.mhid.opensource.socialdistancemeter.diagkeys;

import android.annotation.SuppressLint;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.mhid.opensource.socialdistancemeter.database.CwaDiagKey;
import de.mhid.opensource.socialdistancemeter.utils.HexString;

public class DiagKeyCrypto {
    static class CryptoError extends Exception {
        public CryptoError(Throwable t) { super(t); }
    }

    private final CwaDiagKey cwaDiagKey;

    private byte[] rpiKey = null;
    private Pair<byte[], DiagKeyCrypto>[] calculatedRollingTimestamps = null;

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
    @SuppressWarnings("SameParameterValue")
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

    private static class AESEncrypt {
        private final Cipher cipher;

        @SuppressLint("GetInstance")
        public AESEncrypt(byte[] key) throws CryptoError {
            Key aesKey = new SecretKeySpec(key, "AES");
            try {
                cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
                throw new CryptoError(e);
            }
        }

        public byte[] encrypt(byte[] data) throws CryptoError {
            try {
                return cipher.doFinal(data);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new CryptoError(e);
            }
        }
    }

    public Pair<byte[],DiagKeyCrypto>[] getAllRPIs() throws CryptoError {
        if(calculatedRollingTimestamps == null) {
            //noinspection unchecked
            calculatedRollingTimestamps = (Pair<byte[], DiagKeyCrypto>[]) new Pair[cwaDiagKey.rollingPeriod];

            long rollingTimestamp = cwaDiagKey.rollingStartIntervalNumber;
            byte[] data = ByteBuffer.allocate(16)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .put("EN-RPI".getBytes(StandardCharsets.UTF_8))
                    .put(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                    .putInt(0)
                    .array();

            AESEncrypt aesEncrypt = new AESEncrypt(getRpiKey());

            for(int i=0; i<cwaDiagKey.rollingPeriod; i++) {
                // recalculate
                ByteBuffer.wrap(data)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(12, (int) (rollingTimestamp & 0xffffffffL));

                byte[] rpi = aesEncrypt.encrypt(data);
                calculatedRollingTimestamps[i] = new Pair<>(rpi, this);
                rollingTimestamp++;
            }
        }

        return calculatedRollingTimestamps;
    }

    public long getRollingTimestampForRpi(byte[] token) throws CryptoError {
        Pair<byte[], DiagKeyCrypto>[] rpis = getAllRPIs();
        for(int i=0; i<rpis.length; i++) {
            if(Arrays.equals(rpis[i].first, token)) return cwaDiagKey.rollingStartIntervalNumber + i;
        }
        return -1;
    }
}
