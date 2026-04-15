package compressor.model;

import compressor.utils.HashUtils;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompressionManifest {
    private static final String MANIFEST_FILE_NAME = ".webcompressor.manifest";
    private static final String VERSION = "1.0";

    private final Map<String, FileFingerprint> fingerprints;
    private final Path manifestPath;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String algorithmUsed;

    public CompressionManifest(Path outputDir) {
        this.manifestPath = outputDir.resolve(MANIFEST_FILE_NAME);
        this.fingerprints = new LinkedHashMap<>();
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.algorithmUsed = "Unknown";
    }

    public static CompressionManifest load(Path manifestPath) throws IOException {
        CompressionManifest manifest = new CompressionManifest(manifestPath.getParent());

        if (!Files.exists(manifestPath)) {
            return manifest;
        }

        try (BufferedReader reader = Files.newBufferedReader(manifestPath)) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("#WebCompressor Manifest v")) {
                throw new IOException("无效的清单文件格式");
            }

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                if (line.startsWith("VERSION:")) {
                    manifest.algorithmUsed = line.substring(8).trim();
                } else if (line.startsWith("CREATED:")) {
                    manifest.createdAt = LocalDateTime.parse(line.substring(8).trim(),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else if (line.startsWith("UPDATED:")) {
                    manifest.lastUpdated = LocalDateTime.parse(line.substring(8).trim(),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else if (line.startsWith("ALGORITHM:")) {
                    manifest.algorithmUsed = line.substring(10).trim();
                } else if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        FileFingerprint fp = new FileFingerprint(
                            Paths.get(parts[0]),
                            parts[1],
                            Long.parseLong(parts[2]),
                            LocalDateTime.now()
                        );
                        fp.setCompressed(Boolean.parseBoolean(parts[3]));
                        if (parts.length > 4) fp.setCompressedPath(parts[4]);
                        if (parts.length > 5) fp.setCompressedSize(Long.parseLong(parts[5]));
                        if (parts.length > 6) fp.setAlgorithm(parts[6]);
                        manifest.fingerprints.put(parts[0], fp);
                    }
                }
            }
        }

        return manifest;
    }

    public void save() throws IOException {
        lastUpdated = LocalDateTime.now();

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(manifestPath))) {
            writer.println("#WebCompressor Manifest v" + VERSION);
            writer.println("#不要手动编辑此文件");
            writer.println("VERSION:" + VERSION);
            writer.println("CREATED:" + createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("UPDATED:" + lastUpdated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("ALGORITHM:" + algorithmUsed);
            writer.println("#格式: 绝对路径|MD5|原始大小|已压缩|压缩路径|压缩大小|算法");
            writer.println();

            for (FileFingerprint fp : fingerprints.values()) {
                writer.printf("%s|%s|%d|%b|%s|%d|%s%n",
                    fp.getAbsolutePath(),
                    fp.getMd5Hash(),
                    fp.getFileSize(),
                    fp.isCompressed(),
                    fp.getCompressedPath() != null ? fp.getCompressedPath() : "",
                    fp.getCompressedSize(),
                    fp.getAlgorithm() != null ? fp.getAlgorithm() : ""
                );
            }
        }
    }

    public void addFingerprint(FileFingerprint fingerprint) {
        fingerprints.put(fingerprint.getAbsolutePath(), fingerprint);
    }

    public FileFingerprint getFingerprint(String absolutePath) {
        return fingerprints.get(absolutePath);
    }

    public FileFingerprint getFingerprint(Path path) {
        return fingerprints.get(path.toAbsolutePath().toString());
    }

    public boolean hasChanged(Path path, String currentMD5) {
        FileFingerprint existing = getFingerprint(path);
        if (existing == null) {
            return true;
        }
        return !existing.getMd5Hash().equals(currentMD5);
    }

    public List<FileFingerprint> getUnchangedFiles(List<Path> paths) throws IOException {
        List<FileFingerprint> unchanged = new ArrayList<>();

        for (Path path : paths) {
            String currentMD5 = HashUtils.md5(path);
            if (!hasChanged(path, currentMD5)) {
                FileFingerprint fp = getFingerprint(path);
                if (fp != null && fp.isCompressed()) {
                    unchanged.add(fp);
                }
            }
        }

        return unchanged;
    }

    public List<Path> getModifiedOrNewFiles(List<Path> allPaths) throws IOException {
        List<Path> modified = new ArrayList<>();

        for (Path path : allPaths) {
            if (hasChanged(path, HashUtils.md5(path))) {
                modified.add(path);
            }
        }

        return modified;
    }

    public void setAlgorithmUsed(String algorithm) {
        this.algorithmUsed = algorithm;
    }

    public String getAlgorithmUsed() {
        return algorithmUsed;
    }

    public Path getManifestPath() {
        return manifestPath;
    }

    public Collection<FileFingerprint> getAllFingerprints() {
        return fingerprints.values();
    }

    public int size() {
        return fingerprints.size();
    }

    public int getCompressedCount() {
        int count = 0;
        for (FileFingerprint fp : fingerprints.values()) {
            if (fp.isCompressed()) count++;
        }
        return count;
    }

    public long getTotalOriginalSize() {
        long total = 0;
        for (FileFingerprint fp : fingerprints.values()) {
            total += fp.getFileSize();
        }
        return total;
    }

    public long getTotalCompressedSize() {
        long total = 0;
        for (FileFingerprint fp : fingerprints.values()) {
            if (fp.isCompressed()) {
                total += fp.getCompressedSize();
            }
        }
        return total;
    }

    public CompressionStats getOverallStats() {
        CompressionStats stats = new CompressionStats();
        stats.setOriginalSize(getTotalOriginalSize());
        stats.setCompressedSize(getTotalCompressedSize());
        stats.setAlgorithmName(algorithmUsed);
        stats.calculate();
        return stats;
    }

    @Override
    public String toString() {
        return String.format(
            "CompressionManifest{files=%d, compressed=%d, totalSize=%s, compressedSize=%s, ratio=%.2f%%}",
            size(), getCompressedCount(),
            formatSize(getTotalOriginalSize()),
            formatSize(getTotalCompressedSize()),
            (1.0 - (double) getTotalCompressedSize() / getTotalOriginalSize()) * 100
        );
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
