package compressor.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BitInputStream implements AutoCloseable {
    private final ByteArrayInputStream bais;
    private int buffer;
    private int bitCount;

    public BitInputStream(byte[] data) {
        this.bais = new ByteArrayInputStream(data);
        this.buffer = 0;
        this.bitCount = 0;
    }

    public BitInputStream(ByteArrayInputStream bais) {
        this.bais = bais;
        this.buffer = 0;
        this.bitCount = 0;
    }

    public int readBit() throws IOException {
        if (bitCount == 0) {
            int nextByte = bais.read();
            if (nextByte == -1) {
                return -1;
            }
            buffer = nextByte;
            bitCount = 8;
        }
        int bit = (buffer >> 7) & 1;
        buffer = (buffer << 1) & 0xff;
        bitCount--;
        return bit;
    }

    public int readBits(int numBits) throws IOException {
        int result = 0;
        for (int i = 0; i < numBits; i++) {
            int bit = readBit();
            if (bit == -1) {
                return -1;
            }
            result = (result << 1) | bit;
        }
        return result;
    }

    public int readByte() throws IOException {
        return readBits(8);
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            int b = readByte();
            if (b == -1) {
                throw new IOException("Unexpected end of stream");
            }
            result[i] = (byte) b;
        }
        return result;
    }

    public boolean hasRemaining() throws IOException {
        if (bitCount > 0) {
            return true;
        }
        return bais.available() > 0;
    }

    public int available() throws IOException {
        int bitsAvailable = bitCount + bais.available() * 8;
        return bitsAvailable / 8;
    }

    public void skipToByteBoundary() {
        bitCount = 0;
        buffer = 0;
    }

    @Override
    public void close() throws IOException {
        bais.close();
    }
}
