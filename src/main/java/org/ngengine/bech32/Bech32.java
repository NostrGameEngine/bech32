/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2025, Riccardo Balbo
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ngengine.bech32;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Fast, low memory, thread safe and gc friendly Bech32 encoder/decoder.
 */
public class Bech32 {

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final byte[] CHARSET_REV = new byte[128];
    private static final int[] GENERATORS = { 0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3 };
    private static final char BECH32_SEPARATOR = '1';

    static {
        // Initialize reverse charset lookup table with -1 (invalid)
        for (int i = 0; i < CHARSET_REV.length; i++) {
            CHARSET_REV[i] = -1;
        }
        // Populate with valid character mappings
        for (int i = 0; i < CHARSET.length(); i++) {
            CHARSET_REV[CHARSET.charAt(i)] = (byte) i;
        }
    }

    /**
     * Encode some arbitrary data into a Bech32 string.
     *
     * <p>
     * This method will internally allocate a 6 byte array to hold the checksum.
     * If you want to avoid this internal allocation, use the other overload that takes a byte array as argument
     * </p>
     *
     * @param hrp the human readable part (the prefix), must be utf-8 encoded
     * @param data a ByteBuffer containing the data to encode, must be at position 0 and limit set to the length of the data
     * @return the Bech32 encoded string
     * @throws Bech32EncodingException
     */
    @Nonnull
    public static String bech32Encode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data) throws Bech32EncodingException {
        byte[] chk = new byte[6];
        return bech32Encode(hrp, data, chk);
    }

    /**
     * Encode some arbitrary data into a Bech32 string.
     * @param hrp the human readable part (the prefix), must be utf-8 encoded
     * @param data a ByteBuffer containing the data to encode, must have limit set to the length of the data to encode
     * @param chkOut a byte array of length 6 that will be filled with the checksum
     * @return
     * @throws Bech32EncodingException
     */
    @Nonnull
    public static String bech32Encode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data, @Nonnull byte[] chkOut)
        throws Bech32EncodingException {
        if (chkOut == null || chkOut.length < 6) {
            throw new Bech32EncodingException("invalid checksum buffer");
        }
        ByteBuffer src = data.slice();
        int dataLen = src.remaining();

        // Calculate exact output size: HRP + '1' + (data8 * 8 + 4) / 5 + 6
        int data5Len = (dataLen * 8 + 4) / 5;
        int outputSize = hrp.length + 1 + data5Len + 6;

        char[] output = new char[outputSize];
        int outPos = 0;

        // write HRP in lowercase
        byte[] hrpLower = new byte[hrp.length];
        for (int i = 0; i < hrp.length; i++) {
            byte b = hrp[i];
            if (b >= 0x41 && b <= 0x5a) {
                b += 32;
            }
            hrpLower[i] = b;
            output[outPos++] = (char) b;
        }

        // write separator
        output[outPos++] = BECH32_SEPARATOR;

        // initialize checksum
        int chk = 1;

        // process HRP
        for (int i = 0; i < hrpLower.length; i++) {
            byte b = hrpLower[i];
            b = (byte) (b >> 5);
            chk = polymod(b, chk);
        }
        chk = polymod((byte) 0x00, chk);

        for (int i = 0; i < hrpLower.length; i++) {
            byte b = (byte) (hrpLower[i] & 0x1f);
            chk = polymod(b, chk);
        }

        // convert 8 to 5 bits and compute checksum on the fly
        int acc = 0;
        int bits = 0;

        for (int i = 0; i < dataLen; i++) {
            int value = src.get(i) & 0xFF;
            if ((value >> 8) != 0) {
                throw new IllegalArgumentException("input value is outside of range");
            }
            acc = (acc << 8) | value;
            bits += 8;

            // emit 5-bit groups as we accumulate them
            while (bits >= 5) {
                bits -= 5;
                int v5 = (acc >> bits) & 0x1f;
                output[outPos++] = CHARSET.charAt(v5);
                chk = polymod((byte) v5, chk);
            }
        }

        // padding
        if (bits > 0) {
            int v5 = (acc << (5 - bits)) & 0x1f;
            output[outPos++] = CHARSET.charAt(v5);
            chk = polymod((byte) v5, chk);
        }

        // apply polymod to checksum padding (always 6 zero bits)
        for (int i = 0; i < 6; i++) {
            chk = polymod((byte) 0, chk);
        }

        // compute final checksum
        chk ^= 1;
        for (int i = 0; i < 6; i++) {
            byte checksumByte = (byte) ((chk >> (5 * (5 - i))) & 0x1f);
            chkOut[i] = checksumByte;
            output[outPos++] = CHARSET.charAt(checksumByte);
        }

        return new String(output);
    }

    /**
     * Decode a Bech32 string into a ByteBuffer.
     * @param bech the Bech32 encoded string, must be lower case
     * @return a ByteBuffer containing the decoded data, the position will be set to 0 and the limit to the length of the data
     * @throws Bech32DecodingException
     * @throws Bech32InvalidChecksumException
     * @throws Bech32InvalidRangeException
     */
    @Nonnull
    public static ByteBuffer bech32Decode(@Nonnull String bech)
        throws Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException {
        byte[] bytes = getLowerCaseBytes(bech);

        int hrpLength = 0;

        // extract hrp end using last index of separator
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] == BECH32_SEPARATOR) {
                hrpLength = i;
                break;
            }
        }

        if (hrpLength == 0 || hrpLength == bytes.length) {
            throw new Bech32DecodingException("invalid bech32 string");
        }

        // decode using O(1) reverse charset lookup
        for (int i = hrpLength + 1; i < bytes.length; i++) {
            byte c = bytes[i];
            if (c >= 128) {
                throw new Bech32DecodingException("invalid bech32 character");
            }
            byte v = CHARSET_REV[c];
            if (v < 0) {
                throw new Bech32DecodingException("invalid bech32 character " + (char) c);
            }
            bytes[i] = v;
        }

        // verify checksum
        if (!verifyChecksum(bytes, hrpLength, hrpLength + 1)) {
            throw new Bech32InvalidChecksumException("invalid bech32 checksum");
        }

        // extract data portion (5-bit values, excluding HRP and checksum)
        int dataStart = hrpLength + 1;
        int dataLen = bytes.length - dataStart - 6; // -6 for checksum

        // pre-calculate output size
        int outCapacity = (dataLen * 5) / 8;
        byte[] output = new byte[outCapacity];
        int outPos = 0;

        // 5 to 8 bit conversion
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < dataLen; i++) {
            int value = bytes[dataStart + i];
            acc = (acc << 5) | value;
            bits += 5;

            while (bits >= 8) {
                bits -= 8;
                int byte8 = (acc >> bits) & 0xff;
                output[outPos++] = (byte) byte8;
            }
        }

        // check remaining bits (should be < 5 and padding should be 0)
        if (bits >= 5 || (((acc << (8 - bits)) & 0xff) != 0)) {
            throw new IllegalArgumentException("could not convert bits");
        }

        return ByteBuffer.wrap(output, 0, outPos).slice();
    }

    private static int polymod(
        @Nonnull byte hrp[],
        int hrpLength,
        @Nullable ByteBuffer data,
        @Nonnull byte[] zeroes,
        int zeroesOffset
    ) {
        int chk = 1;

        // expand an process hrp
        // buf1
        for (int i = 0; i < hrpLength; i++) {
            byte b = hrp[i];
            b = (byte) (b >> 5);
            chk = polymod(b, chk);
        }

        // mid
        chk = polymod((byte) 0x00, chk);

        // buf2
        for (int i = 0; i < hrpLength; i++) {
            byte b = (byte) (hrp[i] & 0x1f);
            chk = polymod(b, chk);
        }

        // process data
        if (data != null) {
            ByteBuffer src = data.slice();
            for (int i = 0; i < src.remaining(); i++) {
                byte b = src.get(i);
                chk = polymod(b, chk);
            }
        }

        if (zeroes != null) {
            // process zeroes
            for (int i = zeroesOffset; i < zeroes.length; i++) {
                byte b = zeroes[i];
                chk = polymod(b, chk);
            }
        }

        return chk;
    }

    private static boolean verifyChecksum(@Nonnull byte[] combinedData, int hrpLength, int dataOffset) {
        int p = polymod(combinedData, hrpLength, null, combinedData, dataOffset);
        return (1 == p);
    }

    private static int polymod(byte b, int chk) {
        byte top = (byte) (chk >> 0x19);
        chk = b ^ ((chk & 0x1ffffff) << 5);
        for (int i = 0; i < 5; i++) {
            chk ^= ((top >> i) & 1) == 1 ? GENERATORS[i] : 0;
        }
        return chk;
    }

    @Nonnull
    private static byte[] getLowerCaseBytes(@Nonnull String bech) throws Bech32InvalidRangeException, Bech32DecodingException {
        int length = bech.length();
        byte[] result = new byte[length];
        byte caseState = 0; // 0=undefined, 1=lowercase, 2=uppercase

        for (int i = 0; i < length; i++) {
            char c = bech.charAt(i);

            // Convert uppercase to lowercase and reject mixed case
            if (c >= 'A' && c <= 'Z') {
                if (caseState == 1) {
                    throw new Bech32DecodingException("mixed case strings are not allowed");
                }
                caseState = 2;
                c += 32; // Convert to lowercase
            } else if (c >= 'a' && c <= 'z') {
                if (caseState == 2) {
                    throw new Bech32DecodingException("mixed case strings are not allowed");
                }
                caseState = 1;
            }

            if (c < 0x21 || c > 0x7e) {
                throw new Bech32InvalidRangeException("bech32 characters  out of range");
            }
            result[i] = (byte) c;
        }
        return result;
    }
}
