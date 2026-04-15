package compressor.algorithms;

import compressor.core.ICompressor;
import compressor.model.CompressionManifest;
import compressor.model.CompressionStats;
import compressor.model.FileFingerprint;
import compressor.utils.FileUtils;
import compressor.utils.HashUtils;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class IncrementalCompressor {
    private final ICompressor compressor;
    private final CompressionManifest manifest;
    private final Path outputDir;
    private final boolean parallelProcessing;
    private final int threadCount;

    private Consumer<String> progressCallback;
    private Consumer<FileFingerprint> fileCompleteCallback;

    public IncrementalCompressor(ICompressor compressor, Path outputDir) {
        this(compressor, outputDir, false, 4);
    }

    public IncrementalCompressor(ICompressor compressor, Path outputDir, boolean parallelProcessing, int threadCount) {
        this.compressor = compressor;
        this.outputDir = outputDir;
        this.parallelProcessing = parallelProcessing;
        this.threadCount = threadCount;
        this.manifest = new CompressionManifest(outputDir);
    }

    public CompressionResult compressDirectory(Path sourceDir, Set<String> extensions) throws IOException {
        List<Path> files = collectFiles(sourceDir, extensions);
        return compressFiles(files);
    }

    public CompressionResult compressFiles(List<Path> files) throws IOException {
        CompressionResult result = new CompressionResult();
        result.setStartTime(System.currentTimeMillis());

        publishProgress("开始增量压缩分析...");

        List<Path> modifiedFiles = manifest.getModifiedOrNewFiles(files);
        List<FileFingerprint> unchangedFiles = manifest.getUnchangedFiles(files);

        result.setTotalFiles(files.size());
        result.setModifiedFiles(modifiedFiles.size());
        result.setUnchangedFiles(unchangedFiles.size());

        publishProgress(String.format("分析完成: %d个文件需要压缩, %d个文件未修改",
            modifiedFiles.size(), unchangedFiles.size()));

        for (FileFingerprint fp : unchangedFiles) {
            result.addUnchangedFile(fp);
            result.addBytesSaved(fp.getBytesSaved());
            if (fileCompleteCallback != null) {
                fileCompleteCallback.accept(fp);
            }
        }

        if (parallelProcessing && modifiedFiles.size() > 1) {
            compressInParallel(modifiedFiles, result);
        } else {
            compressSequentially(modifiedFiles, result);
        }

        manifest.setAlgorithmUsed(compressor.getAlgorithmName());
        manifest.save();

        result.setEndTime(System.currentTimeMillis());
        publishProgress("压缩完成! " + result.getSummary());

        return result;
    }

    private void compressSequentially(List<Path> files, CompressionResult result) throws IOException {
        for (Path file : files) {
            try {
                CompressionResult.FileResult fileResult = compressSingleFile(file);
                result.addFileResult(fileResult);
                if (fileResult.isSuccess()) {
                    publishProgress(String.format("已压缩: %s (节省 %d bytes)",
                        file.getFileName(), fileResult.getBytesSaved()));
                } else {
                    publishProgress(String.format("跳过: %s (已有更优压缩)", file.getFileName()));
                }
            } catch (Exception e) {
                publishProgress("压缩失败: " + file.getFileName() + " - " + e.getMessage());
            }
        }
    }

    private void compressInParallel(List<Path> files, CompressionResult result) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<CompressionResult.FileResult>> futures = new ArrayList<>();

        for (Path file : files) {
            Future<CompressionResult.FileResult> future = executor.submit(() -> {
                return compressSingleFile(file);
            });
            futures.add(future);
        }

        for (int i = 0; i < futures.size(); i++) {
            try {
                CompressionResult.FileResult fileResult = futures.get(i).get();
                result.addFileResult(fileResult);
                if (fileResult.isSuccess()) {
                    publishProgress(String.format("[%d/%d] 已压缩: %s",
                        i + 1, files.size(), fileResult.getFileName()));
                }
            } catch (InterruptedException | ExecutionException e) {
                publishProgress("并行压缩出错: " + e.getMessage());
            }
        }

        executor.shutdown();
    }

    private CompressionResult.FileResult compressSingleFile(Path sourceFile) throws IOException {
        CompressionResult.FileResult fileResult = new CompressionResult.FileResult();
        fileResult.setFilePath(sourceFile);
        fileResult.setFileName(sourceFile.getFileName().toString());

        long originalSize = Files.size(sourceFile);
        byte[] originalData = FileUtils.readFileToBytes(sourceFile);
        String md5Hash = HashUtils.md5(originalData);

        FileFingerprint fingerprint = new FileFingerprint(sourceFile, md5Hash, originalSize, LocalDateTime.now());

        byte[] compressedData = compressor.compress(originalData);
        long compressedSize = compressedData.length;

        if (compressedSize >= originalSize) {
            fileResult.setSkipped(true);
            fileResult.setOriginalSize(originalSize);
            fileResult.setCompressedSize(originalSize);
            fileResult.setBytesSaved(0);

            fingerprint.setCompressed(false);
            fingerprint.setCompressedSize(originalSize);
        } else {
            fileResult.setSkipped(false);
            fileResult.setOriginalSize(originalSize);
            fileResult.setCompressedSize(compressedSize);
            fileResult.setBytesSaved(originalSize - compressedSize);

            Path outputPath = outputDir.resolve(sourceFile.getFileName() + ".compressed");
            FileUtils.writeBytesToFile(outputPath, compressedData);

            fingerprint.setCompressed(true);
            fingerprint.setCompressedPath(outputPath.toString());
            fingerprint.setCompressedSize(compressedSize);
            fingerprint.setAlgorithm(compressor.getAlgorithmName());
        }

        fingerprint.setMd5Hash(md5Hash);
        manifest.addFingerprint(fingerprint);

        if (fileCompleteCallback != null) {
            fileCompleteCallback.accept(fingerprint);
        }

        return fileResult;
    }

    public DecompressionResult decompressAll(Path outputDir) throws IOException {
        DecompressionResult result = new DecompressionResult();
        result.setStartTime(System.currentTimeMillis());

        publishProgress("开始解压...");

        int successCount = 0;
        for (FileFingerprint fp : manifest.getAllFingerprints()) {
            if (!fp.isCompressed()) continue;

            try {
                Path compressedPath = Paths.get(fp.getCompressedPath());
                if (!Files.exists(compressedPath)) {
                    publishProgress("找不到压缩文件: " + compressedPath);
                    continue;
                }

                byte[] compressedData = FileUtils.readFileToBytes(compressedPath);
                byte[] originalData = compressor.decompress(compressedData);

                Path outputPath = outputDir.resolve(fp.getFileName());
                FileUtils.writeBytesToFile(outputPath, originalData);

                successCount++;
                result.addBytesRestored(fp.getCompressedSize());
            } catch (Exception e) {
                publishProgress("解压失败: " + fp.getFileName() + " - " + e.getMessage());
            }
        }

        result.setSuccessCount(successCount);
        result.setEndTime(System.currentTimeMillis());
        publishProgress("解压完成: " + successCount + " 个文件");

        return result;
    }

    private List<Path> collectFiles(Path directory, Set<String> extensions) throws IOException {
        List<Path> files = new ArrayList<>();

        if (extensions == null || extensions.isEmpty()) {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .forEach(files::add);
        } else {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String ext = FileUtils.getFileExtension(p);
                    return extensions.contains(ext.toLowerCase());
                })
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .forEach(files::add);
        }

        return files;
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    public void setFileCompleteCallback(Consumer<FileFingerprint> callback) {
        this.fileCompleteCallback = callback;
    }

    public CompressionManifest getManifest() {
        return manifest;
    }

    private void publishProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }

    public static class CompressionResult {
        private long startTime;
        private long endTime;
        private int totalFiles;
        private int modifiedFiles;
        private int unchangedFiles;
        private final List<FileResult> results = new ArrayList<>();
        private final List<FileFingerprint> unchangedFileList = new ArrayList<>();
        private long bytesSaved;

        public static class FileResult {
            private Path filePath;
            private String fileName;
            private long originalSize;
            private long compressedSize;
            private long bytesSaved;
            private boolean success;
            private boolean skipped;
            private long elapsedMillis;

            public void setFilePath(Path filePath) { this.filePath = filePath; }
            public void setFileName(String fileName) { this.fileName = fileName; }
            public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
            public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }
            public void setBytesSaved(long bytesSaved) { this.bytesSaved = bytesSaved; }
            public void setSkipped(boolean skipped) { this.skipped = skipped; }
            public void setElapsedMillis(long elapsedMillis) { this.elapsedMillis = elapsedMillis; }

            public Path getFilePath() { return filePath; }
            public String getFileName() { return fileName; }
            public long getOriginalSize() { return originalSize; }
            public long getCompressedSize() { return compressedSize; }
            public long getBytesSaved() { return bytesSaved; }
            public boolean isSuccess() { return success && !skipped; }
            public boolean isSkipped() { return skipped; }
            public long getElapsedMillis() { return elapsedMillis; }
        }

        public void setStartTime(long startTime) { this.startTime = startTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        public void setModifiedFiles(int modifiedFiles) { this.modifiedFiles = modifiedFiles; }
        public void setUnchangedFiles(int unchangedFiles) { this.unchangedFiles = unchangedFiles; }

        public void addFileResult(FileResult result) { results.add(result); }
        public void addUnchangedFile(FileFingerprint fp) { unchangedFileList.add(fp); }
        public void addBytesSaved(long bytes) { bytesSaved += bytes; }

        public long getElapsedMillis() { return endTime - startTime; }
        public int getTotalFiles() { return totalFiles; }
        public int getModifiedFiles() { return modifiedFiles; }
        public int getUnchangedFiles() { return unchangedFiles; }
        public long getBytesSaved() { return bytesSaved; }
        public List<FileResult> getResults() { return results; }

        public String getSummary() {
            return String.format("%d个文件共节省 %s, 耗时 %dms",
                totalFiles, formatSize(bytesSaved), getElapsedMillis());
        }

        private static String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
    }

    public static class DecompressionResult {
        private long startTime;
        private long endTime;
        private int successCount;
        private long bytesRestored;

        public void setStartTime(long startTime) { this.startTime = startTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public void addBytesRestored(long bytes) { this.bytesRestored += bytes; }

        public long getElapsedMillis() { return endTime - startTime; }
        public int getSuccessCount() { return successCount; }
        public long getBytesRestored() { return bytesRestored; }
    }
}
