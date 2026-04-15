package compressor.algorithms;

import java.util.Objects;

public class HuffmanNode implements Comparable<HuffmanNode> {
    private final int byteValue;
    private final long frequency;
    private final HuffmanNode left;
    private final HuffmanNode right;
    private final boolean isLeaf;

    public HuffmanNode(int byteValue, long frequency, HuffmanNode left, HuffmanNode right) {
        this.byteValue = byteValue;
        this.frequency = frequency;
        this.left = left;
        this.right = right;
        this.isLeaf = (left == null && right == null);
    }

    public static HuffmanNode createLeaf(int byteValue, long frequency) {
        return new HuffmanNode(byteValue, frequency, null, null);
    }

    public static HuffmanNode createInternal(HuffmanNode left, HuffmanNode right) {
        return new HuffmanNode(-1, left.frequency + right.frequency, left, right);
    }

    public int getByteValue() {
        return byteValue;
    }

    public long getFrequency() {
        return frequency;
    }

    public HuffmanNode getLeft() {
        return left;
    }

    public HuffmanNode getRight() {
        return right;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        return Long.compare(this.frequency, other.frequency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HuffmanNode that = (HuffmanNode) o;
        return byteValue == that.byteValue &&
               frequency == that.frequency &&
               Objects.equals(left, that.left) &&
               Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(byteValue, frequency, left, right);
    }
}
