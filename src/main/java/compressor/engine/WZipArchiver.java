package compressor.engine;

import compressor.core.ICompressor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 网页归档打包器 (.wzip格式)
 * 
 * 归档格式：
 * - 全局文件头：魔数(4) + 版本(1) + 文件总数(4)
 * - 文件索引与数据区（循环）：
 *   - 文件相对路径长度(2) + 路径字节数据(String)
 *   - 原始文件大小(8)
 *   - 压缩后数据大小(8)
 *   - 压缩策略ID(1)
 *   - 压缩后的数据块(byte[])
 */
public class WZipArchiver {

    // 魔数 "WZIP"
    private static final byte[] MAGIC = new byte[]{'W', 'Z', 'I', 'P'};
    private static final byte VERSION = 1;

    private final ResourceDispatcher dispatcher;
    private Consumer<String> logCallback;

    // 压缩器类型名称到策略ID的映射
    private static final Map<String, Byte> COMPRESSOR_NAME_TO_STRATEGY = new HashMap<>();
    static {
        COMPRESSOR_NAME_TO_STRATEGY.put("Brotli", ResourceDispatcher.STRATEGY_BROTLI);
        COMPRESSOR_NAME_TO_STRATEGY.put("LZ77", ResourceDispatcher.STRATEGY_LZ77);
        COMPRESSOR_NAME_TO_STRATEGY.put("Huffman", ResourceDispatcher.STRATEGY_LZ77); // Huffman使用LZ77策略
        COMPRESSOR_NAME_TO_STRATEGY.put("LZWImage", ResourceDispatcher.STRATEGY_LZW_IMAGE);
        COMPRESSOR_NAME_TO_STRATEGY.put("PoolingImage", ResourceDispatcher.STRATEGY_POOLING_IMAGE);
        COMPRESSOR_NAME_TO_STRATEGY.put("WebDict", ResourceDispatcher.STRATEGY_WEBDICT);
    }

    public WZipArchiver() {
        this.dispatcher = ResourceDispatcher.getInstance();
    }

