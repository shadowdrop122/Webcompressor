package compressor.algorithms;

import java.util.*;

public class TrieDictionary {

    private final TrieNode root;
    private final TrieNode reverseRoot;
    private final Map<String, String> replacementMap;
    private int matchCount;
    private int replacementBytesSaved;

    public TrieDictionary() {
        this.root = new TrieNode();
        this.reverseRoot = new TrieNode();
        this.replacementMap = new HashMap<>();
        this.matchCount = 0;
        this.replacementBytesSaved = 0;
        buildTrie();
    }

    private void buildTrie() {
        Map<String, String> allMappings = WebDictionary.getAllMappings();
        for (Map.Entry<String, String> entry : allMappings.entrySet()) {
            String tag = entry.getKey();
            String replacement = entry.getValue();
            insert(tag, replacement);
        }
        buildReverseTrie();
    }

    private void buildReverseTrie() {
        for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
            String tag = entry.getKey();
            String replacement = entry.getValue();

            TrieNode current = reverseRoot;
            for (int i = 0; i < replacement.length(); i++) {
                char c = replacement.charAt(i);
                current = current.addChild(c);
            }
            current.setEndOfTag(true);
            current.setReplacement(tag);
            current.setTagLength(replacement.length());
        }
    }

    public void insert(String tag, String replacement) {
        TrieNode current = root;
        for (char c : tag.toCharArray()) {
            current = current.addChild(c);
        }
        current.setEndOfTag(true);
        current.setReplacement(replacement);
        current.setTagLength(tag.length());
        replacementMap.put(tag, replacement);
    }

    public MatchResult findLongestMatch(String text, int startIndex) {
        TrieNode current = root;
        int matchLength = 0;
        String matchedTag = null;
        String replacement = null;
        int matchedTagLength = 0;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            TrieNode next = current.getChild(c);

            if (next == null) {
                break;
            }

            current = next;
            matchLength++;

            if (current.isEndOfTag()) {
                matchedTag = text.substring(startIndex, startIndex + matchLength);
                replacement = current.getReplacement();
                matchedTagLength = current.getTagLength();
            }
        }

        if (matchedTag != null) {
            return new MatchResult(true, matchedTag, replacement, matchedTagLength, matchLength);
        }

        return new MatchResult(false, null, null, 0, 0);
    }

    public List<MatchResult> findAllMatches(String text) {
        List<MatchResult> matches = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            MatchResult result = findLongestMatch(text, i);
            if (result.isMatched()) {
                matches.add(result);
                i += result.getMatchedTagLength();
            } else {
                i++;
            }
        }

        return matches;
    }

    public String applyReplacements(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        matchCount = 0;
        replacementBytesSaved = 0;

        while (i < text.length()) {
            MatchResult matchResult = findLongestMatch(text, i);

            if (matchResult.isMatched()) {
                result.append(matchResult.getReplacement());
                matchCount++;
                replacementBytesSaved += matchResult.getMatchedTagLength() - matchResult.getReplacement().length();
                i += matchResult.getMatchedTagLength();
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    public String removeReplacements(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            MatchResult match = findLongestReverseMatch(text, i);

            if (match != null) {
                result.append(match.getReplacement());
                i += match.getMatchLength();
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    private MatchResult findLongestReverseMatch(String text, int startIndex) {
        TrieNode current = reverseRoot;
        String longestMatch = null;
        int longestLength = 0;
        String original = null;
        int matchedTagLength = 0;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            TrieNode next = current.getChild(c);

            if (next == null) break;

            current = next;
            if (current.isEndOfTag()) {
                longestMatch = text.substring(startIndex, i + 1);
                longestLength = i - startIndex + 1;
                original = current.getReplacement();
                matchedTagLength = current.getTagLength();
            }
        }

        if (longestMatch != null) {
            return new MatchResult(true, longestMatch, original, matchedTagLength, longestLength);
        }
        return new MatchResult(false, null, null, 0, 0);
    }

    public String visualizeTree(int maxDepth) {
        return visualizeNode(root, "", "", 0, maxDepth);
    }

    private String visualizeNode(TrieNode node, String prefix, String edgeLabel, int depth, int maxDepth) {
        if (node == null || depth > maxDepth) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (!edgeLabel.isEmpty()) {
            sb.append(prefix).append(edgeLabel);
            if (node.isEndOfTag()) {
                sb.append(" [END -> '").append(escapeString(node.getReplacement())).append("']");
            }
            sb.append("\n");
        } else {
            sb.append("[ROOT]\n");
        }

        Map<Character, TrieNode> children = node.getChildren();
        List<Map.Entry<Character, TrieNode>> sortedChildren = new ArrayList<>(children.entrySet());
        sortedChildren.sort(Map.Entry.comparingByKey());

        int count = 0;
        for (Map.Entry<Character, TrieNode> entry : sortedChildren) {
            count++;
            String newPrefix = prefix + (edgeLabel.isEmpty() ? "    " : "    ");
            boolean isLast = (count == sortedChildren.size());
            String newEdgeLabel = (isLast ? "└── " : "├── ") + entry.getKey();
            sb.append(visualizeNode(entry.getValue(), newPrefix, newEdgeLabel, depth + 1, maxDepth));
        }

        return sb.toString();
    }

    private String escapeString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 32) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public int getMatchCount() {
        return matchCount;
    }

    public int getReplacementBytesSaved() {
        return replacementBytesSaved;
    }

    public int getTagCount() {
        return replacementMap.size();
    }

    public static class MatchResult {
        private final boolean matched;
        private final String matchedTag;
        private final String replacement;
        private final int matchedTagLength;
        private final int matchLength;

        public MatchResult(boolean matched, String matchedTag, String replacement,
                         int matchedTagLength, int matchLength) {
            this.matched = matched;
            this.matchedTag = matchedTag;
            this.replacement = replacement;
            this.matchedTagLength = matchedTagLength;
            this.matchLength = matchLength;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getMatchedTag() {
            return matchedTag;
        }

        public String getReplacement() {
            return replacement;
        }

        public int getMatchedTagLength() {
            return matchedTagLength;
        }

        public int getMatchLength() {
            return matchLength;
        }

        public int getBytesSaved() {
            return matchedTagLength - replacement.length();
        }

        @Override
        public String toString() {
            if (matched) {
                return String.format("Match{%s -> %s (saved %d bytes)}",
                    matchedTag, replacement, getBytesSaved());
            }
            return "NoMatch";
        }
    }
}
