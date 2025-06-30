# Bech32 for Java

A pure java implementation of the Bech32 encoding, that is heavily optimized to reduce memory copying and allocations.


## Usage

```java
ByteBuffer data = ...; // Your byte data
byte hrp[] = "npub".getBytes(StandardCharsets.UTF_8); // hrp prefix you want to use

String encoded = Bech32.bech32Encode(hrp, data);
System.out.println("Encoded: " + encoded);

ByteBuffer decoded = Bech32.bech32Decode(encoded);
```