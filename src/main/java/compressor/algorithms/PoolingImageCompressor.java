package compressor.algorithms;

import compressor.core.AbstractCompressor;
import compressor.model.CompressionStats;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 池化图像压缩器 - 核心亮点算法
 * 通过矩阵均值池化降采样，再使用LZW压缩
 *
 * 压缩流程：
 * 1. 解码byte[]为像素矩阵
 * 2. 根据quality参数进行NxN均值池化（降采样）
 * 3. 使用LZWImageCompressor压缩池化后的像素
 *
 * 解压流程：
 * 1. LZW解压缩得到缩小后的像素矩阵
 * 2. 根据原尺寸进行最近邻插值放大（马赛克效果）
 * 3. 重建为BufferedImage并输出为byte[]
 */
public class PoolingImageCompressor extends AbstractCompressor {

    public static final String ALGORITHM_NAME = "PoolingImage";
    public static final String DESCRIPTION = "池化降质压缩 - 基于均值池化的有损图像压缩";
    private static final int DEFAULT_QUALITY = 5; // 默认中等质量

    private final int quality; // 1-10，对应池化窗口大小 1x1 - 10x10
    private final LZWImageCompressor lzwCompressor;

    /**
     * 默认构造函数，quality=5
     */
    public PoolingImageCompressor() {
        this(DEFAULT_QUALITY);
    }

    /**
     * 带质量参数的构造函数
     * @param quality 质量等级 1-10（1=最高质量/最小窗口，10=最低质量/最大窗口）
     */
    public PoolingImageCompressor(int quality) {
        this.quality = Math.max(1, Math.min(10, quality));
        this.lzwCompressor = new LZWImageCompressor();
    }

    /**
     * 带LZW字典大小的构造函数（字典大小固定，参数保留兼容性）
     */
    @SuppressWarnings("unused")
    public PoolingImageCompressor(int quality, int lzwDictSize) {
        this.quality = Math.max(1, Math.min(10, quality));
        this.lzwCompressor = new LZWImageCompressor();
    }

    @Override
    public byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        startTiming();
        long startTime = System.currentTimeMillis();

        // 1. 读取原始图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(data));
        if (originalImage == null) {
            throw new IOException("无法解析图片数据");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int originalType = originalImage.getType();

        // 2. 计算池化窗口大小（quality 1-10 → 窗口 1x1-10x10）
        int poolSize = quality; // 窗口大小等于quality值

        // 3. 执行均值池化（降采样）
        BufferedImage pooledImage = applyMeanPooling(originalImage, poolSize);

        // 4. 将池化后的图像转换为byte[]
        ByteArrayOutputStream pooledOut = new ByteArrayOutputStream();
        ImageIO.write(pooledImage, "png", pooledOut);
        byte[] pooledBytes = pooledOut.toByteArray();

        // 5. 使用LZW压缩池化后的图像数据
        byte[] lzwCompressed = lzwCompressor.compress(pooledBytes);

        // 6. 构建包含元数据的归档格式：
        // [魔数(4)][原宽(4)][原高(4)][池化窗口(1)][池化后宽(4)][池化后高(4)][图像类型(4)][LZW压缩长度(4)][LZW数据]
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();

        // 魔数 "POOL"
        finalOut.write('P');
        finalOut.write('O');
        finalOut.write('O');
        finalOut.write('L');

        // 原始尺寸
        writeInt(finalOut, originalWidth);
        writeInt(finalOut, originalHeight);

        // 池化窗口大小
        finalOut.write(poolSize);

        // 池化后尺寸
        writeInt(finalOut, pooledImage.getWidth());
        writeInt(finalOut, pooledImage.getHeight());

        // 图像类型
        writeInt(finalOut, originalType);

        // LZW压缩数据长度
        writeInt(finalOut, lzwCompressed.length);

        // LZW压缩数据
        finalOut.write(lzwCompressed, 0, lzwCompressed.length);

        byte[] result = finalOut.toByteArray();
        endTiming(data.length, result.length);
        return result;
    }

