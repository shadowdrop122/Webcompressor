package compressor.algorithms;

import compressor.core.AbstractCompressor;
import compressor.model.CompressionStats;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * LZW图像压缩器 - 使用 ZLIB (Deflate) 实现
 * Java标准库提供的无损压缩算法，保证正确性和可靠性
 */
public class LZWImageCompressor extends AbstractCompressor {

    public static final String ALGORITHM_NAME = "LZWImage";
    public static final String DESCRIPTION = "图像像素压缩 - 基于ZLIB/Deflate的无损压缩";
    private static final int MAGIC = 0x4C5A5731;

    public LZWImageCompressor() {}

    @Override
    public byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) return new byte[0];
        startTiming();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) throw new IOException("无法解析图片");

        byte[] pixels = extractPixels(img);
        int w = img.getWidth(), h = img.getHeight(), t = img.getType();
        byte[] comp = deflate(pixels);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, MAGIC);
        writeInt(out, t);
        writeInt(out, w);
        writeInt(out, h);
        writeInt(out, pixels.length);
        writeInt(out, comp.length);
        out.write(comp);

        byte[] res = out.toByteArray();
        endTiming(data.length, res.length);
        return res;
    }

    @Override
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length < 24) return new byte[0];
        startTiming();

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        if (readInt(in) != MAGIC) throw new IOException("魔数错误");
        readInt(in); // type
        int w = readInt(in), h = readInt(in);
        int origLen = readInt(in);
        int compLen = readInt(in);

        byte[] comp = new byte[compLen];
        in.read(comp);

        byte[] pixels = inflate(comp);
        if (pixels.length != origLen) {
            throw new IOException("解压长度不匹配: 期望 " + origLen + " 实际 " + pixels.length);
        }

        BufferedImage img = createImage(pixels, w, h);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);

        byte[] res = out.toByteArray();
        endTiming(res.length, data.length);
        return res;
    }

    /* ========== 像素处理 ========== */
    private byte[] extractPixels(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        byte[] p = new byte[w * h * 3];
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                p[idx++] = (byte) ((rgb >> 16) & 0xFF);
                p[idx++] = (byte) ((rgb >> 8) & 0xFF);
                p[idx++] = (byte) (rgb & 0xFF);
            }
        }
        return p;
    }

    private BufferedImage createImage(byte[] pix, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int p = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = pix[p++] & 0xFF;
                int g = pix[p++] & 0xFF;
                int b = pix[p++] & 0xFF;
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /* ========== ZLIB/DEFLATE 压缩 ========== */
    private byte[] deflate(byte[] data) throws IOException {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[4096];
        while (!deflater.finished()) {
            int len = deflater.deflate(buffer);
            baos.write(buffer, 0, len);
        }
        deflater.end();

        return baos.toByteArray();
    }

    private byte[] inflate(byte[] comp) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(comp);

        byte[] result = new byte[comp.length * 10];
        int len;
        try {
            len = inflater.inflate(result);
        } catch (DataFormatException e) {
            throw new IOException("解压失败: " + e.getMessage(), e);
        }
        inflater.end();

        byte[] pixels = new byte[len];
        System.arraycopy(result, 0, pixels, 0, len);
        return pixels;
    }

    /* ========== 工具 ========== */
    private void writeInt(ByteArrayOutputStream out, int v) {
        out.write((v >> 24) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private int readInt(ByteArrayInputStream in) throws IOException {
        return ((in.read() & 0xFF) << 24) |
               ((in.read() & 0xFF) << 16) |
               ((in.read() & 0xFF) << 8) |
               (in.read() & 0xFF);
    }

    @Override public String getAlgorithmName() { return ALGORITHM_NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
}
