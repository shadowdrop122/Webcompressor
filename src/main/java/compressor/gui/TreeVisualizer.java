package compressor.gui;

import compressor.algorithms.HuffmanCompressor;
import compressor.algorithms.TrieDictionary;
import compressor.algorithms.WebDictCompressor;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.Map;

public class TreeVisualizer {

    public static TreeView<String> createHuffmanTreeView(HuffmanCompressor compressor) {
        String treeStructure = compressor.getTreeStructure();
        return createTreeViewFromText(treeStructure);
    }

    public static TreeView<String> createTrieTreeView(TrieDictionary trie, int maxDepth) {
        String treeStructure = trie.visualizeTree(maxDepth);
        return createTreeViewFromText(treeStructure);
    }

    public static void displayTreeInTextArea(TextArea textArea, HuffmanCompressor compressor) {
        textArea.setText(compressor.getTreeStructure());
    }

    public static void displayTrieInTextArea(TextArea textArea, TrieDictionary trie, int maxDepth) {
        textArea.setText(trie.visualizeTree(maxDepth));
    }

    public static void displayWebDictInfo(TextArea textArea, WebDictCompressor compressor) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("WebDict 压缩流水线信息\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append(compressor.getCompressionPipelineInfo()).append("\n\n");

        sb.append("-".repeat(50)).append("\n");
        sb.append("Trie 树结构 (前4层):\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append(compressor.getTrieTreeVisualization());

        textArea.setText(sb.toString());
    }

    public static TreeView<String> createTreeViewFromText(String treeText) {
        String[] lines = treeText.split("\n");
        TreeItem<String> root = new TreeItem<>("ROOT");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            int indentLevel = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ' || line.charAt(i) == '\u2502' ||
                    line.charAt(i) == '\u2500' || line.charAt(i) == '\u2514' ||
                    line.charAt(i) == '\u251C') {
                    indentLevel++;
                } else {
                    break;
                }
            }

            String displayText = line.trim();
            if (displayText.startsWith("[END")) {
                displayText = "\u2705 " + displayText;
            } else if (displayText.startsWith("[ROOT")) {
                displayText = "\uD83C\uDF10 " + displayText;
            } else if (displayText.contains("freq=")) {
                displayText = "\uD83D\uDCC8 " + displayText;
            }

            TreeItem<String> item = new TreeItem<>(displayText);

            TreeItem<String> parent = findParent(root, indentLevel / 4);
            if (parent != null) {
                parent.getChildren().add(item);
            } else {
                root.getChildren().add(item);
            }
        }

        TreeView<String> treeView = new TreeView<>(root);
        treeView.setShowRoot(true);
        treeView.setEditable(false);

        return treeView;
    }

    private static TreeItem<String> findParent(TreeItem<String> root, int targetLevel) {
        if (targetLevel <= 0) return null;

        return findParentRecursive(root, targetLevel, 0);
    }

    private static TreeItem<String> findParentRecursive(TreeItem<String> current, int targetLevel, int currentLevel) {
        if (currentLevel >= targetLevel - 1) {
            return current;
        }

        if (current.getChildren().isEmpty()) {
            return current;
        }

        return findParentRecursive(current.getChildren().get(current.getChildren().size() - 1),
                                 targetLevel, currentLevel + 1);
    }

    public static String generateCodeTableText(HuffmanCompressor compressor) {
        Map<Integer, String> codeTable = compressor.getCodeTable();

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("Huffman 编码表\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append(String.format("%-15s %-20s %s\n", "字符", "编码", "长度"));
        sb.append("-".repeat(50)).append("\n");

        codeTable.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e1.getKey(), e2.getKey()))
            .forEach(entry -> {
                int byteValue = entry.getKey();
                String code = entry.getValue();

                String charStr;
                if (byteValue >= 32 && byteValue < 127) {
                    charStr = String.format("'%c'", (char) byteValue);
                } else {
                    charStr = String.format("0x%02X", byteValue);
                }

                sb.append(String.format("%-15s %-20s %d\n", charStr, code, code.length()));
            });

        return sb.toString();
    }

    public static String generateSummary(HuffmanCompressor compressor, long originalSize, long compressedSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("Huffman 压缩统计\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append("原始大小: ").append(originalSize).append(" bytes\n");
        sb.append("压缩后: ").append(compressedSize).append(" bytes\n");
        if (originalSize > 0) {
            double ratio = (1.0 - (double) compressedSize / originalSize) * 100;
            double compressionRatio = (double) compressedSize / originalSize * 100;
            sb.append("压缩率: ").append(String.format("%.2f%%", compressionRatio)).append("\n");
            sb.append("节省空间: ").append(String.format("%.2f%%", ratio)).append("\n");
        }

        sb.append("\n编码表统计:\n");
        sb.append("- 唯一字符数: ").append(compressor.getCodeTable().size()).append("\n");

        int maxLength = compressor.getCodeTable().values().stream()
            .mapToInt(String::length)
            .max().orElse(0);
        int minLength = compressor.getCodeTable().values().stream()
            .mapToInt(String::length)
            .min().orElse(0);

        sb.append("- 最短编码: ").append(minLength).append(" bits\n");
        sb.append("- 最长编码: ").append(maxLength).append(" bits\n");

        return sb.toString();
    }
}
