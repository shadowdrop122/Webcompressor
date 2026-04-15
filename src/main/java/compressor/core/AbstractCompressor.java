package compressor.core;

import compressor.model.CompressionStats;
import java.io.IOException;

public abstract class AbstractCompressor implements ICompressor {
    protected CompressionStats stats;

    protected AbstractCompressor() {
        this.stats = new CompressionStats();
    }

    protected void startTiming() {
        stats.setOriginalSize(0);
        stats.setCompressedSize(0);
        stats.setElapsedMillis(System.currentTimeMillis());
    }

    protected void endTiming(long originalSize, long compressedSize) {
        stats.setOriginalSize(originalSize);
        stats.setCompressedSize(compressedSize);
        stats.setElapsedMillis(System.currentTimeMillis() - stats.getElapsedMillis());
        stats.calculate();
    }

    protected byte[] ensureCapacity(byte[] data, int needed) {
        if (data == null) {
            return new byte[needed];
        }
        return data;
    }

    @Override
    public CompressionStats getStats() {
        return stats;
    }
}
