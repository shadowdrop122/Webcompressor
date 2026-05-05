package compressor.algorithms;

import compressor.core.AbstractCompressor;
import compressor.utils.BitInputStream;
import compressor.utils.BitOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class HuffmanCompressor extends AbstractCompressor {

    public static final String ALGORITHM_NAME = "Huffman";
    public static final String DESCRIPTION = "哈夫曼编码 - 基于字符频率的变长编码压缩算法";
    private static final byte[] MAGIC = new byte[]{'H', 'U', 'F', 'F'};

    private Map<Integer, String> codeTable;
    private Map<String, Integer> reverseCodeTable;
    private HuffmanNode root;

    public HuffmanCompressor() {
        super();
        this.codeTable = new HashMap<>();
        this.reverseCodeTable = new HashMap<>();
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        startTiming();
        long[] frequencies = countFrequencies(data);
        this.root = buildHuffmanTree(frequencies);

        codeTable.clear();
        reverseCodeTable.clear();
        generateCodeTable(this.root, new StringBuilder());

        BitOutputStream bitOut = new BitOutputStream();
        for (byte b : data) {
            String code = codeTable.get((int) b & 0xff);
            for (char c : code.toCharArray()) {
                bitOut.writeBit(c - '0');
            }
        }

        byte[] encodedData = bitOut.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC[0]);
        out.write(MAGIC[1]);
        out.write(MAGIC[2]);
        out.write(MAGIC[3]);
        out.write(frequencies.length & 0xff);
        for (long freq : frequencies) {
            writeLong(out, freq);
        }
        out.write(encodedData, 0, encodedData.length);

        byte[] result = out.toByteArray();
        endTiming(data.length, result.length);
        return result;
    }

    @Override
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length < 4 + 1 + 256 * 8) {
            return new byte[0];
        }

        startTiming();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] magic = new byte[4];
        in.read(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
            magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new IOException("无效的Huffman压缩文件格式");
        }

        int freqLen = in.read();

        long[] frequencies = new long[256];
        for (int i = 0; i < freqLen; i++) {
            frequencies[i] = readLong(in);
        }

        this.root = buildHuffmanTree(frequencies);

        long originalSize = 0;
        for (long freq : frequencies) {
            originalSize += freq;
        }

        byte[] encodedData = new byte[data.length - (4 + 1 + freqLen * 8)];
        in.read(encodedData);

        BitInputStream bitIn = new BitInputStream(encodedData);
        ByteArrayOutputStream result = new ByteArrayOutputStream((int) Math.min(originalSize, Integer.MAX_VALUE));

        int charsToRead = (int) originalSize;
        int charsRead = 0;
        HuffmanNode current = this.root;

        while (charsRead < charsToRead && current != null) {
            if (current.isLeaf()) {
                result.write(current.getByteValue());
                charsRead++;
                current = this.root;
            } else {
                int bit = bitIn.readBit();
                if (bit == -1) break;
                current = (bit == 0) ? current.getLeft() : current.getRight();
            }
        }

        byte[] resultData = result.toByteArray();
        endTiming(resultData.length, data.length);
        return resultData;
    }

    private long[] countFrequencies(byte[] data) {
        long[] frequencies = new long[256];
        for (byte b : data) {
            frequencies[(int) b & 0xff]++;
        }
        return frequencies;
    }

    private HuffmanNode buildHuffmanTree(long[] frequencies) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();

        int count = 0;
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] > 0) {
                pq.offer(HuffmanNode.createLeaf(i, frequencies[i]));
                count++;
            }
        }

        if (count == 0) {
            return HuffmanNode.createLeaf(0, 0);
        }

        if (count == 1) {
            HuffmanNode single = pq.poll();
            return HuffmanNode.createInternal(
                HuffmanNode.createLeaf(single.getByteValue(), single.getFrequency()),
                HuffmanNode.createLeaf(0, 0)
            );
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            pq.offer(HuffmanNode.createInternal(left, right));
        }

        return pq.poll();
    }

    private void generateCodeTable(HuffmanNode node, StringBuilder prefix) {
        if (node == null) return;

        if (node.isLeaf()) {
            String code = prefix.length() == 0 ? "0" : prefix.toString();
            codeTable.put(node.getByteValue(), code);
            reverseCodeTable.put(code, node.getByteValue());
            return;
        }

        prefix.append('0');
        generateCodeTable(node.getLeft(), prefix);
        prefix.deleteCharAt(prefix.length() - 1);

        prefix.append('1');
        generateCodeTable(node.getRight(), prefix);
        prefix.deleteCharAt(prefix.length() - 1);
    }

    private void writeLong(ByteArrayOutputStream out, long value) throws IOException {
        for (int i = 7; i >= 0; i--) {
            out.write((int) (value >> (i * 8)) & 0xff);
        }
    }

    private long readLong(ByteArrayInputStream in) throws IOException {
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            int b = in.read();
            if (b == -1) b = 0;
            value |= ((long) b) << (i * 8);
        }
        return value;
    }

    public String getTreeStructure() {
        return getTreeStructure("", "", this.root);
    }

    private String getTreeStructure(String prefix, String edgeLabel, HuffmanNode node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        if (edgeLabel.length() > 0) {
            sb.append(prefix).append(edgeLabel).append(" ");
            if (node.isLeaf()) {
                int b = node.getByteValue();
                String charStr = (b >= 32 && b < 127) ? String.format("'%c'", (char) b) :
                                 String.format("(0x%02X)", b);
                sb.append(charStr).append(" [freq=").append(node.getFrequency()).append("]\n");
            } else {
                sb.append("[freq=").append(node.getFrequency()).append("]\n");
                sb.append(getTreeStructure(prefix + "    ", "├── 0", node.getLeft()));
                sb.append(getTreeStructure(prefix + "    ", "└── 1", node.getRight()));
            }
        } else {
            sb.append("[ROOT");
            if (node.isLeaf()) {
                int b = node.getByteValue();
                String charStr = (b >= 32 && b < 127) ? String.format("'%c'", (char) b) :
                                 String.format("(0x%02X)", b);
                sb.append(" ").append(charStr).append(" [freq=").append(node.getFrequency()).append("]\n");
            } else {
                sb.append(" freq=").append(node.getFrequency()).append("]\n");
                sb.append(getTreeStructure(prefix + "    ", "├── 0", node.getLeft()));
                sb.append(getTreeStructure(prefix + "    ", "└── 1", node.getRight()));
            }
        }
        return sb.toString();
    }

    public Map<Integer, String> getCodeTable() {
        return Collections.unmodifiableMap(codeTable);
    }
}
