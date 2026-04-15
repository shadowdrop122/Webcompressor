package compressor.algorithms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WebDictCompressorTest {

    private WebDictCompressor compressor;

    @BeforeEach
    void setUp() {
        compressor = new WebDictCompressor();
    }

    @Test
    void testCompressAndDecompressSimpleHTML() throws IOException {
        String html = "<html><head><title>Test</title></head><body><div>Hello World</div></body></html>";
        byte[] original = html.getBytes();

        byte[] compressed = compressor.compress(original);
        assertTrue(compressed.length < original.length, "HTML应该能被压缩");

        WebDictCompressor decompressor = new WebDictCompressor();
        byte[] decompressed = decompressor.decompress(compressed);
        String result = new String(decompressed);

        assertEquals(html, result, "HTML解压后应一致");
    }

    @Test
    void testCompressAndDecompressComplexHTML() throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Complex Page</title>
                <link rel="stylesheet" href="style.css">
            </head>
            <body>
                <header><nav><main>
                    <div class="container">
                        <h1>Welcome</h1>
                        <p>Paragraph text here</p>
                        <a href="https://example.com">Link</a>
                    </div>
                </main></nav></header>
            </body>
            </html>
            """;
        byte[] original = html.getBytes();

        byte[] compressed = compressor.compress(original);
        System.out.println("复杂HTML - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");
        System.out.println("压缩率: " + String.format("%.2f%%", (1 - (double) compressed.length / original.length) * 100));
        System.out.println(compressor.getCompressionPipelineInfo());

        WebDictCompressor decompressor = new WebDictCompressor();
        byte[] decompressed = decompressor.decompress(compressed);
        String result = new String(decompressed);

        assertEquals(html, result, "复杂HTML应无损解压");
    }

    @Test
    void testTrieDictionary() {
        TrieDictionary trie = new TrieDictionary();

        assertTrue(trie.getTagCount() > 0, "Trie应包含预置标签");
        System.out.println("预置标签数量: " + trie.getTagCount());

        String html = "<html><body><div><span>text</span></div></body></html>";
        String result = trie.applyReplacements(html);

        System.out.println("Trie替换效果:");
        System.out.println("原始: " + html);
        System.out.println("替换: " + result);
        System.out.println("节省字节: " + trie.getReplacementBytesSaved());
        System.out.println("匹配次数: " + trie.getMatchCount());

        assertTrue(result.length() < html.length(), "替换后应该更短");
    }

    @Test
    void testTrieTreeVisualization() {
        TrieDictionary trie = new TrieDictionary();
        String tree = trie.visualizeTree(3);
        System.out.println("Trie树结构 (前3层):\n" + tree);
        assertNotNull(tree);
        assertTrue(tree.contains("[ROOT]"));
    }

    @Test
    void testCompressAndDecompressCSS() throws IOException {
        String css = """
            body { font-family: Arial; color: #333; }
            .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
            #header { background: #f5f5f5; height: 80px; }
            """;
        byte[] original = css.getBytes();

        byte[] compressed = compressor.compress(original);
        System.out.println("CSS - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");

        WebDictCompressor decompressor = new WebDictCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertTrue(Arrays.equals(original, decompressed), "CSS应无损解压");
    }

    @Test
    void testCompressAndDecompressJS() throws IOException {
        String js = """
            function init() {
                var element = document.getElementById('app');
                if (element) {
                    element.innerHTML = '<div>Loaded</div>';
                }
                return true;
            }
            window.onload = init;
            """;
        byte[] original = js.getBytes();

        byte[] compressed = compressor.compress(original);
        System.out.println("JS - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");
        System.out.println(compressor.getStats());

        WebDictCompressor decompressor = new WebDictCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertTrue(Arrays.equals(original, decompressed), "JS应无损解压");
    }

    @Test
    void testNonWebContent() throws IOException {
        byte[] original = new byte[10000];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (Math.random() * 256);
        }

        byte[] compressed = compressor.compress(original);
        System.out.println("随机数据 - 原始: " + original.length + " bytes, 压缩后: " + compressed.length + " bytes");

        WebDictCompressor decompressor = new WebDictCompressor();
        byte[] decompressed = decompressor.decompress(compressed);

        assertTrue(Arrays.equals(original, decompressed), "随机数据应无损解压");
    }

    @Test
    void testWebDictVsHuffman() throws IOException {
        String html = "<html><head><title>Test</title></head><body><div>Content</div></body></html>";
        byte[] original = html.getBytes();

        HuffmanCompressor huffman = new HuffmanCompressor();
        byte[] huffmanCompressed = huffman.compress(original);

        WebDictCompressor webdict = new WebDictCompressor();
        byte[] webdictCompressed = webdict.compress(original);

        System.out.println("对比分析:");
        System.out.println("原始大小: " + original.length);
        System.out.println("Huffman压缩: " + huffmanCompressed.length + " (" +
            String.format("%.2f%%", (1 - (double) huffmanCompressed.length / original.length) * 100) + ")");
        System.out.println("WebDict压缩: " + webdictCompressed.length + " (" +
            String.format("%.2f%%", (1 - (double) webdictCompressed.length / original.length) * 100) + ")");

        assertTrue(webdictCompressed.length <= huffmanCompressed.length,
            "WebDict压缩效果应优于或等于纯Huffman");
    }

    @Test
    void testEmptyData() throws IOException {
        byte[] original = new byte[0];
        byte[] compressed = compressor.compress(original);

        assertEquals(0, compressed.length, "空数据压缩结果应为空");
    }

    @Test
    void testStatsTracking() throws IOException {
        String html = "<html><body><div>Test</div></body></html>";
        byte[] original = html.getBytes();

        compressor.compress(original);
        System.out.println(compressor.getStats());
        System.out.println(compressor.getCompressionPipelineInfo());

        assertNotNull(compressor.getStats());
        assertEquals(original.length, compressor.getStats().getOriginalSize());
    }

    @Test
    void testInvalidFormat() {
        byte[] invalidData = "NOTWDCT".getBytes();

        WebDictCompressor decompressor = new WebDictCompressor();
        assertThrows(IOException.class, () -> decompressor.decompress(invalidData));
    }
}