    public WZipArchiver(ResourceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        }
        System.out.println(message);
    }

    /**
     * 打包：将网页文件夹归档为.wzip文件（使用智能选择）
     * @param inputDir 输入文件夹路径
     * @param outputFile 输出.wzip文件路径
     */
    public void archive(Path inputDir, Path outputFile) throws IOException {
        if (!Files.isDirectory(inputDir)) {
            throw new IOException("输入路径不是有效的目录: " + inputDir);
        }

        // 收集所有文件
        List<FileEntry> entries = collectFiles(inputDir, inputDir);

        if (entries.isEmpty()) {
            throw new IOException("没有有效的文件可打包");
        }

        // 写入归档文件
        try (OutputStream out = new BufferedOutputStream(
                new FileOutputStream(outputFile.toFile()))) {
            writeHeader(out, entries.size());

            for (FileEntry entry : entries) {
                writeEntryWithStrategy(out, entry, null, ResourceDispatcher.STRATEGY_NONE, "智能匹配");
            }
        }

        log("归档完成: " + entries.size() + " 个文件 -> " + outputFile);
    }

    /**
     * 获取压缩器对应的策略ID
     */
    private byte getStrategyIdForCompressor(ICompressor compressor) {
        if (compressor == null) {
            return ResourceDispatcher.STRATEGY_NONE; // 智能模式
        }
        String name = compressor.getAlgorithmName();
        Byte strategyId = COMPRESSOR_NAME_TO_STRATEGY.get(name);
        if (strategyId != null) {
            return strategyId;
        }
        // 未知压缩器，默认使用LZ77
        return ResourceDispatcher.STRATEGY_LZ77;
    }

    /**
     * 写入单个文件条目（使用指定压缩器和策略）
     */
    private void writeEntryWithStrategy(OutputStream out, FileEntry entry, ICompressor compressor,
                                        byte strategyId, String strategyName) throws IOException {
        // 读取原始文件内容
        byte[] originalData = Files.readAllBytes(entry.path);
        long originalSize = originalData.length;

        byte[] compressedData;
        String actualStrategyName = strategyName;

        // 根据是否指定压缩器决定压缩方式
        if (compressor != null) {
            // 使用指定压缩器
            compressedData = compressor.compress(originalData);
            actualStrategyName = compressor.getAlgorithmName();
        } else {
            // 使用ResourceDispatcher智能选择
            ResourceDispatcher.CompressionResult result = dispatcher.compress(
                    entry.relativePath, originalData);
            compressedData = result.compressedData;
            strategyId = result.strategyId;
            actualStrategyName = result.strategyName;

            // 诊断日志：显示策略选择过程
            String extension = getFileExtension(entry.relativePath);
            System.out.println("[智能匹配诊断] 文件: " + entry.relativePath +
                    " | 扩展名: " + extension +
                    " | 策略ID: " + strategyId +
                    " | 算法: " + actualStrategyName +
                    " | 原始: " + originalSize + " bytes" +
                    " | 压缩后: " + compressedData.length + " bytes");
        }

        // 计算元数据开销
        byte[] pathBytes = entry.relativePath.getBytes(StandardCharsets.UTF_8);
        long metadataSize = 2 + pathBytes.length + 8 + 8 + 1; // 路径长度+路径+原始大小+压缩后大小+策略ID

        // 写入相对路径（先写入长度，再写入内容）
        out.write((pathBytes.length >> 8) & 0xFF);
        out.write(pathBytes.length & 0xFF);
        out.write(pathBytes);

        // 写入元数据
        writeLong(out, originalSize);
        writeLong(out, compressedData.length);
        out.write(strategyId);

        // 写入压缩数据
        out.write(compressedData);

        double savingsPercent = originalSize > 0 ? (1.0 - (double) compressedData.length / originalSize) * 100 : 0;
        double netSavingsPercent = originalSize > 0 ? (1.0 - (double) (compressedData.length + metadataSize) / originalSize) * 100 : 0;

        // 诊断日志：显示实际压缩效果
        System.out.println("  归档: " + entry.relativePath +
                " (" + actualStrategyName + ", " +
                String.format("原始压缩率:%.1f%%", savingsPercent) + ", " +
                String.format("净压缩率(含元数据%.0fB):%.1f%%", (double)metadataSize, netSavingsPercent) + ")");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    /**
     * 打包：归档单个文件为.wzip文件（使用指定压缩器）
     * @param file 要打包的文件
     * @param outputFile 输出.wzip文件路径
     * @param compressor 指定使用的压缩器（为null时使用ResourceDispatcher智能选择）
     */
    public void archiveSingleFile(Path file, Path outputFile, ICompressor compressor) throws IOException {
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IOException("无效的文件路径: " + file);
        }

        List<Path> files = new ArrayList<>();
        files.add(file);
        // 使用文件名作为相对路径
        archiveFiles(files, file.getParent(), outputFile, compressor);
    }

    /**
     * 打包：归档单个文件为.wzip文件（使用智能选择）
     */
    public void archiveSingleFile(Path file, Path outputFile) throws IOException {
        archiveSingleFile(file, outputFile, null);
    }

    /**
     * 打包：将文件列表归档为.wzip文件（使用指定压缩器）
     * @param files 要打包的文件列表
     * @param baseDir 基础目录（用于计算相对路径）
     * @param outputFile 输出.wzip文件路径
     * @param compressor 指定使用的压缩器（为null时使用ResourceDispatcher智能选择）
     */
    public void archiveFiles(List<Path> files, Path baseDir, Path outputFile, ICompressor compressor) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IOException("文件列表为空");
        }

        // 收集所有文件条目
        List<FileEntry> entries = new ArrayList<>();
        for (Path file : files) {
            if (!Files.exists(file)) {
                log("跳过不存在的文件: " + file);
                continue;
            }
            if (Files.isDirectory(file)) {
                // 如果是目录，递归收集
                entries.addAll(collectFiles(file, baseDir));
            } else {
                String relativePath = baseDir.relativize(file).toString().replace("\\", "/");
                entries.add(new FileEntry(file, relativePath));
            }
        }

        if (entries.isEmpty()) {
            throw new IOException("没有有效的文件可打包");
        }

        // 获取压缩器的策略ID
        byte strategyId = getStrategyIdForCompressor(compressor);
        String strategyName = compressor != null ? compressor.getAlgorithmName() : "智能匹配";

        // 写入归档文件
        try (OutputStream out = new BufferedOutputStream(
                new FileOutputStream(outputFile.toFile()))) {
            writeHeader(out, entries.size());

            for (FileEntry entry : entries) {
                writeEntryWithStrategy(out, entry, compressor, strategyId, strategyName);
            }
        }

        log("归档完成: " + entries.size() + " 个文件 -> " + outputFile + " (使用" + strategyName + ")");
    }

    /**
     * 打包：归档文件列表为.wzip文件（使用智能选择）
     */
    public void archiveFiles(List<Path> files, Path baseDir, Path outputFile) throws IOException {
        archiveFiles(files, baseDir, outputFile, null);
    }

    /**
     * 解包：还原.wzip文件到目标目录
     * @param wzipFile .wzip归档文件
     * @param outputDir 输出目录
     */
    public void extract(Path wzipFile, Path outputDir) throws IOException {
        if (!Files.exists(wzipFile)) {
            throw new IOException("归档文件不存在: " + wzipFile);
        }

        Files.createDirectories(outputDir);

        try (InputStream in = new BufferedInputStream(
                new FileInputStream(wzipFile.toFile()))) {
            // 读取并验证魔数
            byte[] magic = new byte[4];
            in.read(magic);
            if (magic[0] != 'W' || magic[1] != 'Z' || magic[2] != 'I' || magic[3] != 'P') {
                throw new IOException("无效的WZIP格式，魔数错误");
            }

            // 读取版本
            int version = in.read();
            if (version != VERSION) {
                throw new IOException("不支持的WZIP版本: " + version);
            }

            // 读取文件总数
            int fileCount = readInt(in) << 24 | readInt(in) << 16 |
                           readInt(in) << 8 | readInt(in);

            // 逐个提取文件
            for (int i = 0; i < fileCount; i++) {
                extractEntry(in, outputDir);
            }
        }

        System.out.println("解包完成: " + wzipFile + " -> " + outputDir);
    }

    /**
     * 收集目录下所有文件
     */
    private List<FileEntry> collectFiles(Path dir, Path baseDir) throws IOException {
        List<FileEntry> entries = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    entries.addAll(collectFiles(path, baseDir));
                } else {
                    String relativePath = baseDir.relativize(path).toString()
                            .replace("\\", "/"); // 统一使用正斜杠
                    entries.add(new FileEntry(path, relativePath));
                }
            }
        }

        return entries;
    }

    /**
     * 写入全局文件头
     */
    private void writeHeader(OutputStream out, int fileCount) throws IOException {
        out.write(MAGIC);
        out.write(VERSION);
        out.write((fileCount >> 24) & 0xFF);
        out.write((fileCount >> 16) & 0xFF);
        out.write((fileCount >> 8) & 0xFF);
        out.write(fileCount & 0xFF);
    }

    /**
     * 提取单个文件条目
     */
    private void extractEntry(InputStream in, Path outputDir) throws IOException {
        // 读取路径长度和路径
        int pathLen = (in.read() << 8) | in.read();
        byte[] pathBytes = new byte[pathLen];
        in.read(pathBytes);
        String relativePath = new String(pathBytes, StandardCharsets.UTF_8);

        // 读取元数据
        long originalSize = readLong(in);
        long compressedSize = readLong(in);
        byte strategyId = (byte) in.read();

        // 读取压缩数据
        byte[] compressedData = new byte[(int) compressedSize];
        in.read(compressedData);

        // 解压缩
        byte[] originalData;
        if (strategyId == ResourceDispatcher.STRATEGY_NONE) {
            originalData = compressedData;
        } else {
            originalData = dispatcher.decompress(strategyId, compressedData);
        }

        // 创建目录结构
        Path filePath = outputDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());

        // 写入文件
        Files.write(filePath, originalData);

        System.out.println("  还原: " + relativePath + " (" + originalSize + " bytes)");
    }

    /**
     * 写入8字节长整数
     */
    private void writeLong(OutputStream out, long value) throws IOException {
        out.write((byte) ((value >> 56) & 0xFF));
        out.write((byte) ((value >> 48) & 0xFF));
        out.write((byte) ((value >> 40) & 0xFF));
        out.write((byte) ((value >> 32) & 0xFF));
        out.write((byte) ((value >> 24) & 0xFF));
        out.write((byte) ((value >> 16) & 0xFF));
        out.write((byte) ((value >> 8) & 0xFF));
        out.write((byte) (value & 0xFF));
    }

    /**
     * 读取单字节（转换为int）
     */
    private int readInt(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) throw new IOException("意外的文件结束");
        return b & 0xFF;
    }

    /**
     * 读取8字节长整数
     */
    private long readLong(InputStream in) throws IOException {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (in.read() & 0xFF);
        }
        return value;
    }

    /**
     * 文件条目封装
     */
    private static class FileEntry {
        final Path path;
        final String relativePath;

        FileEntry(Path path, String relativePath) {
            this.path = path;
            this.relativePath = relativePath;
        }
    }

    /**
     * 测试 main 方法
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== WZipArchiver 测试 ===\n");

        WZipArchiver archiver = new WZipArchiver();

        // 创建测试网页目录
        Path testDir = createTestWebpage();
        Path testWzip = Paths.get("test_output.wzip");
        Path extractDir = Paths.get("test_extracted");

        try {
            // 测试打包
            System.out.println("--- 测试打包 ---");
            archiver.archive(testDir, testWzip);

            long wzipSize = Files.size(testWzip);
            System.out.println("WZIP文件大小: " + wzipSize + " bytes\n");

            // 测试解包
            System.out.println("--- 测试解包 ---");
            archiver.extract(testWzip, extractDir);

            System.out.println("\n--- 验证 ---");
            verifyExtraction(testDir, extractDir);

            System.out.println("\n✓ 测试通过！");

        } finally {
            // 清理测试文件
            cleanup(testDir, testWzip, extractDir);
        }
    }

    /**
     * 创建测试网页目录
     */
    private static Path createTestWebpage() throws IOException {
        Path dir = Paths.get("test_webpage");
        Files.createDirectories(dir);

        // 创建 index.html
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>测试网页</title>
                <link rel="stylesheet" href="css/style.css">
            </head>
            <body>
                <header><h1>欢迎访问</h1></header>
                <main><p>这是一个测试网页</p></main>
                <script src="js/main.js"></script>
            </body>
            </html>
            """;
        Files.writeString(dir.resolve("index.html"), html);

        // 创建子目录和文件
        Files.createDirectories(dir.resolve("css"));
        Files.writeString(dir.resolve("css/style.css"),
            "body { font-family: Arial; }\n.h1 { color: blue; }");

        Files.createDirectories(dir.resolve("js"));
        Files.writeString(dir.resolve("js/main.js"),
            "function init() { console.log('Hello'); }");

        Files.createDirectories(dir.resolve("images"));
        byte[] imgData = new byte[100];
        Files.write(dir.resolve("images/icon.png"), imgData);

        System.out.println("测试目录已创建: " + dir.toAbsolutePath());
        return dir;
    }

    /**
     * 验证解包结果
     */
    private static void verifyExtraction(Path original, Path extracted) throws IOException {
        // 简单验证：检查文件是否存在
        String[] files = {"index.html", "css/style.css", "js/main.js", "images/icon.png"};
        for (String f : files) {
            Path p = extracted.resolve(f);
            if (Files.exists(p)) {
                System.out.println("✓ 存在: " + f);
            } else {
                System.out.println("✗ 缺失: " + f);
            }
        }
    }

    /**
     * 清理测试文件
     */
    private static void cleanup(Path... paths) throws IOException {
        for (Path p : paths) {
            if (p != null && Files.exists(p)) {
                if (p.toFile().isDirectory()) {
                    deleteDirectory(p);
                } else {
                    Files.delete(p);
                }
            }
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    deleteDirectory(p);
                } else {
                    Files.delete(p);
                }
            }
        }
        Files.delete(dir);
    }
}
