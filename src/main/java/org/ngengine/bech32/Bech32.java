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
import java.nio.charset.StandardCharsets;

// Bech32 encoder/decoder
//      based on https://github.com/SamouraiDev/bech32/tree/master
//      this implementation aims to be faster reducing memory allocations,
//      copying and gc pressure.
// + thread safe

public class Bech32 {

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final byte[] ZEROES = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private static final int[] GENERATORS = { 0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3 };
    private static final char BECH32_SEPARATOR = '1';

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
     * @param data a ByteBuffer containing the data to encode, must be at position 0 and limit set to the length of the data
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

        data = convertBits(data, 0, 8, 5, true);

        if (data.position() != 0) throw new Bech32EncodingException("expected data buffer to be at position 0");

        int xlatLength = 6 + data.limit();

        int i = 0;

        byte[] ret = new byte[hrp.length + xlatLength + 1];
        for (int j = 0; j < hrp.length; j++) {
            byte b = hrp[j];
            if (b >= 0x41 && b <= 0x5a) {
                b += 32;
            }
            ret[i++] = b;
        }

        createChecksum(ret, hrp.length, data, chkOut);

        ret[i++] = BECH32_SEPARATOR;

        for (int j = 0; j < data.limit(); j++) {
            ret[i++] = (byte) CHARSET.charAt(data.get(j));
        }

        for (int j = 0; j < 6; j++) {
            ret[i++] = (byte) CHARSET.charAt(chkOut[j]);
        }

        return new String(ret, StandardCharsets.UTF_8);
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

        // decode
        for (int i = hrpLength + 1; i < bytes.length; i++) {
            int v = CHARSET.indexOf(bytes[i]);
            if (v == -1) throw new Bech32DecodingException("invalid bech32 character " + bytes[i]);
            bytes[i] = (byte) v;
        }

        // verify
        if (!verifyChecksum(bytes, hrpLength, hrpLength + 1)) {
            throw new Bech32InvalidChecksumException("invalid bech32 checksum");
        }

        ByteBuffer out = ByteBuffer.wrap(bytes, hrpLength + 1, bytes.length - (hrpLength + 1) - 6);
        out = convertBits(out, hrpLength + 1, 5, 8, false);
        return out.slice();
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
            for (int i = 0; i < data.limit(); i++) {
                byte b = data.get(i);
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

    private static void createChecksum(@Nonnull byte[] hrp, int hrpLength, @Nonnull ByteBuffer data, @Nonnull byte[] ret) {
        int polymod = polymod(hrp, hrpLength, data, ZEROES, 0) ^ 1;
        for (int i = 0; i < 6; i++) {
            ret[i] = (byte) ((polymod >> 5 * (5 - i)) & 0x1f);
        }
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
    private static byte[] getLowerCaseBytes(@Nonnull String bech) throws Bech32InvalidRangeException {
        int length = bech.length();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            char c = bech.charAt(i);
            // Convert uppercase to lowercase (only for ASCII A-Z)
            if (c >= 'A' && c <= 'Z') {
                c += 32; // 'A'-'a'
            }
            if (c < 0x21 || c > 0x7e) {
                throw new Bech32InvalidRangeException("bech32 characters  out of range");
            }
            result[i] = (byte) c;
        }
        return result;
    }

    @Nonnull
    private static ByteBuffer convertBits(@Nonnull ByteBuffer in, int skip, int fromBits, int toBits, boolean pad) {
        int outCapacity = (in.limit() * fromBits + toBits - 1) / toBits;
        ByteBuffer output = ByteBuffer.allocate(outCapacity);

        int acc = 0;
        int bits = 0;
        final int maxv = (1 << toBits) - 1;

        for (int i = skip; i < in.limit(); i++) {
            int value = in.get(i) & 0xFF;
            if ((value >> fromBits) != 0) {
                throw new IllegalArgumentException("input value is outside of range");
            }
            acc = (acc << fromBits) | value;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                int word = (acc >> bits) & maxv;
                output.put((byte) word);
            }
        }

        if (pad) {
            if (bits > 0) {
                int word = (acc << (toBits - bits)) & maxv;
                output.put((byte) word);
            }
        } else if (bits >= fromBits || (((acc << (toBits - bits)) & maxv) != 0)) {
            throw new IllegalArgumentException("could not convert bits");
        }

        output.flip();
        return output;
    }
}
