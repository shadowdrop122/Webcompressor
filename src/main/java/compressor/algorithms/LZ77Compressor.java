package compressor.algorithms;

import compressor.core.AbstractCompressor;
import compressor.utils.BitOutputStream;
import compressor.utils.BitInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class LZ77Compressor extends AbstractCompressor {

    public static final String ALGORITHM_NAME = "LZ77";
    public static final String DESCRIPTION = "LZ77滑动窗口字典压缩 - 基于重复模式的字典编码";
    private static final byte[] MAGIC = new byte[]{'L', 'Z', '7', '7'};

    private static final int DEFAULT_WINDOW_SIZE = 4096;
    private static final int DEFAULT_LOOKAHEAD_SIZE = 256;
    private static final int MIN_MATCH_LENGTH = 3;

    private final int windowSize;
    private final int lookaheadSize;

    public LZ77Compressor() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_LOOKAHEAD_SIZE);
    }

    public LZ77Compressor(int windowSize, int lookaheadSize) {
        super();
        this.windowSize = windowSize;
        this.lookaheadSize = Math.min(lookaheadSize, 258);
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
        long startTime = System.currentTimeMillis();

        List<Token> tokens = encode(data);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC[0]);
        out.write(MAGIC[1]);
        out.write(MAGIC[2]);
        out.write(MAGIC[3]);

        out.write((byte) (windowSize & 0xFF));
        out.write((byte) ((windowSize >> 8) & 0xFF));
        out.write((byte) (lookaheadSize & 0xFF));

        out.write(data.length & 0xFF);
        out.write((data.length >> 8) & 0xFF);
        out.write((data.length >> 16) & 0xFF);
        out.write((data.length >> 24) & 0xFF);

        ByteArrayOutputStream tokenStream = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(tokenStream);

        for (Token token : tokens) {
            bitOut.writeBit(token.isLiteral() ? 1 : 0);
            if (token.isLiteral()) {
                bitOut.writeByte(token.getLiteral());
            } else {
                int offset = token.getOffset();
                int length = token.getLength();
                int maxOffset = windowSize - 1;
                int maxLength = lookaheadSize - 1;

                int bitsForOffset = Integer.SIZE - Integer.numberOfLeadingZeros(maxOffset);
                int bitsForLength = Integer.SIZE - Integer.numberOfLeadingZeros(maxLength);

                bitOut.writeBits(offset, bitsForOffset);
                bitOut.writeBits(length, bitsForLength);
            }
        }

        bitOut.alignToByte();
        byte[] encoded = tokenStream.toByteArray();
        out.write(encoded, 0, encoded.length);

        byte[] result = out.toByteArray();
        endTiming(data.length, result.length);
        return result;
    }

    @Override
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length < 4 + 3 + 4) {
            return new byte[0];
        }

        startTiming();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] magic = new byte[4];
        in.read(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
            magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new IOException("无效的LZ77压缩文件格式");
        }

        int ws = in.read() | (in.read() << 8);
        int ls = in.read();
        int originalLength = in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);

        byte[] remaining = new byte[data.length - (4 + 3 + 4)];
        in.read(remaining);

        BitInputStream bitIn = new BitInputStream(remaining);
        byte[] result = new byte[originalLength];
        int resultIndex = 0;
        Deque<Byte> buffer = new ArrayDeque<>();

        int maxOffset = ws - 1;
        int maxLength = ls - 1;
        int bitsForOffset = Integer.SIZE - Integer.numberOfLeadingZeros(maxOffset);
        int bitsForLength = Integer.SIZE - Integer.numberOfLeadingZeros(maxLength);

        while (resultIndex < originalLength) {
            int isLiteral = bitIn.readBit();
            if (isLiteral == -1) break;

            if (isLiteral == 1) {
                int literal = bitIn.readByte();
                result[resultIndex++] = (byte) literal;
                buffer.offerLast((byte) literal);
                if (buffer.size() > windowSize) {
                    buffer.pollFirst();
                }
            } else {
                int offset = bitIn.readBits(bitsForOffset);
                int length = bitIn.readBits(bitsForLength);

                if (offset < 0 || length < 0) break;

                int copyStart = resultIndex - offset - 1;
                for (int i = 0; i < length + 1 && resultIndex < originalLength; i++) {
                    int srcIndex = copyStart + i;
                    if (srcIndex >= 0 && srcIndex < resultIndex) {
                        byte b = result[srcIndex];
                        result[resultIndex++] = b;
                        buffer.offerLast(b);
                        if (buffer.size() > windowSize) {
                            buffer.pollFirst();
                        }
                    }
                }
            }
        }

        endTiming(result.length, data.length);
        return result;
    }

    private List<Token> encode(byte[] data) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < data.length) {
            Match match = findLongestMatch(data, i);

            if (match.length >= MIN_MATCH_LENGTH) {
                tokens.add(new Token(match.offset, match.length));
                i += match.length;
            } else {
                tokens.add(new Token(data[i]));
                i++;
            }
        }

        return tokens;
    }

    private Match findLongestMatch(byte[] data, int position) {
        int windowStart = Math.max(0, position - windowSize);
        int lookaheadEnd = Math.min(data.length, position + lookaheadSize);

        int bestOffset = 0;
        int bestLength = 0;

        for (int i = windowStart; i < position; i++) {
            int matchLength = 0;

            while (position + matchLength < lookaheadEnd &&
                   data[i + matchLength] == data[position + matchLength]) {
                matchLength++;
                if (matchLength > lookaheadSize - 1) break;
            }

            if (matchLength > bestLength) {
                bestLength = matchLength;
                bestOffset = position - i - 1;
            }
        }

        return new Match(bestOffset, bestLength);
    }

    private static class Token {
        private final boolean isLiteral;
        private final byte literal;
        private final int offset;
        private final int length;

        public Token(byte literal) {
            this.isLiteral = true;
            this.literal = literal;
            this.offset = 0;
            this.length = 0;
        }

        public Token(int offset, int length) {
            this.isLiteral = false;
            this.literal = 0;
            this.offset = offset;
            this.length = length;
        }

        public boolean isLiteral() {
            return isLiteral;
        }

        public byte getLiteral() {
            return literal;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }

    private static class Match {
        final int offset;
        final int length;

        Match(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getLookaheadSize() {
        return lookaheadSize;
    }
}
