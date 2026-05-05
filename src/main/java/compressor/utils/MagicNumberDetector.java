package compressor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件头魔数探测工具
 * 用于识别没有扩展名或扩展名不正确的图片文件
 *
 * 支持的图片格式魔数：
 * - JPEG: FF D8 FF
 * - PNG: 89 50 4E 47 0D 0A 1A 0A
 * - GIF: 47 49 46 38 (GIF8)
 * - WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
 */
public class MagicNumberDetector {

    /**
     * 图片类型枚举
     */
    public enum ImageType {
        JPEG("JPEG", "jpg"),
        PNG("PNG", "png"),
        GIF("GIF", "gif"),
        WEBP("WebP", "webp"),
        UNKNOWN(null, null);

        private final String displayName;
        private final String extension;

        ImageType(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getExtension() {
            return extension;
        }
    }

    // 魔数常量（使用字节数组避免字符串编码问题）
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC_87A = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF_MAGIC_89A = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a
    private static final byte[] RIFF_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF
    private static final byte[] WEBP_SIGNATURE = new byte[]{0x57, 0x45, 0x42, 0x50}; // WEBP

    // 需要读取的字节数（WebP需要检查偏移8字节处）
    private static final int MIN_BYTES_TO_READ = 12;

    private MagicNumberDetector() {
    }

    /**
     * 检测文件类型（通过魔数）
     *
     * @param filePath 文件路径
     * @return 检测到的图片类型，如果无法识别返回 UNKNOWN
     */
    public static ImageType detectImageType(Path filePath) throws IOException {
        try (InputStream in = Files.newInputStream(filePath)) {
            return detectImageType(in);
        }
    }

    /**
     * 检测字节数组类型（通过魔数）
     *
     * @param data 文件字节数据（至少需要前12个字节）
     * @return 检测到的图片类型，如果无法识别返回 UNKNOWN
     */
    public static ImageType detectImageType(byte[] data) {
        if (data == null || data.length < 4) {
            return ImageType.UNKNOWN;
        }
        return detectFromBytes(data, 0, data.length);
    }

    /**
     * 从输入流检测图片类型
     *
     * @param in 输入流（需要至少可读取12字节）
     * @return 检测到的图片类型，如果无法识别返回 UNKNOWN
     */
    public static ImageType detectImageType(InputStream in) throws IOException {
        byte[] header = new byte[MIN_BYTES_TO_READ];
        int bytesRead = readFully(in, header);
        return detectFromBytes(header, 0, bytesRead);
    }

    /**
     * 从字节数组检测图片类型
     *
     * @param bytes  字节数组
     * @param offset 起始偏移
     * @param length 有效数据长度
     * @return 检测到的图片类型，如果无法识别返回 UNKNOWN
     */
    private static ImageType detectFromBytes(byte[] bytes, int offset, int length) {
        if (length < 4) {
            return ImageType.UNKNOWN;
        }

        // 检查JPEG: FF D8 FF
        if (matchesMagic(bytes, offset, JPEG_MAGIC)) {
            return ImageType.JPEG;
        }

        // 检查PNG: 89 50 4E 47 0D 0A 1A 0A
        if (length >= 8 && matchesMagic(bytes, offset, PNG_MAGIC)) {
            return ImageType.PNG;
        }

        // 检查GIF: 47 49 46 38 37|39 61 (GIF87a or GIF89a)
        if (length >= 6 && matchesMagic(bytes, offset, GIF_MAGIC_87A)) {
            return ImageType.GIF;
        }
        if (length >= 6 && matchesMagic(bytes, offset, GIF_MAGIC_89A)) {
            return ImageType.GIF;
        }

        // 检查WebP: 52 49 46 46 (RIFF) + offset 8 = 57 45 42 50 (WEBP)
        if (length >= MIN_BYTES_TO_READ && matchesMagic(bytes, offset, RIFF_MAGIC)) {
            // 检查第8-11字节是否为WEBP签名
            if (matchesMagicAtOffset(bytes, offset + 8, WEBP_SIGNATURE)) {
                return ImageType.WEBP;
            }
        }

        return ImageType.UNKNOWN;
    }

    /**
     * 字节数组匹配魔数
     *
     * @param data   字节数据
     * @param offset 起始偏移
     * @param magic  魔数字节数组
     * @return 是否匹配
     */
    private static boolean matchesMagic(byte[] data, int offset, byte[] magic) {
        if (data.length - offset < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在指定偏移量匹配魔数
     */
    private static boolean matchesMagicAtOffset(byte[] data, int offset, byte[] magic) {
        if (data.length - offset < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从输入流读取指定数量的字节
     *
     * @param in     输入流
     * @param buffer 目标缓冲区
     * @return 实际读取的字节数
     */
    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int totalRead = 0;
        int read;
        while (totalRead < buffer.length && (read = in.read(buffer, totalRead, buffer.length - totalRead)) != -1) {
            totalRead += read;
        }
        return totalRead;
    }

    /**
     * 判断文件扩展名是否"可疑"（空或未知）
     */
    public static boolean isSuspiciousExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return true;
        }
        // 转为小写
        String ext = extension.toLowerCase();
        // 常见图片扩展名不算可疑
        String[] knownImageExts = {"jpg", "jpeg", "png", "gif", "webp", "bmp", "ico", "svg"};
        for (String known : knownImageExts) {
            if (ext.equals(known)) {
                return false;
            }
        }
        // 其他扩展名（包括奇怪的后缀）都视为可疑
        return true;
    }

    /**
     * 获取扩展名（不含点号）
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
