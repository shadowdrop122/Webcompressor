package compressor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalCompressorTest {

    private Path tempDir;
    private Path testFilesDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("webcompressor_test");
        testFilesDir = tempDir.resolve("testfiles");
        Files.createDirectories(testFilesDir);
    }

    @Test
    void testFileFingerprintCreation() {
        Path testFile = testFilesDir.resolve("test.txt");
        String md5 = "d41d8cd98f00b204e9800998ecf8427e";
        long size = 100;

        FileFingerprint fp = new FileFingerprint(testFile, md5, size, java.time.LocalDateTime.now());

        assertEquals(testFile.toAbsolutePath().toString(), fp.getAbsolutePath());
        assertEquals("test.txt", fp.getFileName());
        assertEquals(md5, fp.getMd5Hash());
        assertEquals(size, fp.getFileSize());
        assertFalse(fp.isCompressed());
    }

    @Test
    void testFileFingerprintContentEquals() {
        Path file1 = testFilesDir.resolve("file1.txt");
        Path file2 = testFilesDir.resolve("file2.txt");
        String sameMD5 = "abc123";
        String differentMD5 = "def456";

        FileFingerprint fp1 = new FileFingerprint(file1, sameMD5, 100, java.time.LocalDateTime.now());
        FileFingerprint fp2 = new FileFingerprint(file2, sameMD5, 200, java.time.LocalDateTime.now());
        FileFingerprint fp3 = new FileFingerprint(file1, differentMD5, 100, java.time.LocalDateTime.now());

        assertTrue(fp1.contentEquals(fp2));
        assertFalse(fp1.contentEquals(fp3));
    }

    @Test
    void testFileFingerprintCompressionStats() {
        Path testFile = testFilesDir.resolve("test.txt");
        FileFingerprint fp = new FileFingerprint(testFile, "md5", 1000, java.time.LocalDateTime.now());
        fp.setCompressed(true);
        fp.setCompressedSize(400);
        fp.setAlgorithm("Huffman");

        assertEquals(0.4, fp.getCompressionRatio(), 0.01);
        assertEquals(60.0, fp.getSavingsPercent(), 0.1);
        assertEquals(600, fp.getBytesSaved());
    }

    @Test
    void testCompressionManifestSaveAndLoad() throws IOException {
        CompressionManifest manifest = new CompressionManifest(tempDir);

        Path file1 = testFilesDir.resolve("file1.txt");
        Path file2 = testFilesDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        FileFingerprint fp1 = new FileFingerprint(file1, "md5-1", 100, java.time.LocalDateTime.now());
        FileFingerprint fp2 = new FileFingerprint(file2, "md5-2", 200, java.time.LocalDateTime.now());

        manifest.addFingerprint(fp1);
        manifest.addFingerprint(fp2);
        manifest.setAlgorithmUsed("Huffman");
        manifest.save();

        CompressionManifest loaded = CompressionManifest.load(tempDir.resolve(".webcompressor.manifest"));

        assertEquals(2, loaded.size());
        assertEquals("Huffman", loaded.getAlgorithmUsed());
        assertNotNull(loaded.getFingerprint(file1));
        assertNotNull(loaded.getFingerprint(file2));
    }

    @Test
    void testCompressionManifestHasChanged() throws IOException {
        CompressionManifest manifest = new CompressionManifest(tempDir);

        Path testFile = testFilesDir.resolve("changing.txt");
        Files.writeString(testFile, "original content");
        String originalMD5 = compressor.utils.HashUtils.md5(testFile);

        FileFingerprint fp = new FileFingerprint(testFile, originalMD5, 100, java.time.LocalDateTime.now());
        manifest.addFingerprint(fp);

        assertFalse(manifest.hasChanged(testFile, originalMD5));
        assertTrue(manifest.hasChanged(testFile, "different-md5"));
    }

    @Test
    void testManifestOverallStats() throws IOException {
        CompressionManifest manifest = new CompressionManifest(tempDir);

        Path file1 = testFilesDir.resolve("file1.txt");
        Path file2 = testFilesDir.resolve("file2.txt");

        FileFingerprint fp1 = new FileFingerprint(file1, "md5-1", 1000, java.time.LocalDateTime.now());
        fp1.setCompressed(true);
        fp1.setCompressedSize(400);

        FileFingerprint fp2 = new FileFingerprint(file2, "md5-2", 2000, java.time.LocalDateTime.now());
        fp2.setCompressed(true);
        fp2.setCompressedSize(800);

        manifest.addFingerprint(fp1);
        manifest.addFingerprint(fp2);

        assertEquals(3000, manifest.getTotalOriginalSize());
        assertEquals(1200, manifest.getTotalCompressedSize());
        assertEquals(2, manifest.getCompressedCount());
    }

    @Test
    void testTransferSimulatorEstimate() {
        compressor.utils.TransferSimulator simulator = new compressor.utils.TransferSimulator(
            compressor.utils.TransferSimulator.BandwidthProfile.FOUR_G
        );

        long originalSize = 10 * 1024 * 1024; // 10 MB
        long compressedSize = 3 * 1024 * 1024; // 3 MB

        compressor.utils.TransferSimulator.TransferEstimate estimate = simulator.estimateTransfer(originalSize, compressedSize);

        assertTrue(estimate.getOriginalTimeMs() > estimate.getCompressedTimeMs());
        assertTrue(estimate.getTimeSavedMs() > 0);
        assertEquals(70.0, estimate.getSavingsPercent(), 0.1);
    }

    @Test
    void testTransferSimulatorGenerateReport() {
        compressor.utils.TransferSimulator simulator = new compressor.utils.TransferSimulator(
            compressor.utils.TransferSimulator.BandwidthProfile.WIFI_5GHZ
        );

        String report = simulator.generateReport(10 * 1024 * 1024, 5 * 1024 * 1024);

        assertTrue(report.contains("WiFi 5GHz"));
        assertTrue(report.contains("10.00 MB"));
        assertTrue(report.contains("5.00 MB"));
        assertTrue(report.contains("节省"));
    }

    @Test
    void testTransferSimulatorAllProfiles() {
        compressor.utils.TransferSimulator.BandwidthProfile[] profiles =
            compressor.utils.TransferSimulator.BandwidthProfile.values();

        assertEquals(9, profiles.length);

        for (compressor.utils.TransferSimulator.BandwidthProfile profile : profiles) {
            compressor.utils.TransferSimulator simulator = new compressor.utils.TransferSimulator(profile);
            compressor.utils.TransferSimulator.TransferEstimate estimate =
                simulator.estimateTransfer(1024 * 1024, 512 * 1024);

            assertNotNull(estimate);
            assertTrue(estimate.getTimeSavedMs() >= 0);
        }
    }

    @Test
    void testTransferEstimateSavingsPercent() {
        compressor.utils.TransferSimulator simulator = new compressor.utils.TransferSimulator();

        compressor.utils.TransferSimulator.TransferEstimate estimate50 =
            simulator.estimateTransfer(1000, 500);
        assertEquals(50.0, estimate50.getSavingsPercent(), 0.1);

        compressor.utils.TransferSimulator.TransferEstimate estimate30 =
            simulator.estimateTransfer(1000, 700);
        assertEquals(30.0, estimate30.getSavingsPercent(), 0.1);
    }
}
