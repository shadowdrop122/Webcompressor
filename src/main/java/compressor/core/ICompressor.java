package compressor.core;

import compressor.model.CompressionStats;
import java.io.IOException;

public interface ICompressor {
    byte[] compress(byte[] data) throws IOException;
    byte[] decompress(byte[] data) throws IOException;
    CompressionStats getStats();
    String getAlgorithmName();
    String getDescription();
}
