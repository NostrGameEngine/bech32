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
import java.nio.ByteBuffer;
import org.ngengine.bech32.Bech32.DataFormat;

public final class Bech32m {

    private Bech32m() {}

    @Nonnull
    public static String bech32mEncode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data) throws Bech32EncodingException {
        return bech32mEncode(hrp, data, DataFormat.BITS_8);
    }

    @Nonnull
    public static String bech32mEncode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data, @Nonnull DataFormat dataFormat)
        throws Bech32EncodingException {
        return bech32mEncode(hrp, data, dataFormat, new byte[6], -1);
    }

    @Nonnull
    public static String bech32mEncode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data, int maxLength)
        throws Bech32EncodingException {
        return bech32mEncode(hrp, data, DataFormat.BITS_8, maxLength);
    }

    @Nonnull
    public static String bech32mEncode(
        @Nonnull byte[] hrp,
        @Nonnull ByteBuffer data,
        @Nonnull DataFormat dataFormat,
        int maxLength
    ) throws Bech32EncodingException {
        return bech32mEncode(hrp, data, dataFormat, new byte[6], maxLength);
    }

    @Nonnull
    public static String bech32mEncode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data, @Nonnull byte[] chkOut)
        throws Bech32EncodingException {
        return bech32mEncode(hrp, data, DataFormat.BITS_8, chkOut);
    }

    @Nonnull
    public static String bech32mEncode(
        @Nonnull byte[] hrp,
        @Nonnull ByteBuffer data,
        @Nonnull DataFormat dataFormat,
        @Nonnull byte[] chkOut
    ) throws Bech32EncodingException {
        return bech32mEncode(hrp, data, dataFormat, chkOut, -1);
    }

    @Nonnull
    public static String bech32mEncode(@Nonnull byte[] hrp, @Nonnull ByteBuffer data, @Nonnull byte[] chkOut, int maxLength)
        throws Bech32EncodingException {
        return bech32mEncode(hrp, data, DataFormat.BITS_8, chkOut, maxLength);
    }

    @Nonnull
    public static String bech32mEncode(
        @Nonnull byte[] hrp,
        @Nonnull ByteBuffer data,
        @Nonnull DataFormat dataFormat,
        @Nonnull byte[] chkOut,
        int maxLength
    ) throws Bech32EncodingException {
        return Bech32.bech32Encode(ChecksumVariant.BECH32M_CONST, hrp, data, dataFormat, chkOut, maxLength);
    }

    @Nonnull
    public static ByteBuffer bech32mDecode(@Nonnull String bech)
        throws Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException {
        return bech32mDecode(bech, DataFormat.BITS_8);
    }

    @Nonnull
    public static ByteBuffer bech32mDecode(@Nonnull String bech, @Nonnull DataFormat outputFormat)
        throws Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException {
        return Bech32.bech32Decode(bech, -1, new ChecksumVariant().requireVariant(ChecksumVariant.BECH32M_CONST), outputFormat);
    }

    /**
     * Extract the HRP (Human Readable Part) from a Bech32m string.
     * @param bech the Bech32m encoded string
     * @return the HRP as a byte array
     * @throws Bech32DecodingException if the string is invalid
     */
    @Nonnull
    public static byte[] hrp(@Nonnull String bech) throws Bech32DecodingException, Bech32InvalidRangeException {
        return Bech32.hrp(bech);
    }
}
