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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;

public class TestBech32Performance {

    @Test
    public void benchmarkMixedEncodeDecodeRoundtrip() throws Exception {
        byte[] hrp = "npub".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[32];
        byte[] checksum = new byte[6];

        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        // Warmup
        for (int i = 0; i < 100; i++) {
            String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
            Bech32.bech32Decode(encoded);
        }

        final int iterations = 100_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
            Bech32.bech32Decode(encoded);
        }

        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double encodeOpsPerSec = (iterations / (elapsedMs / 1000.0));
        double decodeOpsPerSec = (iterations / (elapsedMs / 1000.0));

        System.out.println("=== Mixed Encode/Decode Roundtrip ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Total time: " + String.format("%.2f ms", elapsedMs));
        System.out.println("Encoding ops/s: " + String.format("%.0f", encodeOpsPerSec));
        System.out.println("Decoding ops/s: " + String.format("%.0f", decodeOpsPerSec));
        System.out.println("Combined ops/s: " + String.format("%.0f", encodeOpsPerSec + decodeOpsPerSec));
    }

    @Test
    public void benchmarkEncodeOnly() throws Exception {
        byte[] hrp = "npub".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[32];
        byte[] checksum = new byte[6];

        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        // Warmup
        for (int i = 0; i < 100; i++) {
            Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
        }

        final int iterations = 200_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
        }

        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double encodingOpsPerSec = (iterations / (elapsedMs / 1000.0));

        System.out.println("=== Encode Only ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Total time: " + String.format("%.2f ms", elapsedMs));
        System.out.println("Encoding ops/s: " + String.format("%.0f", encodingOpsPerSec));
        System.out.println("Time per iteration: " + String.format("%.3f", elapsedMs / iterations) + " ms");
    }

    @Test
    public void benchmarkDecodeOnly() throws Exception {
        byte[] hrp = "npub".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[32];
        byte[] checksum = new byte[6];

        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);

        // Warmup
        for (int i = 0; i < 100; i++) {
            Bech32.bech32Decode(encoded);
        }

        final int iterations = 200_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            Bech32.bech32Decode(encoded);
        }

        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double decodingOpsPerSec = (iterations / (elapsedMs / 1000.0));

        System.out.println("=== Decode Only ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Total time: " + String.format("%.2f ms", elapsedMs));
        System.out.println("Decoding ops/s: " + String.format("%.0f", decodingOpsPerSec));
        System.out.println("Time per iteration: " + String.format("%.3f", elapsedMs / iterations) + " ms");
    }

    @Test
    public void benchmarkVariablePayloadSizes() throws Exception {
        byte[] hrp = "test".getBytes(StandardCharsets.UTF_8);
        byte[] checksum = new byte[6];
        Random random = new Random(42L);

        int[] sizes = { 1, 8, 16, 32, 64, 128 };

        for (int size : sizes) {
            byte[] payload = new byte[size];
            random.nextBytes(payload);

            // Warmup
            for (int i = 0; i < 50; i++) {
                String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
                Bech32.bech32Decode(encoded);
            }

            final int iterations = 50_000;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                String encoded = Bech32.bech32Encode(hrp, ByteBuffer.wrap(payload), checksum);
                Bech32.bech32Decode(encoded);
            }

            long endTime = System.nanoTime();
            double elapsedMs = (endTime - startTime) / 1_000_000.0;
            double encodingOpsPerSec = (iterations / (elapsedMs / 1000.0));
            double decodingOpsPerSec = (iterations / (elapsedMs / 1000.0));

            System.out.println(
                "Payload size " +
                size +
                " bytes - Encoding ops/s: " +
                String.format("%.0f", encodingOpsPerSec) +
                ", Decoding ops/s: " +
                String.format("%.0f", decodingOpsPerSec)
            );
        }
    }
}
