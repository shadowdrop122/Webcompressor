package compressor.utils;

import java.util.*;

public class TransferSimulator {

    public enum BandwidthProfile {
        FIVE_G("5G移动网络", 500, 100, 10),
        FOUR_G("4G移动网络", 50, 10, 50),
        THREE_G("3G移动网络", 5, 1, 200),
        WIFI_5GHZ("WiFi 5GHz", 300, 150, 5),
        WIFI_2_4GHZ("WiFi 2.4GHz", 100, 50, 10),
        ETHERNET("千兆以太网", 1000, 1000, 1),
        FIBER("光纤宽带", 500, 500, 1),
        DSL("ADSL宽带", 20, 5, 100),
        SATELLITE("卫星网络", 20, 2, 600);

        private final String displayName;
        private final long downloadMbps;
        private final long uploadMbps;
        private final int latencyMs;

        BandwidthProfile(String displayName, long downloadMbps, long uploadMbps, int latencyMs) {
            this.displayName = displayName;
            this.downloadMbps = downloadMbps;
            this.uploadMbps = uploadMbps;
            this.latencyMs = latencyMs;
        }

        public String getDisplayName() { return displayName; }
        public long getDownloadMbps() { return downloadMbps; }
        public long getUploadMbps() { return uploadMbps; }
        public int getLatencyMs() { return latencyMs; }

        public long getDownloadBps() { return downloadMbps * 1_000_000 / 8; }
        public long getUploadBps() { return uploadMbps * 1_000_000 / 8; }
    }

    private final BandwidthProfile profile;
    private final Map<String, TransferRecord> records;

    public TransferSimulator() {
        this(BandwidthProfile.FOUR_G);
    }

    public TransferSimulator(BandwidthProfile profile) {
        this.profile = profile;
        this.records = new HashMap<>();
    }

    public TransferEstimate estimateTransfer(long originalBytes, long compressedBytes) {
        long originalTime = calculateTransferTime(originalBytes, true);
        long compressedTime = calculateTransferTime(compressedBytes, true);
        long timeSaved = originalTime - compressedTime;

        return new TransferEstimate(
            originalBytes,
            compressedBytes,
            originalTime,
            compressedTime,
            timeSaved,
            profile
        );
    }

    public TransferEstimate estimateDownload(long originalBytes, long compressedBytes) {
        return estimateTransfer(originalBytes, compressedBytes);
    }

    public TransferEstimate estimateUpload(long originalBytes, long compressedBytes) {
        long originalTime = calculateTransferTime(originalBytes, false);
        long compressedTime = calculateTransferTime(compressedBytes, false);
        long timeSaved = originalTime - compressedTime;

        return new TransferEstimate(
            originalBytes,
            compressedBytes,
            originalTime,
            compressedTime,
            timeSaved,
            profile
        );
    }

    private long calculateTransferTime(long bytes, boolean isDownload) {
        if (bytes <= 0) return 0;

        long bps = isDownload ? profile.getDownloadBps() : profile.getUploadBps();
        long baseTimeMs = (bytes * 1000L) / bps;
        long latencyMs = profile.getLatencyMs();

        if (bytes > 1024 * 1024) {
            latencyMs = (int) (latencyMs * 3);
        }

        return baseTimeMs + latencyMs;
    }

    public TransferEstimate compareWithBatch(List<Long> originalSizes, List<Long> compressedSizes) {
        if (originalSizes.size() != compressedSizes.size()) {
            throw new IllegalArgumentException("原始大小和压缩大小列表长度必须一致");
        }

        long totalOriginal = 0;
        long totalCompressed = 0;

        for (int i = 0; i < originalSizes.size(); i++) {
            totalOriginal += originalSizes.get(i);
            totalCompressed += compressedSizes.get(i);
        }

        return estimateTransfer(totalOriginal, totalCompressed);
    }

    public String generateReport(long originalBytes, long compressedBytes) {
        TransferEstimate estimate = estimateTransfer(originalBytes, compressedBytes);
        return generateReport(estimate);
    }

