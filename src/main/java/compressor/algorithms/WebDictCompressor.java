package compressor.algorithms;

import compressor.core.AbstractCompressor;
import compressor.model.CompressionStats;
import compressor.utils.BitOutputStream;
import compressor.utils.BitInputStream;
import compressor.utils.HashUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class WebDictCompressor extends AbstractCompressor {

    public static final String ALGORITHM_NAME = "WebDict";
    public static final String DESCRIPTION = "网页专用压缩 - Trie树字典 + LZ77 + Huffman 三级混合压缩";
    private static final byte[] MAGIC = new byte[]{'W', 'D', 'C', 'T'};

    private final TrieDictionary trieDictionary;
    private final HuffmanCompressor huffmanCompressor;
    private final LZ77Compressor lz77Compressor;

    private int trieMatchCount;
    private int trieBytesSaved;
    private int lz77MatchCount;
    private int lz77BytesSaved;

    public WebDictCompressor() {
        this.trieDictionary = new TrieDictionary();
        this.huffmanCompressor = new HuffmanCompressor();
        this.lz77Compressor = new LZ77Compressor();
        resetStats();
    }

    private void resetStats() {
        this.trieMatchCount = 0;
        this.trieBytesSaved = 0;
        this.lz77MatchCount = 0;
        this.lz77BytesSaved = 0;
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        startTiming();
        resetStats();

        String text = new String(data, StandardCharsets.UTF_8);

        String afterTrie = trieDictionary.applyReplacements(text);
        trieMatchCount = trieDictionary.getMatchCount();
        trieBytesSaved = trieDictionary.getReplacementBytesSaved();

        byte[] afterTrieBytes = afterTrie.getBytes(StandardCharsets.UTF_8);

        byte[] afterLZ77 = lz77Compressor.compress(afterTrieBytes);
        CompressionStats lz77Stats = lz77Compressor.getStats();
        lz77MatchCount = (int) (lz77Stats.getOriginalSize() - lz77Stats.getCompressedSize()) / 2;
        lz77BytesSaved = (int) (lz77Stats.getOriginalSize() - lz77Stats.getCompressedSize());

        byte[] finalResult = huffmanCompressor.compress(afterLZ77);
        CompressionStats huffmanStats = huffmanCompressor.getStats();

        CRC32 crc32 = new CRC32();
        crc32.update(data);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC[0]);
        out.write(MAGIC[1]);
        out.write(MAGIC[2]);
        out.write(MAGIC[3]);
        out.write(1);

        byte[] meta = buildMetadata();
        out.write(meta.length & 0xFF);
        out.write((meta.length >> 8) & 0xFF);
        out.write((meta.length >> 16) & 0xFF);
        out.write((meta.length >> 24) & 0xFF);
        out.write(meta);

        out.write(finalResult, 0, finalResult.length);

        byte[] result = out.toByteArray();
        endTiming(data.length, result.length);
        return result;
    }

    private byte[] buildMetadata() throws IOException {
        ByteArrayOutputStream meta = new ByteArrayOutputStream();

        meta.write(0x01);

        byte[] trieBytes = String.valueOf(trieMatchCount).getBytes(StandardCharsets.UTF_8);
        meta.write(trieBytes.length & 0xFF);
        meta.write(trieBytes);

        byte[] trieSaved = String.valueOf(trieBytesSaved).getBytes(StandardCharsets.UTF_8);
        meta.write(trieSaved.length & 0xFF);
        meta.write(trieSaved);

        return meta.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length < 4 + 1 + 4 + 10) {
            return new byte[0];
        }

        startTiming();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] magic = new byte[4];
        in.read(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
            magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new IOException("无效的WebDict压缩文件格式");
        }

        in.read();

        int metaLen = in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
        byte[] meta = new byte[metaLen];
        in.read(meta);

        byte[] compressedData = new byte[data.length - (4 + 1 + 4 + metaLen)];
        in.read(compressedData);

        byte[] afterHuffman = huffmanCompressor.decompress(compressedData);

        byte[] afterLZ77 = lz77Compressor.decompress(afterHuffman);

        String afterTrie = new String(afterLZ77, StandardCharsets.UTF_8);
        String original = trieDictionary.removeReplacements(afterTrie);

        byte[] result = original.getBytes(StandardCharsets.UTF_8);
        endTiming(result.length, data.length);
        return result;
    }

    public String getTrieTreeVisualization() {
        return trieDictionary.visualizeTree(4);
    }

    public int getTrieMatchCount() {
        return trieMatchCount;
    }

    public int getTrieBytesSaved() {
        return trieBytesSaved;
    }

    public int getLz77MatchCount() {
        return lz77MatchCount;
    }

    public int getLz77BytesSaved() {
        return lz77BytesSaved;
    }

    public String getCompressionPipelineInfo() {
        return String.format(
            "压缩流水线统计:\n" +
            "  1. Trie字典替换: 匹配 %d 次, 节省 %d 字节\n" +
            "  2. LZ77压缩: 匹配 %d 次, 节省 %d 字节\n" +
            "  3. Huffman编码: 进一步压缩\n" +
            "  总计节省: %d 字节",
            trieMatchCount, trieBytesSaved,
            lz77MatchCount, lz77BytesSaved,
            trieBytesSaved + lz77BytesSaved
        );
    }

    public TrieDictionary getTrieDictionary() {
        return trieDictionary;
    }
}
