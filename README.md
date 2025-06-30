# Bech32 for Java

A pure java implementation of the Bech32 encoding, that is heavily optimized to reduce memory copying and allocations.


The library is released to maven central: [org.ngengine/bech32](https://central.sonatype.com/artifact/org.ngengine/bech32)
```gradle
repositories {
    mavenCentral()
    // Uncomment this if you want to use a -SNAPSHOT version
    //maven { 
    //    url = uri("https://central.sonatype.com/repository/maven-snapshots")
    //}
}

dependencies {
    implementation 'org.ngengine:bech32:<version>'
}

```

## Usage example

```java
ByteBuffer data = ...; // Your byte data
byte hrp[] = "npub".getBytes(StandardCharsets.UTF_8); // hrp prefix you want to use

String encoded = Bech32.bech32Encode(hrp, data);
System.out.println("Encoded: " + encoded);

ByteBuffer decoded = Bech32.bech32Decode(encoded);
```