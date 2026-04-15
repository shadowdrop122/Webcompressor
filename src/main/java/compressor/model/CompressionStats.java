package compressor.model;

public class CompressionStats {
    private long originalSize;
    private long compressedSize;
    private long elapsedMillis;
    private double compressionRatio;
    private String algorithmName;

    public CompressionStats() {
    }

    public CompressionStats(long originalSize, long compressedSize, long elapsedMillis, String algorithmName) {
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.elapsedMillis = elapsedMillis;
        this.algorithmName = algorithmName;
        this.compressionRatio = originalSize > 0 ? (double) compressedSize / originalSize : 0;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public double getCompressionRatio() {
        return compressionRatio;
    }

    public void setCompressionRatio(double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public double getSavingsPercent() {
        if (originalSize <= 0) return 0;
        return (1.0 - compressionRatio) * 100;
    }

    public void calculate() {
        if (originalSize > 0) {
            this.compressionRatio = (double) compressedSize / originalSize;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "[%s] 原始: %d bytes | 压缩后: %d bytes | 压缩率: %.2f%% | 节省: %.2f%% | 耗时: %dms",
            algorithmName, originalSize, compressedSize,
            compressionRatio * 100, getSavingsPercent(), elapsedMillis
        );
    }
}
