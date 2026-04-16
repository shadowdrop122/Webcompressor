package compressor.engine;

import compressor.core.ICompressor;
import compressor.algorithms.LZ77Compressor;
import compressor.algorithms.PoolingImageCompressor;
import compressor.algorithms.LZWImageCompressor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能资源调度器
 * 根据文件类型自动选择合适的压缩算法
 */
public class ResourceDispatcher {

    // 压缩策略ID
    public static final byte STRATEGY_WEBDICT = 1;    // 网页文本压缩
    public static final byte STRATEGY_LZ77 = 2;       // LZ77无损压缩
    public static final byte STRATEGY_LZW_IMAGE = 3;  // 图像LZW压缩
    public static final byte STRATEGY_POOLING_IMAGE = 4; // 图像池化压缩
    public static final byte STRATEGY_NONE = 0;       // 不压缩

    // 单例模式
    private static ResourceDispatcher instance;

    // 压缩器实例缓存
    private final Map<Byte, ICompressor> compressorCache;

    // 文件类型映射表（后缀 -> 策略ID）
    private final Map<String, Byte> extensionToStrategy;

    public ResourceDispatcher() {
        this.compressorCache = new HashMap<>();
        this.extensionToStrategy = new HashMap<>();

        initExtensionMap();
    }

    /**
     * 初始化文件类型映射表
     */
    private void initExtensionMap() {
        // 文本类 - 使用WebDict压缩
        extensionToStrategy.put(".html", STRATEGY_WEBDICT);
        extensionToStrategy.put(".htm", STRATEGY_WEBDICT);
        extensionToStrategy.put(".css", STRATEGY_WEBDICT);
        extensionToStrategy.put(".js", STRATEGY_WEBDICT);
        extensionToStrategy.put(".txt", STRATEGY_WEBDICT);
        extensionToStrategy.put(".xml", STRATEGY_WEBDICT);
        extensionToStrategy.put(".json", STRATEGY_WEBDICT);
        extensionToStrategy.put(".svg", STRATEGY_WEBDICT);
        extensionToStrategy.put(".md", STRATEGY_WEBDICT);

        // 图片类 - 使用池化压缩
        extensionToStrategy.put(".jpg", STRATEGY_POOLING_IMAGE);
        extensionToStrategy.put(".jpeg", STRATEGY_POOLING_IMAGE);
        extensionToStrategy.put(".png", STRATEGY_POOLING_IMAGE);
        extensionToStrategy.put(".gif", STRATEGY_POOLING_IMAGE);
        extensionToStrategy.put(".webp", STRATEGY_POOLING_IMAGE);
        extensionToStrategy.put(".bmp", STRATEGY_POOLING_IMAGE);

        // 二进制类 - 使用LZ77
        extensionToStrategy.put(".woff", STRATEGY_LZ77);
        extensionToStrategy.put(".woff2", STRATEGY_LZ77);
        extensionToStrategy.put(".ttf", STRATEGY_LZ77);
        extensionToStrategy.put(".eot", STRATEGY_LZ77);
        extensionToStrategy.put(".otf", STRATEGY_LZ77);
        extensionToStrategy.put(".ico", STRATEGY_LZ77);
        extensionToStrategy.put(".bin", STRATEGY_LZ77);

        // 其他未知类型 - 不压缩
        extensionToStrategy.put(".unknown", STRATEGY_NONE);
    }

    /**
     * 获取单例实例
     */
    public static synchronized ResourceDispatcher getInstance() {
        if (instance == null) {
            instance = new ResourceDispatcher();
        }
        return instance;
    }

