package compressor.gui;

import compressor.algorithms.*;
import compressor.core.ICompressor;
import java.util.*;

public class CompressorFactory {

    public enum CompressorType {
        HUFFMAN("Huffman", "哈夫曼编码 - 基于字符频率的变长编码压缩算法"),
        LZ77("LZ77", "LZ77滑动窗口字典压缩 - 基于重复模式的字典编码"),
        WEB_DICT("WebDict", "网页专用压缩 - Trie树字典 + LZ77 + Huffman 三级混合压缩");

        private final String name;
        private final String description;

        CompressorType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final Map<String, CompressorType> TYPE_MAP = new HashMap<>();

    static {
        for (CompressorType type : CompressorType.values()) {
            TYPE_MAP.put(type.getName().toLowerCase(), type);
            TYPE_MAP.put(type.name().toLowerCase(), type);
        }
    }

    public static ICompressor createCompressor(CompressorType type) {
        return switch (type) {
            case HUFFMAN -> new HuffmanCompressor();
            case LZ77 -> new LZ77Compressor();
            case WEB_DICT -> new WebDictCompressor();
        };
    }

    public static ICompressor createCompressor(String name) {
        CompressorType type = TYPE_MAP.get(name.toLowerCase());
        if (type == null) {
            throw new IllegalArgumentException("未知的压缩算法: " + name);
        }
        return createCompressor(type);
    }

    public static CompressorType getTypeFromExtension(String extension) {
        String ext = extension.toLowerCase();
        return switch (ext) {
            case "html", "htm", "css", "js", "json", "xml" -> CompressorType.WEB_DICT;
            case "txt", "log", "csv", "md" -> CompressorType.HUFFMAN;
            default -> CompressorType.HUFFMAN;
        };
    }

    public static List<CompressorType> getAvailableTypes() {
        return Arrays.asList(CompressorType.values());
    }

    public static CompressorInfo getCompressorInfo(CompressorType type) {
        return new CompressorInfo(type.getName(), type.getDescription());
    }

    public record CompressorInfo(String name, String description) {}
}