    public String generateReport(TransferEstimate estimate) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(60)).append("\n");
        sb.append("传输模拟报告\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append("网络环境: ").append(profile.getDisplayName()).append("\n");
        sb.append("- 下载速度: ").append(profile.getDownloadMbps()).append(" Mbps\n");
        sb.append("- 上传速度: ").append(profile.getUploadMbps()).append(" Mbps\n");
        sb.append("- 网络延迟: ").append(profile.getLatencyMs()).append(" ms\n");

        sb.append("\n").append("-".repeat(60)).append("\n\n");

        sb.append("文件传输分析:\n");
        sb.append("  原始大小:     ").append(formatSize(estimate.getOriginalBytes())).append("\n");
        sb.append("  压缩后大小:   ").append(formatSize(estimate.getCompressedBytes())).append("\n");
        sb.append("  节省空间:     ").append(formatSize(estimate.getOriginalBytes() - estimate.getCompressedBytes()));
        sb.append(" (").append(String.format("%.1f%%", estimate.getSavingsPercent())).append(")\n");

        sb.append("\n").append("-".repeat(60)).append("\n\n");

        sb.append("传输时间:\n");
        sb.append("  原始传输:     ").append(formatDuration(estimate.getOriginalTimeMs())).append("\n");
        sb.append("  压缩后传输:   ").append(formatDuration(estimate.getCompressedTimeMs())).append("\n");
        sb.append("  时间节省:     ").append(formatDuration(estimate.getTimeSavedMs())).append("\n");

        if (estimate.getTimeSavedMs() > 0) {
            sb.append("\n").append("=".repeat(60)).append("\n");
            sb.append("结论: 压缩后传输可节省 ").append(formatDuration(estimate.getTimeSavedMs())).append("\n");

            if (estimate.getTimeSavedMs() > 3600000) {
                sb.append("      对于大批量文件，这将显著减少带宽成本!\n");
            } else if (estimate.getTimeSavedMs() > 60000) {
                sb.append("      用户体验提升明显，等待时间大幅减少。\n");
            } else if (estimate.getTimeSavedMs() > 1000) {
                sb.append("      感知上的响应速度有明显改善。\n");
            }
        }

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        if (ms < 60000) return String.format("%.2f s", ms / 1000.0);
        if (ms < 3600000) {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%d min %d s", minutes, seconds);
        }
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        return String.format("%d h %d min", hours, minutes);
    }

    public BandwidthProfile getProfile() {
        return profile;
    }

    public static BandwidthProfile[] getAllProfiles() {
        return BandwidthProfile.values();
    }

    public static class TransferEstimate {
        private final long originalBytes;
        private final long compressedBytes;
        private final long originalTimeMs;
        private final long compressedTimeMs;
        private final long timeSavedMs;
        private final BandwidthProfile profile;

        public TransferEstimate(long originalBytes, long compressedBytes,
                               long originalTimeMs, long compressedTimeMs,
                               long timeSavedMs, BandwidthProfile profile) {
            this.originalBytes = originalBytes;
            this.compressedBytes = compressedBytes;
            this.originalTimeMs = originalTimeMs;
            this.compressedTimeMs = compressedTimeMs;
            this.timeSavedMs = timeSavedMs;
            this.profile = profile;
        }

        public long getOriginalBytes() { return originalBytes; }
        public long getCompressedBytes() { return compressedBytes; }
        public long getOriginalTimeMs() { return originalTimeMs; }
        public long getCompressedTimeMs() { return compressedTimeMs; }
        public long getTimeSavedMs() { return timeSavedMs; }
        public BandwidthProfile getProfile() { return profile; }

        public double getSavingsPercent() {
            if (originalBytes <= 0) return 0;
            return (1.0 - (double) compressedBytes / originalBytes) * 100;
        }

        @Override
        public String toString() {
            return String.format(
                "TransferEstimate{原始=%d bytes (%s), 压缩后=%d bytes (%s), 节省=%s}",
                originalBytes, formatSize(originalBytes),
                compressedBytes, formatSize(compressedBytes),
                formatDuration(timeSavedMs)
            );
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }

        private String formatDuration(long ms) {
            if (ms < 1000) return ms + " ms";
            if (ms < 60000) return String.format("%.2f s", ms / 1000.0);
            return String.format("%.1f min", ms / 60000.0);
        }
    }

    private static class TransferRecord {
        private final long originalBytes;
        private final long compressedBytes;
        private final long timeMs;
        private final BandwidthProfile profile;

        public TransferRecord(long originalBytes, long compressedBytes, long timeMs, BandwidthProfile profile) {
            this.originalBytes = originalBytes;
            this.compressedBytes = compressedBytes;
            this.timeMs = timeMs;
            this.profile = profile;
        }
    }
}
