package compressor.algorithms;

import compressor.core.ICompressor;
import compressor.model.CompressionStats;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片压缩算法测试类
 * 提供 main 方法用于手动测试 LZWImageCompressor 和 PoolingImageCompressor
 */
public class ImageCompressorTest {

    public static void main(String[] args) {
        System.out.println("=== 图片压缩算法测试 ===\n");

        // 测试1: 创建一个测试图片（如果用户没有提供）
        File testImage = createTestImage("test_input.jpg", 200, 200);

        // 测试2: LZWImageCompressor 无损压缩
        testLZWImageCompressor(testImage);

        // 测试3: PoolingImageCompressor 有损压缩
        testPoolingImageCompressor(testImage);

        // 清理测试文件
        cleanupTestFiles();
    }

    /**
     * 测试 LZWImageCompressor
     */
    private static void testLZWImageCompressor(File imageFile) {
        System.out.println("--- 测试 LZWImageCompressor (无损压缩) ---");

        try {
            // 读取原始图片
            byte[] originalBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            System.out.println("原始图片大小: " + originalBytes.length + " bytes");

            // 压缩
            ICompressor compressor = new LZWImageCompressor();
            long start = System.currentTimeMillis();
            byte[] compressed = compressor.compress(originalBytes);
            long compressTime = System.currentTimeMillis() - start;

            System.out.println("压缩后大小: " + compressed.length + " bytes");
            System.out.println("压缩耗时: " + compressTime + "ms");
            System.out.println("压缩率: " + String.format("%.2f%%", (1.0 - (double) compressed.length / originalBytes.length) * 100));

            CompressionStats stats = compressor.getStats();
            System.out.println("统计信息: " + stats);

            // 解压
            start = System.currentTimeMillis();
            byte[] decompressed = compressor.decompress(compressed);
            long decompressTime = System.currentTimeMillis() - start;

            System.out.println("解压后大小: " + decompressed.length + " bytes");
            System.out.println("解压耗时: " + decompressTime + "ms");

            // 保存解压后的图片
            File outputFile = new File("test_output_lzw.png");
            java.nio.file.Files.write(outputFile.toPath(), decompressed);
            System.out.println("解压图片已保存: " + outputFile.getAbsolutePath());

            // 验证图片有效性
            BufferedImage decompressedImg = ImageIO.read(outputFile);
            if (decompressedImg != null) {
                System.out.println("✓ LZW解压图片验证通过 - 尺寸: " + decompressedImg.getWidth() + "x" + decompressedImg.getHeight());
            } else {
                System.out.println("✗ LZW解压图片验证失败");
            }

            System.out.println();

        } catch (IOException e) {
            System.err.println("LZW测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试 PoolingImageCompressor
     */
    private static void testPoolingImageCompressor(File imageFile) {
        System.out.println("--- 测试 PoolingImageCompressor (有损压缩 + 池化降质) ---");

        try {
            // 读取原始图片
            byte[] originalBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            System.out.println("原始图片大小: " + originalBytes.length + " bytes");

            BufferedImage originalImg = ImageIO.read(imageFile);
            System.out.println("原始图片尺寸: " + originalImg.getWidth() + "x" + originalImg.getHeight());

            // 测试不同质量等级
            for (int quality = 2; quality <= 8; quality += 3) {
                System.out.println("\n>>> 质量等级 quality=" + quality + " (池化窗口: " + quality + "x" + quality + ")");

                // 压缩
                ICompressor compressor = new PoolingImageCompressor(quality);
                long start = System.currentTimeMillis();
                byte[] compressed = compressor.compress(originalBytes);
                long compressTime = System.currentTimeMillis() - start;

                System.out.println("压缩后大小: " + compressed.length + " bytes");
                System.out.println("压缩耗时: " + compressTime + "ms");
                System.out.println("压缩率: " + String.format("%.2f%%",
                    (1.0 - (double) compressed.length / originalBytes.length) * 100));

                // 解压
                start = System.currentTimeMillis();
                byte[] decompressed = compressor.decompress(compressed);
                long decompressTime = System.currentTimeMillis() - start;

                System.out.println("解压耗时: " + decompressTime + "ms");

                // 保存解压后的图片
                String filename = "test_output_pool_q" + quality + ".png";
                File outputFile = new File(filename);
                java.nio.file.Files.write(outputFile.toPath(), decompressed);

                // 验证图片
                BufferedImage decompressedImg = ImageIO.read(outputFile);
                if (decompressedImg != null) {
                    System.out.println("✓ 解压图片验证通过 - 尺寸: " + decompressedImg.getWidth() + "x" + decompressedImg.getHeight());
                    System.out.println("  保存为: " + outputFile.getAbsolutePath());
                } else {
                    System.out.println("✗ 解压图片验证失败");
                }
            }

            System.out.println();

        } catch (IOException e) {
            System.err.println("Pooling测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建一个简单的测试图片（200x200 渐变色彩）
     */
    private static File createTestImage(String filename, int width, int height) {
        try {
            File file = new File(filename);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // 创建渐变背景
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = (x * 255) / width;
                    int gVal = (y * 255) / height;
                    int b = ((x + y) * 255) / (width + height);
                    int rgb = (r << 16) | (gVal << 8) | b;
                    image.setRGB(x, y, rgb);
                }
            }

            // 添加一些几何图形增加复杂度
            g.setColor(Color.WHITE);
            g.fillOval(50, 50, 100, 100);
            g.setColor(Color.RED);
            g.fillRect(120, 120, 50, 50);

            g.dispose();

            ImageIO.write(image, "jpg", file);
            System.out.println("测试图片已创建: " + file.getAbsolutePath() + " (" + width + "x" + height + ")");

            return file;
        } catch (IOException e) {
            System.err.println("创建测试图片失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清理测试生成的文件
     */
    private static void cleanupTestFiles() {
        String[] filesToDelete = {
            "test_input.jpg",
            "test_output_lzw.png",
            "test_output_pool_q2.png",
            "test_output_pool_q5.png",
            "test_output_pool_q8.png"
        };

        System.out.println("--- 清理测试文件 ---");
        for (String filename : filesToDelete) {
            File f = new File(filename);
            if (f.exists()) {
                if (f.delete()) {
                    System.out.println("已删除: " + filename);
                } else {
                    System.out.println("删除失败: " + filename);
                }
            }
        }
    }
}
