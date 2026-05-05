package compressor.gui;

import compressor.algorithms.*;
import compressor.core.ICompressor;
import compressor.engine.WZipArchiver;
import compressor.model.CompressionStats;
import compressor.utils.FileUtils;
import compressor.utils.TransferSimulator;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CompressionService extends Service<CompressionService.CompressionResult> {

    private List<Path> selectedFiles;
    private CompressorFactory.CompressorType compressorType;
    private Path outputDirectory;
    private String outputFileName;
    private Consumer<String> logCallback;
    private boolean smartMode = true;
    private int imageQuality = 5;
    private boolean useWZipArchive = true;
    private ICompressor specifiedCompressor; // 用户指定的压缩器

    public CompressionService() {
    }

    public void configure(List<Path> files, CompressorFactory.CompressorType type,
                         Path outputDir, Consumer<String> logCallback) {
        this.configure(files, type, outputDir, null, logCallback, true, 5, true);
    }

    public void configure(List<Path> files, CompressorFactory.CompressorType type,
                         Path outputDir, Consumer<String> logCallback,
                         boolean smartMode, int imageQuality) {
        // 默认使用归档模式
        this.configure(files, type, outputDir, null, logCallback, smartMode, imageQuality, true);
    }

    public void configure(List<Path> files, CompressorFactory.CompressorType type,
                         Path outputDir, String outputFileName, Consumer<String> logCallback,
                         boolean smartMode, int imageQuality, boolean useWZipArchive) {
        this.selectedFiles = files;
        this.compressorType = type;
        this.outputDirectory = outputDir;
        this.outputFileName = outputFileName;
        this.logCallback = logCallback;
        this.smartMode = smartMode;
        this.imageQuality = imageQuality;
        this.useWZipArchive = useWZipArchive;
        this.specifiedCompressor = null; // 始终使用智能模式创建的压缩器
    }

    /**
     * 配置方法：支持指定压缩器
     * @param files 要压缩的文件列表
     * @param compressor 用户指定的压缩器（用于归档模式）
     * @param outputDir 输出目录
     * @param logCallback 日志回调
     * @param useWZipArchive 是否使用wzip归档模式
     */
    public void configureWithCompressor(List<Path> files, ICompressor compressor,
                                        Path outputDir, Consumer<String> logCallback,
                                        boolean useWZipArchive) {
        this.selectedFiles = files;
        this.specifiedCompressor = compressor;
        this.outputDirectory = outputDir;
        this.outputFileName = null;
        this.logCallback = logCallback;
        this.smartMode = false; // 使用指定压缩器
        this.imageQuality = 5;
        this.useWZipArchive = useWZipArchive;
    }

    @Override
    protected Task<CompressionResult> createTask() {
        return new Task<>() {
            @Override
            protected CompressionResult call() throws Exception {
                CompressionResult result = new CompressionResult();

                log("开始压缩任务...");
                log("选中文件数: " + selectedFiles.size());

                if (smartMode) {
                    log("使用智能匹配：根据文件类型自动选择压缩算法");
                } else if (specifiedCompressor != null) {
                    log("使用指定压缩器: " + specifiedCompressor.getAlgorithmName());
                } else {
                    log("使用算法: " + compressorType.getName());
                }
                log("=".repeat(50));

                long totalOriginal = 0;
                long startTime = System.currentTimeMillis();

                if (useWZipArchive) {
                    // 使用 WZip 归档模式：生成单个压缩包
                    result = compressToArchive(selectedFiles, startTime);
                } else {
                    // 原有模式：每个文件单独压缩（保留兼容性）
                    result = compressIndividualFiles(selectedFiles, startTime);
                }

                return result;
            }

            private CompressionResult compressToArchive(List<Path> files, long startTime) throws Exception {
                CompressionResult result = new CompressionResult();

                // 收集所有文件（包括目录内的文件）
                List<Path> allFiles = new ArrayList<>();
                for (Path file : files) {
                    if (Files.isDirectory(file)) {
                        try (java.nio.file.DirectoryStream<Path> stream =
                             Files.newDirectoryStream(file)) {
                            for (Path p : stream) {
                                if (Files.isRegularFile(p)) {
                                    allFiles.add(p);
                                }
                            }
                        }
                    } else if (Files.isRegularFile(file)) {
                        allFiles.add(file);
                    }
                }

                if (allFiles.isEmpty()) {
                    log("没有可压缩的文件");
                    return result;
                }

                // 计算原始总大小
                long totalOriginal = 0;
                for (Path file : allFiles) {
                    totalOriginal += Files.size(file);
                }

                // 生成输出文件名
                String archiveFileName;
                if (outputFileName != null && !outputFileName.isEmpty()) {
                    archiveFileName = outputFileName;
                } else if (files.size() == 1 && Files.isDirectory(files.get(0))) {
                    archiveFileName = files.get(0).getFileName() + ".wzip";
                } else if (files.size() == 1 && Files.isRegularFile(files.get(0))) {
                    String baseName = files.get(0).getFileName().toString();
                    int dotIdx = baseName.lastIndexOf('.');
                    archiveFileName = (dotIdx > 0 ? baseName.substring(0, dotIdx) : baseName) + ".wzip";
                } else {
                    archiveFileName = "archive_" + System.currentTimeMillis() + ".wzip";
                }

                Path outputPath = outputDirectory.resolve(archiveFileName);

                // 使用 WZipArchiver 打包
                WZipArchiver archiver = new WZipArchiver();
                archiver.setLogCallback(logCallback);

                // 确定基础目录
                Path baseDir;
                if (files.size() == 1 && Files.isDirectory(files.get(0))) {
                    baseDir = files.get(0);
                } else {
                    // 多个文件时，使用第一个文件的父目录
                    baseDir = files.get(0).getParent();
                }

                // 确定要使用的压缩器
                ICompressor compressorToUse = null;
                String algorithmName = "智能匹配";
                if (!smartMode) {
                    if (specifiedCompressor != null) {
                        compressorToUse = specifiedCompressor;
                        algorithmName = specifiedCompressor.getAlgorithmName();
                    } else if (compressorType != null) {
                        compressorToUse = createCompressorWithQuality(compressorType, "");
                        algorithmName = compressorType.getName();
                    }
                }

                if (compressorToUse != null) {
                    archiver.archiveFiles(allFiles, baseDir, outputPath, compressorToUse);

                    if (compressorToUse instanceof HuffmanCompressor huffman) {
                        result.setHuffmanTree(huffman.getTreeStructure());
                        result.setCodeTable(huffman.getCodeTable());
                    }
                } else {
                    archiver.archiveFiles(allFiles, baseDir, outputPath);
                }

                // 获取压缩后大小
                long totalCompressed = Files.size(outputPath);
                long elapsed = System.currentTimeMillis() - startTime;

                // 添加文件结果
                for (Path file : allFiles) {
                    String relativePath = baseDir.relativize(file).toString();
                    result.addFileResult(new CompressionResult.FileResult(
                        relativePath,
                        Files.size(file),
                        0, // 单个文件压缩后大小不单独统计
                        0,
                        algorithmName
                    ));
                }

                result.setTotalOriginalSize(totalOriginal);
                result.setTotalCompressedSize(totalCompressed);
                result.setTotalElapsedMillis(elapsed);
                result.setAlgorithmName(algorithmName);
                result.setOutputArchivePath(outputPath.toString());

                TransferSimulator simulator = new TransferSimulator(TransferSimulator.BandwidthProfile.FOUR_G);
                TransferSimulator.TransferEstimate estimate = simulator.estimateTransfer(totalOriginal, totalCompressed);
                result.setTransferEstimate(estimate);

                log("");
                log("=".repeat(50));
                log("压缩完成!");
                log("总耗时: " + elapsed + "ms");
                log("原始大小: " + formatSize(totalOriginal));
                log("压缩后: " + formatSize(totalCompressed));
                if (totalOriginal > 0) {
                    double ratio = (1.0 - (double) totalCompressed / totalOriginal) * 100;
                    log("总压缩率: " + String.format("%.2f%%", ratio));
                }
                log("输出文件: " + outputPath);
                log("=".repeat(50));

                return result;
            }

            private CompressionResult compressIndividualFiles(List<Path> files, long startTime) throws Exception {
                CompressionResult result = new CompressionResult();

                long totalOriginal = 0;
                long totalCompressed = 0;

                for (int i = 0; i < files.size(); i++) {
                    Path file = files.get(i);
                    updateProgress(i, files.size());

                    if (!Files.exists(file) || Files.isDirectory(file)) {
                        log("跳过: " + file.getFileName());
                        continue;
                    }

                    try {
                        String extension = FileUtils.getFileExtension(file.getFileName().toString());
                        CompressorFactory.CompressorType useType;

                        if (smartMode) {
                            useType = CompressorFactory.getTypeFromExtension(extension);
                        } else {
                            useType = compressorType;
                        }

                        ICompressor compressor = createCompressorWithQuality(useType, extension);

                        long fileSize = Files.size(file);
                        totalOriginal += fileSize;

                        byte[] data = FileUtils.readFileToBytes(file);
                        byte[] compressed = compressor.compress(data);

                        Path outputPath = outputDirectory.resolve(file.getFileName() + ".compressed");
                        FileUtils.writeBytesToFile(outputPath, compressed);

                        long compressedSize = compressed.length;
                        totalCompressed += compressedSize;

                        CompressionStats stats = compressor.getStats();
                        double savingsPercent = (1.0 - (double) compressedSize / fileSize) * 100;

                        log(String.format("[%d/%d] %s (%s) -> %s (节省 %.1f%%)",
                            i + 1, files.size(),
                            file.getFileName(),
                            useType.getName(),
                            formatSize(compressedSize),
                            savingsPercent));

                        result.addFileResult(new CompressionResult.FileResult(
                            file.getFileName().toString(),
                            fileSize,
                            compressedSize,
                            stats.getElapsedMillis(),
                            useType.getName()
                        ));

                        if (compressor instanceof HuffmanCompressor huffman) {
                            result.setHuffmanTree(huffman.getTreeStructure());
                            result.setCodeTable(huffman.getCodeTable());
                        }

                        if (compressor instanceof WebDictCompressor webDict) {
                            result.setPipelineInfo(webDict.getCompressionInfo());
                        }

                    } catch (Exception e) {
                        log("压缩失败: " + file.getFileName() + " - " + e.getMessage());
                        result.addError(file.getFileName().toString(), e.getMessage());
                    }
                }

                long elapsed = System.currentTimeMillis() - startTime;
                result.setTotalOriginalSize(totalOriginal);
                result.setTotalCompressedSize(totalCompressed);
                result.setTotalElapsedMillis(elapsed);
                result.setAlgorithmName(smartMode ? "智能匹配" : (compressorType != null ? compressorType.getName() : "未知"));

                TransferSimulator simulator = new TransferSimulator(TransferSimulator.BandwidthProfile.FOUR_G);
                TransferSimulator.TransferEstimate estimate = simulator.estimateTransfer(totalOriginal, totalCompressed);
                result.setTransferEstimate(estimate);

                log("");
                log("=".repeat(50));
                log("压缩完成!");
                log("总耗时: " + elapsed + "ms");
                log("原始大小: " + formatSize(totalOriginal));
                log("压缩后: " + formatSize(totalCompressed));
                if (totalOriginal > 0) {
                    double ratio = (1.0 - (double) totalCompressed / totalOriginal) * 100;
                    log("总压缩率: " + String.format("%.2f%%", ratio));
                }
                log("=".repeat(50));

                return result;
            }
        };
    }

    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private ICompressor createCompressorWithQuality(CompressorFactory.CompressorType type, String extension) {
        if (type == CompressorFactory.CompressorType.POOLING_IMAGE) {
            return new PoolingImageCompressor(imageQuality);
        }
        return CompressorFactory.createCompressor(type);
    }

    public static class CompressionResult {
        private long totalOriginalSize;
        private long totalCompressedSize;
        private long totalElapsedMillis;
        private String algorithmName;
        private String huffmanTree;
        private String trieTree;
        private String pipelineInfo;
        private String outputArchivePath;
        private java.util.Map<Integer, String> codeTable;
        private TransferSimulator.TransferEstimate transferEstimate;
        private final java.util.List<FileResult> fileResults = new java.util.ArrayList<>();
        private final java.util.List<ErrorResult> errors = new java.util.ArrayList<>();

        public static class FileResult {
            private final String fileName;
            private final long originalSize;
            private final long compressedSize;
            private final long elapsedMillis;
            private final String algorithm;

            public FileResult(String fileName, long originalSize, long compressedSize,
                            long elapsedMillis, String algorithm) {
                this.fileName = fileName;
                this.originalSize = originalSize;
                this.compressedSize = compressedSize;
                this.elapsedMillis = elapsedMillis;
                this.algorithm = algorithm;
            }

            public String getFileName() { return fileName; }
            public long getOriginalSize() { return originalSize; }
            public long getCompressedSize() { return compressedSize; }
            public long getElapsedMillis() { return elapsedMillis; }
            public String getAlgorithm() { return algorithm; }
            public double getSavingsPercent() {
                if (originalSize <= 0) return 0;
                return (1.0 - (double) compressedSize / originalSize) * 100;
            }
        }

        public static class ErrorResult {
            private final String fileName;
            private final String errorMessage;

            public ErrorResult(String fileName, String errorMessage) {
                this.fileName = fileName;
                this.errorMessage = errorMessage;
            }

            public String getFileName() { return fileName; }
            public String getErrorMessage() { return errorMessage; }
        }

        public void setTotalOriginalSize(long size) { this.totalOriginalSize = size; }
        public void setTotalCompressedSize(long size) { this.totalCompressedSize = size; }
        public void setTotalElapsedMillis(long millis) { this.totalElapsedMillis = millis; }
        public void setAlgorithmName(String name) { this.algorithmName = name; }
        public void setHuffmanTree(String tree) { this.huffmanTree = tree; }
        public void setTrieTree(String tree) { this.trieTree = tree; }
        public void setPipelineInfo(String info) { this.pipelineInfo = info; }
        public void setCodeTable(java.util.Map<Integer, String> table) { this.codeTable = table; }
        public void setTransferEstimate(TransferSimulator.TransferEstimate estimate) { this.transferEstimate = estimate; }
        public void setOutputArchivePath(String path) { this.outputArchivePath = path; }

        public long getTotalOriginalSize() { return totalOriginalSize; }
        public long getTotalCompressedSize() { return totalCompressedSize; }
        public long getTotalElapsedMillis() { return totalElapsedMillis; }
        public String getAlgorithmName() { return algorithmName; }
        public String getHuffmanTree() { return huffmanTree; }
        public String getTrieTree() { return trieTree; }
        public String getPipelineInfo() { return pipelineInfo; }
        public String getOutputArchivePath() { return outputArchivePath; }
        public java.util.Map<Integer, String> getCodeTable() { return codeTable; }
        public TransferSimulator.TransferEstimate getTransferEstimate() { return transferEstimate; }
        public List<FileResult> getFileResults() { return fileResults; }
        public List<ErrorResult> getErrors() { return errors; }

        public void addFileResult(FileResult result) { fileResults.add(result); }
        public void addError(String fileName, String message) { errors.add(new ErrorResult(fileName, message)); }

        public double getOverallCompressionRatio() {
            if (totalOriginalSize <= 0) return 0;
            return (1.0 - (double) totalCompressedSize / totalOriginalSize) * 100;
        }
    }
}
