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

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import org.junit.Test;

public class TestBech32 {

    private static String[] VALID = {
        "A12UEL5L",
        "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
        "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
        "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j",
        "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
        "nsec1v4gj83ph04flwe940mkkr9fnxv0s7r85pqjj3kwuhdg8455f460q08upxx",
        "npub1wpuq4mcuhnxhnrqk85hk29qjz6u93vpzxqy9qpuugpyc302fepkqg8t3a4",
    };

    private static String[] INVALID = { "npub1wpuq4mcuDFxhnrqk85hk29qjz6u93vpzxqy9qpuugpyc302fepkqg8t3a4" };

    @Test
    public void bech32Checksum() throws Exception {
        for (String s : VALID) {
            Bech32.bech32Decode(s);
        }
    }

    @Test
    public void bech32DecodeEncode() throws Exception {
        for (String s : VALID) {
            ByteBuffer decoded = Bech32.bech32Decode(s);
            String encoded = Bech32.bech32Encode(s.substring(0, s.lastIndexOf('1')).getBytes(), decoded, new byte[6]);
            assertEquals(s.toLowerCase(), encoded.toLowerCase());
            ByteBuffer decoded2 = Bech32.bech32Decode(encoded);
            assertEquals(decoded, decoded2);
        }
    }

    @Test
    public void invalidCheckSum() throws Exception {
        for (String s : INVALID) {
            try {
                Bech32.bech32Decode(s);
                fail("Expected Bech32DecodingException");
            } catch (Exception e) {
                // Expected
            }
        }
    }

    @Test
    public void randomData()
        throws Bech32EncodingException, Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException {
        byte[] hrp = new byte[5];
        byte[] data = new byte[32];
        for (int i = 0; i < hrp.length; i++) {
            hrp[i] = "a".getBytes()[0];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.random() * 256);
        }
        String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(data), new byte[6]);
        System.out.println("Encoded: " + encoded);
        ByteBuffer decoded = Bech32.bech32Decode(encoded);
        assertEquals(ByteBuffer.wrap(data), decoded);
    }
}
