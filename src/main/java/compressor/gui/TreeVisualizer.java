package compressor.gui;

import compressor.algorithms.HuffmanCompressor;
import compressor.algorithms.WebDictCompressor;
import compressor.algorithms.WebDictionary;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;

import java.util.Map;

public class TreeVisualizer {

    private static final double NODE_WIDTH = 88;
    private static final double NODE_HEIGHT = 42;
    private static final double HORIZONTAL_GAP = 30;
    private static final double LEVEL_GAP = 86;
    private static final double MARGIN = 28;

    public static Pane createHuffmanTreePane(Map<Integer, String> codeTable, int maxDepth) {
        Pane pane = new Pane();
        pane.getStyleClass().add("tree-canvas");

        if (codeTable == null || codeTable.isEmpty()) {
            addEmptyMessage(pane, "请使用 Huffman 算法完成一次压缩后查看树结构");
            return pane;
        }

        DisplayNode root = buildTree(codeTable);
        int depthLimit = Math.max(1, maxDepth);
        measure(root, 0, depthLimit);
        assignPositions(root, MARGIN, 0, depthLimit);
        drawNode(pane, root, depthLimit);

        pane.setMinWidth(Math.max(root.width + MARGIN * 2, 420));
        pane.setMinHeight((Math.min(maxDepth(root), depthLimit) + 1) * LEVEL_GAP + MARGIN * 2);
        return pane;
    }

    public static void displayWebDictInfo(TextArea textArea, WebDictCompressor compressor) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("WebDict 压缩信息\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append(compressor.getCompressionInfo()).append("\n\n");
        sb.append("字典大小: ").append(WebDictionary.size()).append(" 个词条\n");

        textArea.setText(sb.toString());
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
                String charStr = formatByteValue(byteValue);
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
            .mapToInt(String::length).max().orElse(0);
        int minLength = compressor.getCodeTable().values().stream()
            .mapToInt(String::length).min().orElse(0);

        sb.append("- 最短编码: ").append(minLength).append(" bits\n");
        sb.append("- 最长编码: ").append(maxLength).append(" bits\n");

        return sb.toString();
    }

    private static DisplayNode buildTree(Map<Integer, String> codeTable) {
        DisplayNode root = new DisplayNode("ROOT", "");
        codeTable.forEach((byteValue, code) -> {
            DisplayNode current = root;
            for (int i = 0; i < code.length(); i++) {
                char bit = code.charAt(i);
                if (bit == '0') {
                    if (current.left == null) {
                        current.left = new DisplayNode("0", code.substring(0, i + 1));
                    }
                    current = current.left;
                } else {
                    if (current.right == null) {
                        current.right = new DisplayNode("1", code.substring(0, i + 1));
                    }
                    current = current.right;
                }
            }
            current.byteValue = byteValue;
            current.leaf = true;
            current.label = formatByteValue(byteValue);
        });
        return root;
    }

    private static double measure(DisplayNode node, int depth, int maxDepth) {
        if (node == null) {
            return 0;
        }
        if (depth >= maxDepth || node.isVisibleLeaf()) {
            node.width = NODE_WIDTH;
            return node.width;
        }

        double leftWidth = measure(node.left, depth + 1, maxDepth);
        double rightWidth = measure(node.right, depth + 1, maxDepth);
        if (leftWidth == 0 && rightWidth == 0) {
            node.width = NODE_WIDTH;
        } else if (leftWidth == 0 || rightWidth == 0) {
            node.width = Math.max(NODE_WIDTH, leftWidth + rightWidth);
        } else {
            node.width = Math.max(NODE_WIDTH, leftWidth + rightWidth + HORIZONTAL_GAP);
        }
        return node.width;
    }

