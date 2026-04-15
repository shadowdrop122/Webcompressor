package compressor.algorithms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class HuffmanCompressorTest {

    private HuffmanCompressor compressor;

    @BeforeEach
    void setUp() {
        compressor = new HuffmanCompressor();
    }

    @Test
    void testCompressAndDecompressSimpleText() throws IOException {
        String text = "AAAAAAABBBBBCCCC";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = compressor.compress(original);
        assertTrue(compressed.length < original.length, "压缩后应该更小");

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);
        String result = new String(decompressed, StandardCharsets.UTF_8);

        assertEquals(text, result, "解压后应该与原始数据一致");
    }

    @Test
    void testCompressAndDecompressRepeatingPattern() throws IOException {
        String text = "AAAAAAAAAA".repeat(100);
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = compressor.compress(original);
        System.out.println("原始大小: " + original.length + " bytes");
        System.out.println("压缩后大小: " + compressed.length + " bytes");
        System.out.println(compressor.getStats());

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);
        String result = new String(decompressed, StandardCharsets.UTF_8);

        assertEquals(text, result, "解压后应该与原始数据一致");
    }

    @Test
    void testCompressAndDecompressRandomData() throws IOException {
        byte[] original = new byte[10000];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (Math.random() * 256);
        }

        byte[] compressed = compressor.compress(original);
        System.out.println("随机数据 - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertTrue(Arrays.equals(original, decompressed), "解压后应该与原始数据一致");
    }

    @Test
    void testCompressAndDecompressAllAsciiChars() throws IOException {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }

        byte[] compressed = compressor.compress(original);

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertTrue(Arrays.equals(original, decompressed), "所有字节模式应该无损解压");
    }

    @Test
    void testCompressEmptyData() throws IOException {
        byte[] original = new byte[0];
        byte[] compressed = compressor.compress(original);

        assertEquals(0, compressed.length, "空数据的压缩结果应为空");
    }

    @Test
    void testCompressSingleCharacter() throws IOException {
        byte[] original = "A".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(original);

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertEquals("A", new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    void testInvalidFormat() {
        byte[] invalidData = "NOT_HUFF".getBytes(StandardCharsets.UTF_8);

        HuffmanCompressor decompressor = new HuffmanCompressor();
        assertThrows(IOException.class, () -> decompressor.decompress(invalidData));
    }

    @Test
    void testStatsTracking() throws IOException {
        String text = "Hello World! This is a Huffman compression test.";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        compressor.compress(original);
        System.out.println(compressor.getStats());

        assertNotNull(compressor.getStats());
        assertEquals(original.length, compressor.getStats().getOriginalSize());
        assertTrue(compressor.getStats().getOriginalSize() > 0);
        assertTrue(compressor.getStats().getElapsedMillis() >= 0);
    }

    @Test
    void testChineseText() throws IOException {
        String text = "这是一个中文测试文本，用于验证哈夫曼编码对Unicode字符的压缩效果。";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = compressor.compress(original);
        System.out.println("中文文本 - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");

        HuffmanCompressor decompressor = new HuffmanCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }
}
