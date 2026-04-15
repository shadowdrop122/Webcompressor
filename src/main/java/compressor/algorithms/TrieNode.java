package compressor.algorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrieNode {
    private final Map<Character, TrieNode> children;
    private String replacement;
    private boolean isEndOfTag;
    private int tagLength;

    public TrieNode() {
        this.children = new HashMap<>();
        this.replacement = null;
        this.isEndOfTag = false;
        this.tagLength = 0;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public TrieNode getChild(char c) {
        return children.get(c);
    }

    public TrieNode addChild(char c) {
        return children.computeIfAbsent(c, k -> new TrieNode());
    }

    public boolean hasChild(char c) {
        return children.containsKey(c);
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isEndOfTag() {
        return isEndOfTag;
    }

    public void setEndOfTag(boolean endOfTag) {
        isEndOfTag = endOfTag;
    }

    public int getTagLength() {
        return tagLength;
    }

    public void setTagLength(int tagLength) {
        this.tagLength = tagLength;
    }

    public boolean hasReplacement() {
        return replacement != null;
    }

    @Override
    public String toString() {
        return "TrieNode{children=" + children.size() + ", replacement=" + replacement + "}";
    }
}