    private static void assignPositions(DisplayNode node, double left, int depth, int maxDepth) {
        if (node == null) {
            return;
        }
        node.x = left + node.width / 2;
        node.y = MARGIN + depth * LEVEL_GAP;

        if (depth >= maxDepth || node.isVisibleLeaf()) {
            return;
        }

        double childLeft = left;
        if (node.left != null) {
            assignPositions(node.left, childLeft, depth + 1, maxDepth);
            childLeft += node.left.width + HORIZONTAL_GAP;
        }
        if (node.right != null) {
            assignPositions(node.right, childLeft, depth + 1, maxDepth);
        }
    }

    private static void drawNode(Pane pane, DisplayNode node, int maxDepth) {
        drawNode(pane, node, 0, maxDepth);
    }

    private static void drawNode(Pane pane, DisplayNode node, int depth, int maxDepth) {
        if (node == null) {
            return;
        }

        boolean collapsed = depth >= maxDepth && !node.isVisibleLeaf();
        StackPane nodeBox = createNodeBox(node, collapsed);
        nodeBox.setLayoutX(node.x - NODE_WIDTH / 2);
        nodeBox.setLayoutY(node.y);
        pane.getChildren().add(nodeBox);

        if (collapsed || node.isVisibleLeaf()) {
            return;
        }

        drawChild(pane, node, node.left, "0", depth, maxDepth);
        drawChild(pane, node, node.right, "1", depth, maxDepth);
    }

    private static void drawChild(Pane pane, DisplayNode parent, DisplayNode child,
                                  String edgeLabel, int depth, int maxDepth) {
        if (child == null) {
            return;
        }

        Line line = new Line(
            parent.x,
            parent.y + NODE_HEIGHT,
            child.x,
            child.y
        );
        line.getStyleClass().add("tree-edge");
        pane.getChildren().add(0, line);

        Label label = new Label(edgeLabel);
        label.getStyleClass().add("tree-edge-label");
        label.setLayoutX((parent.x + child.x) / 2 - 6);
        label.setLayoutY((parent.y + child.y + NODE_HEIGHT) / 2 - 12);
        pane.getChildren().add(label);

        drawNode(pane, child, depth + 1, maxDepth);
    }

    private static StackPane createNodeBox(DisplayNode node, boolean collapsed) {
        Label label = new Label(collapsed ? node.prefix + "\n..." : node.displayText());
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        label.setMaxWidth(NODE_WIDTH - 10);

        StackPane box = new StackPane(label);
        box.setPrefSize(NODE_WIDTH, NODE_HEIGHT);
        box.getStyleClass().add("tree-node");
        if (node.leaf) {
            box.getStyleClass().add("tree-node-leaf");
        } else if (collapsed) {
            box.getStyleClass().add("tree-node-collapsed");
        } else {
            box.getStyleClass().add("tree-node-internal");
        }
        return box;
    }

    private static void addEmptyMessage(Pane pane, String message) {
        Label label = new Label(message);
        label.getStyleClass().add("tree-empty-label");
        label.setLayoutX(MARGIN);
        label.setLayoutY(MARGIN);
        pane.getChildren().add(label);
        pane.setMinWidth(420);
        pane.setMinHeight(160);
    }

    private static int maxDepth(DisplayNode node) {
        if (node == null || node.isVisibleLeaf()) {
            return 0;
        }
        return 1 + Math.max(maxDepth(node.left), maxDepth(node.right));
    }

    private static String formatByteValue(int byteValue) {
        if (byteValue >= 32 && byteValue < 127) {
            return String.format("'%c'", (char) byteValue);
        }
        return String.format("0x%02X", byteValue);
    }

    private static class DisplayNode {
        private String label;
        private final String prefix;
        private Integer byteValue;
        private boolean leaf;
        private DisplayNode left;
        private DisplayNode right;
        private double x;
        private double y;
        private double width;

        private DisplayNode(String label, String prefix) {
            this.label = label;
            this.prefix = prefix;
        }

        private boolean isVisibleLeaf() {
            return leaf || (left == null && right == null);
        }

        private String displayText() {
            if (byteValue != null) {
                return label + "\n" + prefix;
            }
            return prefix.isEmpty() ? label : prefix;
        }
    }
}
