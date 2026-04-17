package compressor.gui;

import compressor.algorithms.*;
import compressor.core.ICompressor;
import compressor.model.CompressionStats;
import compressor.utils.FileUtils;
import compressor.utils.TransferSimulator;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class CompressionService extends Service<CompressionService.CompressionResult> {

    private List<Path> selectedFiles;
    private CompressorFactory.CompressorType compressorType;
    private Path outputDirectory;
    private Consumer<String> logCallback;
    private boolean smartMode = true;
    private int imageQuality = 5;

    public CompressionService() {
    }

    public void configure(List<Path> files, CompressorFactory.CompressorType type,
                         Path outputDir, Consumer<String> logCallback) {
        this.configure(files, type, outputDir, logCallback, true, 5);
    }

    public void configure(List<Path> files, CompressorFactory.CompressorType type,
                         Path outputDir, Consumer<String> logCallback,
                         boolean smartMode, int imageQuality) {
        this.selectedFiles = files;
        this.compressorType = type;
        this.outputDirectory = outputDir;
        this.logCallback = logCallback;
        this.smartMode = smartMode;
        this.imageQuality = imageQuality;
    }

    @Override
    protected Task<CompressionResult> createTask() {
        return new Task<>() {
            @Override
            protected CompressionResult call() throws Exception {
                CompressionResult result = new CompressionResult();

                log("开始压缩任务...");
                log("选中文件数: " + selectedFiles.size());
                log("使用智能匹配：根据文件类型自动选择压缩算法");
                log("=".repeat(50));

                long totalOriginal = 0;
                long totalCompressed = 0;
                long startTime = System.currentTimeMillis();

                for (int i = 0; i < selectedFiles.size(); i++) {
                    Path file = selectedFiles.get(i);
                    updateProgress(i, selectedFiles.size());

                    if (!Files.exists(file)) {
                        log("跳过不存在的文件: " + file.getFileName());
                        continue;
                    }

                    try {
                        // 根据模式选择压缩器
                        String extension = FileUtils.getFileExtension(file.getFileName().toString());
                        CompressorFactory.CompressorType useType;

                        if (smartMode) {
                            // 智能模式：根据文件类型自动选择
                            useType = CompressorFactory.getTypeFromExtension(extension);
                        } else {
                            // 手动模式：使用用户选择的算法
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
                            i + 1, selectedFiles.size(),
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
                            result.setTrieTree(webDict.getTrieTreeVisualization());
                            result.setPipelineInfo(webDict.getCompressionPipelineInfo());
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
                result.setAlgorithmName("智能匹配");

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

        public long getTotalOriginalSize() { return totalOriginalSize; }
        public long getTotalCompressedSize() { return totalCompressedSize; }
        public long getTotalElapsedMillis() { return totalElapsedMillis; }
        public String getAlgorithmName() { return algorithmName; }
        public String getHuffmanTree() { return huffmanTree; }
        public String getTrieTree() { return trieTree; }
        public String getPipelineInfo() { return pipelineInfo; }
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