    /**
     * 根据文件后缀名获取压缩策略ID
     */
    public byte getStrategyByExtension(String filename) {
        if (filename == null) return STRATEGY_NONE;

        String lower = filename.toLowerCase();
        for (Map.Entry<String, Byte> entry : extensionToStrategy.entrySet()) {
            if (lower.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return STRATEGY_NONE;
    }

    /**
     * ��据MIME类型获取压缩策略ID
     */
    public byte getStrategyByMimeType(String mimeType) {
        if (mimeType == null) return STRATEGY_NONE;

        String lower = mimeType.toLowerCase();
        if (lower.contains("html") || lower.contains("css") || lower.contains("javascript") ||
            lower.contains("text") || lower.contains("xml") || lower.contains("json")) {
            return STRATEGY_WEBDICT;
        }
        if (lower.contains("image")) {
            return STRATEGY_POOLING_IMAGE;
        }
        if (lower.contains("font") || lower.contains("octet-stream")) {
            return STRATEGY_LZ77;
        }
        return STRATEGY_NONE;
    }

    /**
     * 根据策略ID获取压缩器实例
     */
    public ICompressor getCompressor(byte strategyId) throws IOException {
        ICompressor compressor = compressorCache.get(strategyId);
        if (compressor != null) {
            return compressor;
        }

        switch (strategyId) {
            case STRATEGY_WEBDICT:
                try {
                    compressor = new LZ77Compressor(); // 使用LZ77替代WebDict避免内存问题
                } catch (Exception e) {
                    return null;
                }
                break;
            case STRATEGY_LZ77:
                compressor = new LZ77Compressor();
                break;
            case STRATEGY_LZW_IMAGE:
                compressor = new LZWImageCompressor();
                break;
            case STRATEGY_POOLING_IMAGE:
                try {
                    compressor = new PoolingImageCompressor(5); // 默认中等质量
                } catch (Exception e) {
                    return null; // 图片解析失败，不压缩
                }
                break;
            case STRATEGY_NONE:
            default:
                return null; // 不压缩
        }

        compressorCache.put(strategyId, compressor);
        return compressor;
    }

    /**
     * 根据文件后缀名获取压缩器
     */
    public ICompressor getCompressorByExtension(String filename) throws IOException {
        byte strategyId = getStrategyByExtension(filename);
        return getCompressor(strategyId);
    }

    /**
     * 根据MIME类型获取压缩器
     */
    public ICompressor getCompressorByMimeType(String mimeType) throws IOException {
        byte strategyId = getStrategyByMimeType(mimeType);
        return getCompressor(strategyId);
    }

    /**
     * 压缩数据（根据文件类型自动选择算法）
     */
    public CompressionResult compress(String filename, byte[] data) throws IOException {
        byte strategyId = getStrategyByExtension(filename);
        ICompressor compressor = getCompressor(strategyId);

        if (compressor == null) {
            return new CompressionResult(filename, data, data.length, STRATEGY_NONE, "不压缩");
        }

        try {
            long startTime = System.currentTimeMillis();
            byte[] compressed = compressor.compress(data);
            long elapsed = System.currentTimeMillis() - startTime;

            return new CompressionResult(
                filename,
                compressed,
                data.length,
                strategyId,
                compressor.getAlgorithmName()
            );
        } catch (IOException e) {
            // 压缩失败，返回原始数据不压缩
            return new CompressionResult(filename, data, data.length, STRATEGY_NONE, "压缩失败-原样");
        }
    }

    /**
     * 解压缩数据（根据策略ID选择算法）
     */
    public byte[] decompress(byte strategyId, byte[] compressedData) throws IOException {
        ICompressor compressor = getCompressor(strategyId);
        if (compressor == null) {
            return compressedData; // 未经压缩
        }
        return compressor.decompress(compressedData);
    }

    /**
     * 获取策略名称
     */
    public String getStrategyName(byte strategyId) {
        switch (strategyId) {
            case STRATEGY_WEBDICT: return "WebDict";
            case STRATEGY_LZ77: return "LZ77";
            case STRATEGY_LZW_IMAGE: return "LZWImage";
            case STRATEGY_POOLING_IMAGE: return "PoolingImage";
            case STRATEGY_NONE: return "None";
            default: return "Unknown";
        }
    }

    /**
     * 压缩结果封装类
     */
    public static class CompressionResult {
        public final String filename;
        public final byte[] compressedData;
        public final long originalSize;
        public final byte strategyId;
        public final String strategyName;

        public CompressionResult(String filename, byte[] compressedData,
                                 long originalSize, byte strategyId, String strategyName) {
            this.filename = filename;
            this.compressedData = compressedData;
            this.originalSize = originalSize;
            this.strategyId = strategyId;
            this.strategyName = strategyName;
        }

        public double getCompressionRatio() {
            return originalSize > 0 ? (double) compressedData.length / originalSize : 1.0;
        }

        public double getSavingsPercent() {
            return (1.0 - getCompressionRatio()) * 100;
        }
    }
}
