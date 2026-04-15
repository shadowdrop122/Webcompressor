package compressor.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

public class FileFingerprint implements Comparable<FileFingerprint> {
    private final String absolutePath;
    private final String fileName;
    private String md5Hash;
    private final long fileSize;
    private final LocalDateTime lastModified;
    private boolean compressed;
    private String compressedPath;
    private long compressedSize;
    private String algorithm;

    public FileFingerprint(Path path, String md5Hash, long fileSize, LocalDateTime lastModified) {
        this.absolutePath = path.toAbsolutePath().toString();
        this.fileName = path.getFileName().toString();
        this.md5Hash = md5Hash;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.compressed = false;
        this.compressedPath = null;
        this.compressedSize = 0;
        this.algorithm = null;
    }

    public static FileFingerprint create(Path path, String md5Hash, long fileSize) {
        LocalDateTime now = LocalDateTime.now();
        return new FileFingerprint(path, md5Hash, fileSize, now);
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public Path getPath() {
        return Paths.get(absolutePath);
    }

    public String getFileName() {
        return fileName;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public String getCompressedPath() {
        return compressedPath;
    }

    public void setCompressedPath(String compressedPath) {
        this.compressedPath = compressedPath;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public double getCompressionRatio() {
        if (fileSize <= 0) return 0;
        return (double) compressedSize / fileSize;
    }

    public double getSavingsPercent() {
        if (fileSize <= 0) return 0;
        return (1.0 - getCompressionRatio()) * 100;
    }

    public long getBytesSaved() {
        return fileSize - compressedSize;
    }

    public boolean contentEquals(FileFingerprint other) {
        return this.md5Hash.equals(other.md5Hash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileFingerprint that = (FileFingerprint) o;
        return Objects.equals(absolutePath, that.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absolutePath);
    }

    @Override
    public int compareTo(FileFingerprint other) {
        return this.absolutePath.compareTo(other.absolutePath);
    }

    @Override
    public String toString() {
        return String.format(
            "FileFingerprint{path='%s', name='%s', md5='%s', size=%d, compressed=%s, ratio=%.2f%%}",
            absolutePath, fileName, md5Hash, fileSize, compressed, getSavingsPercent()
        );
    }

    public String toDetailedString() {
        return String.format(
            "文件: %s\n" +
            "  路径: %s\n" +
            "  MD5: %s\n" +
            "  大小: %s\n" +
            "  压缩: %s\n" +
            "  算法: %s\n" +
            "  压缩后: %s\n" +
            "  节省: %d bytes (%.2f%%)",
            fileName, absolutePath, md5Hash,
            formatSize(fileSize), compressed ? "是" : "否",
            algorithm != null ? algorithm : "N/A",
            compressed ? formatSize(compressedSize) : "N/A",
            getBytesSaved(), getSavingsPercent()
        );
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
