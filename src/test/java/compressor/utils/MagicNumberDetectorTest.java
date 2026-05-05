package compressor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * MagicNumberDetector 测试类
 */
public class MagicNumberDetectorTest {

    public static void main(String[] args) throws IOException {
        System.out.println("=== MagicNumberDetector 测试 ===\n");

        boolean allPassed = true;

        // 测试1: JPEG 魔数检测
        allPassed &= testJpeg();

        // 测试2: PNG 魔数检测
        allPassed &= testPng();

        // 测试3: GIF 魔数检测
        allPassed &= testGif();

        // 测试4: WebP 魔数检测
        allPassed &= testWebp();

        // 测试5: 扩展名可疑性判断
        allPassed &= testSuspiciousExtension();

        System.out.println("\n" + (allPassed ? "✓ 所有测试通过!" : "✗ 部分测试失败"));
    }

    private static boolean testJpeg() {
        System.out.println("--- 测试 JPEG 魔数 ---");
        // JPEG: FF D8 FF ...
        byte[] jpegData = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        MagicNumberDetector.ImageType type = MagicNumberDetector.detectImageType(jpegData);
        boolean passed = type == MagicNumberDetector.ImageType.JPEG;
        System.out.println("JPEG 检测: " + type + (passed ? " ✓" : " ✗"));
        return passed;
    }

    private static boolean testPng() {
        System.out.println("--- 测试 PNG 魔数 ---");
        // PNG: 89 50 4E 47 0D 0A 1A 0A ...
        byte[] pngData = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        };

        MagicNumberDetector.ImageType type = MagicNumberDetector.detectImageType(pngData);
        boolean passed = type == MagicNumberDetector.ImageType.PNG;
        System.out.println("PNG 检测: " + type + (passed ? " ✓" : " ✗"));
        return passed;
    }

    private static boolean testGif() {
        System.out.println("--- 测试 GIF 魔数 ---");
        // GIF89a: 47 49 46 38 39 61 ...
        byte[] gifData = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
            0x01, 0x00, 0x00, 0x00
        };

        MagicNumberDetector.ImageType type = MagicNumberDetector.detectImageType(gifData);
        boolean passed = type == MagicNumberDetector.ImageType.GIF;
        System.out.println("GIF 检测: " + type + (passed ? " ✓" : " ✗"));
        return passed;
    }

    private static boolean testWebp() {
        System.out.println("--- 测试 WebP 魔数 ---");
        // WebP: 52 49 46 46 (RIFF) ... 57 45 42 50 (WEBP) at offset 8
        byte[] webpData = new byte[]{
            0x52, 0x49, 0x46, 0x46,  // RIFF
            0x00, 0x00, 0x00, 0x00,  // size (placeholder)
            0x57, 0x45, 0x42, 0x50   // WEBP
        };

        MagicNumberDetector.ImageType type = MagicNumberDetector.detectImageType(webpData);
        boolean passed = type == MagicNumberDetector.ImageType.WEBP;
        System.out.println("WebP 检测: " + type + (passed ? " ✓" : " ✗"));
        return passed;
    }

    private static boolean testSuspiciousExtension() {
        System.out.println("--- 测试可疑扩展名判断 ---");

        boolean allPassed = true;

        // 空扩展名应该可疑
        allPassed &= testExt("", true, "空扩展名");
        // 纯文本扩展名应该可疑
        allPassed &= testExt("txt", true, "txt 扩展名");
        // 奇怪的后缀应该可疑
        allPassed &= testExt("xyz123", true, "奇怪后缀");
        // 常见图片扩展名不应该可疑
        allPassed &= testExt("jpg", false, "jpg 扩展名");
        allPassed &= testExt("jpeg", false, "jpeg 扩展名");
        allPassed &= testExt("png", false, "png 扩展名");
        allPassed &= testExt("gif", false, "gif 扩展名");
        allPassed &= testExt("webp", false, "webp 扩展名");

        return allPassed;
    }

    private static boolean testExt(String ext, boolean expected, String desc) {
        boolean isSuspicious = MagicNumberDetector.isSuspiciousExtension(ext);
        boolean passed = isSuspicious == expected;
        System.out.println("扩展名 '" + ext + "' (" + desc + "): " +
            (isSuspicious ? "可疑" : "正常") + (passed ? " ✓" : " ✗"));
        return passed;
    }
}
