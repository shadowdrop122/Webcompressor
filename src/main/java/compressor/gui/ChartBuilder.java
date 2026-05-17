package compressor.gui;

import compressor.engine.ResourceDispatcher;
import compressor.utils.FileUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.List;
import java.util.Map;

public class ChartBuilder {

    public static BarChart<String, Number> createCompressionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("文件");
        xAxis.setTickLabelRotation(45);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("大小 (bytes)");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, " B") {
            @Override
            public String toString(Number object) {
                double value = object.doubleValue();
                if (value >= 1_000_000) {
                    return String.format("%.1fM", value / 1_000_000);
                } else if (value >= 1_000) {
                    return String.format("%.1fK", value / 1_000);
                }
                return String.format("%.0f", value);
            }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("压缩效果对比");
        chart.setLegendSide(Side.BOTTOM);
        chart.setAnimated(true);
        chart.setBarGap(5);
        chart.setCategoryGap(20);

        return chart;
    }

    public static void populateChart(BarChart<String, Number> chart,
                                    List<FileChartData> dataList) {
        chart.getData().clear();

        XYChart.Series<String, Number> originalSeries = new XYChart.Series<>();
        originalSeries.setName("原始大小");

        XYChart.Series<String, Number> compressedSeries = new XYChart.Series<>();
        compressedSeries.setName("压缩后");

        XYChart.Series<String, Number> savingsSeries = new XYChart.Series<>();
        savingsSeries.setName("节省空间");

        // 计算动态 Y 轴范围
        double maxValue = 0;
        for (FileChartData data : dataList) {
            if (data.originalSize > maxValue) maxValue = data.originalSize;
            if (data.compressedSize > maxValue) maxValue = data.compressedSize;
        }
        // 设置 Y 轴最大值为最大原始大小的 120%
        double yAxisMax = maxValue * 1.2;

        // 获取 Y 轴并动态设置范围
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        if (yAxis != null) {
            yAxis.setAutoRanging(false); // 禁用自动范围
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(Math.max(yAxisMax, 1024));
            yAxis.setTickUnit(calculateTickUnit(yAxisMax));
        }

        for (FileChartData data : dataList) {
            String label = shortenFileName(data.fileName);

            originalSeries.getData().add(new XYChart.Data<>(label, data.originalSize));
            compressedSeries.getData().add(new XYChart.Data<>(label, data.compressedSize));
            savingsSeries.getData().add(new XYChart.Data<>(label, data.originalSize - data.compressedSize));
        }

        chart.getData().addAll(originalSeries, compressedSeries);

        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                NodeBuilder.applyDataLabel(data);
            }
        }
    }

    /**
     * 根据最大值计算合适的刻度单位
     */
    private static double calculateTickUnit(double maxValue) {
        if (maxValue <= 1024) return 200;           // 小于 1KB
        if (maxValue <= 10_000) return 2000;        // 小于 10KB
        if (maxValue <= 100_000) return 20000;      // 小于 100KB
        if (maxValue <= 1_000_000) return 200000;   // 小于 1MB
        if (maxValue <= 10_000_000) return 2000000; // 小于 10MB
        if (maxValue <= 100_000_000) return 20000000; // 小于 100MB
        return maxValue / 5;                        // 其他情况
    }

    public static void updateChartWithSummary(BarChart<String, Number> chart,
                                             long totalOriginal,
                                             long totalCompressed) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("压缩对比");

        series.getData().add(new XYChart.Data<>("原始大小", totalOriginal));
        series.getData().add(new XYChart.Data<>("压缩后", totalCompressed));

        // 动态设置 Y 轴范围
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        if (yAxis != null) {
            double maxValue = Math.max(totalOriginal, totalCompressed);
            yAxis.setAutoRanging(false); // 禁用自动范围
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(maxValue * 1.2);
            yAxis.setTickUnit(calculateTickUnit(maxValue));
        }

        chart.getData().add(series);
    }

    public static ObservableList<XYChart.Series<String, Number>> createPieChartData(
            long original, long compressed) {
        ObservableList<XYChart.Series<String, Number>> data = FXCollections.observableArrayList();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("压缩统计");

        series.getData().add(new XYChart.Data<>("原始大小", original));
        series.getData().add(new XYChart.Data<>("压缩后", compressed));

        data.add(series);
        return data;
    }

    public static void updateResourceTypePieChart(PieChart chart,
                                                  FlowPane legend,
                                                  Map<ResourceDispatcher.ResourceType, Long> typeSizes) {
        chart.getData().clear();
        legend.getChildren().clear();
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);

        long totalSize = typeSizes.values().stream().mapToLong(Long::longValue).sum();
        if (totalSize <= 0) {
            chart.setTitle("暂无文件");
            Label emptyLabel = new Label("暂无文件");
            emptyLabel.getStyleClass().add("resource-empty-label");
            legend.getChildren().add(emptyLabel);
            return;
        }

        chart.setTitle("资源类型占比");
        for (ResourceDispatcher.ResourceType type : ResourceDispatcher.ResourceType.values()) {
            long size = typeSizes.getOrDefault(type, 0L);
            if (size <= 0) {
                continue;
            }

            double percent = (double) size * 100 / totalSize;
            PieChart.Data data = new PieChart.Data(
                String.format("%s %.1f%%", type.getDisplayName(), percent),
                size
            );
            chart.getData().add(data);
            applyPieSliceColor(data, getResourceTypeColor(type));
            legend.getChildren().add(createResourceLegendItem(type, size, percent));
        }
    }

    private static void applyPieSliceColor(PieChart.Data data, String color) {
        data.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-pie-color: " + color + ";");
            }
        });
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
    }

    private static HBox createResourceLegendItem(ResourceDispatcher.ResourceType type,
                                                long size,
                                                double percent) {
        Rectangle swatch = new Rectangle(12, 12);
        swatch.setArcWidth(2);
        swatch.setArcHeight(2);
        swatch.setFill(Color.web(getResourceTypeColor(type)));

        Label label = new Label(String.format(
            "%s：%s，占 %.1f%%",
            type.getDisplayName(),
            FileUtils.formatFileSize(size),
            percent
        ));

        HBox item = new HBox(6, swatch, label);
        item.getStyleClass().add("resource-legend-item");
        return item;
    }

    private static String getResourceTypeColor(ResourceDispatcher.ResourceType type) {
        return switch (type) {
            case WEB_TEXT -> "#1976d2";
            case IMAGE -> "#2e7d32";
            case BINARY_FONT -> "#f57c00";
            case OTHER -> "#757575";
        };
    }

    private static String shortenFileName(String fileName) {
        if (fileName == null) return "";
        if (fileName.length() <= 15) return fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String name = fileName.substring(0, dotIndex);
            String ext = fileName.substring(dotIndex);
            if (name.length() > 10) {
                return name.substring(0, 7) + "..." + ext;
            }
        }
        return fileName.substring(0, 12) + "...";
    }

    public static class FileChartData {
        public final String fileName;
        public final long originalSize;
        public final long compressedSize;

        public FileChartData(String fileName, long originalSize, long compressedSize) {
            this.fileName = fileName;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }

        public long getSavings() {
            return originalSize - compressedSize;
        }

        public double getSavingsPercent() {
            if (originalSize <= 0) return 0;
            return (1.0 - (double) compressedSize / originalSize) * 100;
        }
    }

    private static class NodeBuilder {
        public static void applyDataLabel(XYChart.Data<String, Number> data) {
            data.nodeProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    newValue.setStyle("-fx-font-size: 10px;");
                }
            });
        }
    }
}