    @Override
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length < 25) {
            return new byte[0];
        }

        startTiming();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        // 1. 验证魔数
        if (in.read() != 'P' || in.read() != 'O' || in.read() != 'O' || in.read() != 'L') {
            throw new IOException("无效的PoolingImage压缩格式，魔数不匹配");
        }

        // 2. 读取元数据
        int originalWidth = readInt(in);
        int originalHeight = readInt(in);
        int poolSize = in.read() & 0xFF;
        int pooledWidth = readInt(in);
        int pooledHeight = readInt(in);
        int originalType = readInt(in);
        int lzwCompressedLength = readInt(in);

        // 3. 读取LZW压缩数据
        byte[] lzwCompressed = new byte[lzwCompressedLength];
        in.read(lzwCompressed);

        // 4. LZW解压缩，得到池化后的图像数据
        byte[] pooledBytes = lzwCompressor.decompress(lzwCompressed);

        // 5. 从池化后的byte[]重建BufferedImage
        BufferedImage pooledImage = bytesToImage(pooledBytes);
        if (pooledImage == null) {
            throw new IOException("无法重建池化后的图像");
        }

        // 6. 最近邻插值放大到原始尺寸（马赛克效果）
        BufferedImage restoredImage = nearestNeighborUpscale(pooledImage, originalWidth, originalHeight);

        // 7. 转换为byte[]输出
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(restoredImage, "png", out);
        byte[] result = out.toByteArray();

        endTiming(result.length, data.length);
        return result;
    }

    /**
     * 应用均值池化（降采样）
     */
    private BufferedImage applyMeanPooling(BufferedImage src, int poolSize) {
        if (poolSize <= 1) {
            return src; // 不进行池化
        }

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        // 计算目标尺寸
        int dstWidth = srcWidth / poolSize;
        int dstHeight = srcHeight / poolSize;

        if (dstWidth == 0) dstWidth = 1;
        if (dstHeight == 0) dstHeight = 1;

        BufferedImage dst = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                // 计算当前池化区域的像素范围
                int startX = x * poolSize;
                int startY = y * poolSize;
                int endX = Math.min(startX + poolSize, srcWidth);
                int endY = Math.min(startY + poolSize, srcHeight);

                // 计算区域均值
                long sumR = 0, sumG = 0, sumB = 0;
                int count = 0;

                for (int py = startY; py < endY; py++) {
                    for (int px = startX; px < endX; px++) {
                        int rgb = src.getRGB(px, py);
                        sumR += (rgb >> 16) & 0xFF;
                        sumG += (rgb >> 8) & 0xFF;
                        sumB += rgb & 0xFF;
                        count++;
                    }
                }

                int avgR = (int) (sumR / count);
                int avgG = (int) (sumG / count);
                int avgB = (int) (sumB / count);

                int avgRgb = (avgR << 16) | (avgG << 8) | avgB;
                dst.setRGB(x, y, avgRgb);
            }
        }

        return dst;
    }

    /**
     * 最近邻插值放大
     */
    private BufferedImage nearestNeighborUpscale(BufferedImage src, int targetWidth, int targetHeight) {
        BufferedImage dst = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        double scaleX = (double) src.getWidth() / targetWidth;
        double scaleY = (double) src.getHeight() / targetHeight;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int srcX = (int) (x * scaleX);
                int srcY = (int) (y * scaleY);
                srcX = Math.min(srcX, src.getWidth() - 1);
                srcY = Math.min(srcY, src.getHeight() - 1);
                int rgb = src.getRGB(srcX, srcY);
                dst.setRGB(x, y, rgb);
            }
        }

        return dst;
    }

    /**
     * byte[] 转 BufferedImage
     */
    private BufferedImage bytesToImage(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * 写入4字节整数（大端序）
     */
    private void writeInt(ByteArrayOutputStream out, int value) {
        out.write((byte) ((value >> 24) & 0xFF));
        out.write((byte) ((value >> 16) & 0xFF));
        out.write((byte) ((value >> 8) & 0xFF));
        out.write((byte) (value & 0xFF));
    }

    /**
     * 读取4字节整数（大端序）
     */
    private int readInt(ByteArrayInputStream in) throws IOException {
        return ((in.read() & 0xFF) << 24) |
               ((in.read() & 0xFF) << 16) |
               ((in.read() & 0xFF) << 8) |
               (in.read() & 0xFF);
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
