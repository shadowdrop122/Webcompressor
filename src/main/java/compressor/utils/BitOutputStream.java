package compressor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BitOutputStream implements AutoCloseable {
    private final ByteArrayOutputStream baos;
    private int buffer;
    private int bitCount;

    public BitOutputStream() {
        this.baos = new ByteArrayOutputStream();
        this.buffer = 0;
        this.bitCount = 0;
    }

    public BitOutputStream(ByteArrayOutputStream baos) {
        this.baos = baos;
        this.buffer = 0;
        this.bitCount = 0;
    }

    public void writeBit(int bit) {
        buffer = (buffer << 1) | (bit & 1);
        bitCount++;
        if (bitCount == 8) {
            flushBuffer();
        }
    }

    public void writeBits(int value, int numBits) {
        for (int i = numBits - 1; i >= 0; i--) {
            int bit = (value >> i) & 1;
            writeBit(bit);
        }
    }

    public void writeByte(int b) {
        for (int i = 7; i >= 0; i--) {
            writeBit((b >> i) & 1);
        }
    }

    public void writeBytes(byte[] data) {
        for (byte b : data) {
            writeByte(b & 0xff);
        }
    }

    private void flushBuffer() {
        if (bitCount > 0) {
            while (bitCount < 8) {
                buffer = buffer << 1;
                bitCount++;
            }
            baos.write(buffer);
            buffer = 0;
            bitCount = 0;
        }
    }

    public void alignToByte() {
        if (bitCount > 0) {
            while (bitCount < 8) {
                buffer = buffer << 1;
                bitCount++;
            }
            baos.write(buffer);
            buffer = 0;
            bitCount = 0;
        }
    }

    public void flush() throws IOException {
        alignToByte();
        baos.flush();
    }

    public byte[] toByteArray() {
        alignToByte();
        return baos.toByteArray();
    }

    public int size() {
        return baos.size();
    }

    public void reset() {
        buffer = 0;
        bitCount = 0;
        baos.reset();
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
