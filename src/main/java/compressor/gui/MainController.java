package compressor.gui;

import compressor.model.CompressionStats;
import compressor.utils.FileUtils;
import compressor.utils.TransferSimulator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {

    @FXML private ComboBox<CompressorFactory.CompressorType> algorithmComboBox;
    @FXML private ComboBox<TransferSimulator.BandwidthProfile> bandwidthComboBox;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private TextArea consoleOutput;
    @FXML private TextArea treeVisualization;
    @FXML private TextArea transferReport;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private Label currentAlgorithm;
    @FXML private Label memoryUsage;
    @FXML private Label selectedFilesCount;
    @FXML private Label selectedTotalSize;
    @FXML private Label chartOriginalSize;
    @FXML private Label chartCompressedSize;
    @FXML private Label chartSavings;
    @FXML private Spinner<Integer> treeDepthSpinner;
    @FXML private BarChart<String, Number> compressionChart;

    private final ObservableList<FileEntry> fileEntries = FXCollections.observableArrayList();
    private final CompressionService compressionService = new CompressionService();
    private Path outputDirectory;
    private CompressionService.CompressionResult lastResult;

    public static class FileEntry {
        private String fileName;
        private long fileSize;
        private String fileType;
        private String filePath;

        public FileEntry(String fileName, long fileSize, String fileType, String filePath) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.filePath = filePath;
        }

        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getFileSizeFormatted() { return FileUtils.formatFileSize(fileSize); }
        public String getFileType() { return fileType; }
        public String getFilePath() { return filePath; }
    }

    @FXML
    private void initialize() {
        algorithmComboBox.getItems().addAll(CompressorFactory.getAvailableTypes());
        algorithmComboBox.setValue(CompressorFactory.CompressorType.HUFFMAN);
        algorithmComboBox.setOnAction(e -> {
            currentAlgorithm.setText(algorithmComboBox.getValue().getName());
        });
        currentAlgorithm.setText(CompressorFactory.CompressorType.HUFFMAN.getName());

        bandwidthComboBox.getItems().addAll(TransferSimulator.getAllProfiles());
        bandwidthComboBox.setValue(TransferSimulator.BandwidthProfile.FOUR_G);
        bandwidthComboBox.setOnAction(e -> updateTransferReport());

        TableColumn<FileEntry, String> nameCol = new TableColumn<>("文件名");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setPrefWidth(150);

        TableColumn<FileEntry, String> sizeCol = new TableColumn<>("大小");
        sizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
            cell.getValue().getFileSizeFormatted()));
        sizeCol.setPrefWidth(80);

        TableColumn<FileEntry, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        typeCol.setPrefWidth(50);

        fileTable.getColumns().addAll(nameCol, sizeCol, typeCol);
        fileTable.setItems(fileEntries);

        setupCompressionService();
        startMemoryMonitor();

        appendLog("网页资源压缩系统 WebCompressor 已启动");
        appendLog("请选择文件或文件夹开始压缩");
    }

    private void setupCompressionService() {
        compressionService.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            statusLabel.setText("压缩完成");
            lastResult = compressionService.getValue();
            updateCompressionChart();
            updateTransferReport();
            if (lastResult.getHuffmanTree() != null) {
                treeVisualization.setText(lastResult.getHuffmanTree());
            }
        });

        compressionService.setOnFailed(e -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            statusLabel.setText("压缩失败");
            appendLog("错误: " + e.getSource().getException().getMessage());
        });

        compressionService.setOnCancelled(e -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            statusLabel.setText("已取消");
            appendLog("压缩已取消");
        });
    }

    private void startMemoryMonitor() {
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                    memoryUsage.setText("内存: " + usedMemory + " MB");
                });
            }
        }, 0, 2000);
    }

    @FXML
    private void handleSelectFiles(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择要压缩的文件");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("网页文件", "*.html", "*.htm", "*.css", "*.js"),
            new FileChooser.ExtensionFilter("文本文件", "*.txt", "*.log", "*.json", "*.xml"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        List<File> files = chooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                addFileToTable(file.toPath());
            }
            updateStatistics();
            appendLog("已添加 " + files.size() + " 个文件");
        }
    }

    @FXML
    private void handleSelectDirectory(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择文件夹");

        File dir = chooser.showDialog(null);
        if (dir != null) {
            try {
                java.nio.file.Files.walk(dir.toPath())
                    .filter(p -> java.nio.file.Files.isRegularFile(p))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(this::addFileToTable);
                updateStatistics();
                appendLog("已添加文件夹中的所有文件");
            } catch (Exception e) {
                appendLog("读取文件夹失败: " + e.getMessage());
            }
        }
    }

    private void addFileToTable(Path path) {
        if (java.nio.file.Files.exists(path)) {
            String fileName = path.getFileName().toString();
            long fileSize = 0;
            try {
                fileSize = FileUtils.getFileSize(path);
            } catch (java.io.IOException e) {
                appendLog("读取文件大小失败: " + fileName);
            }
            String fileType = FileUtils.getFileExtension(path);

            boolean exists = fileEntries.stream()
                .anyMatch(e -> e.getFilePath().equals(path.toAbsolutePath().toString()));

            if (!exists) {
                fileEntries.add(new FileEntry(fileName, fileSize, fileType, path.toAbsolutePath().toString()));
            }
        }
    }

    private void updateStatistics() {
        int count = fileEntries.size();
        long totalSize = fileEntries.stream().mapToLong(FileEntry::getFileSize).sum();

        selectedFilesCount.setText(count + " 个文件");
        selectedTotalSize.setText(FileUtils.formatFileSize(totalSize));
    }

    @FXML
    private void handleStartCompression(ActionEvent event) {
        if (fileEntries.isEmpty()) {
            showAlert("请先选择要压缩的文件");
            return;
        }

        if (compressionService.isRunning()) {
            showAlert("压缩任务正在进行中，请等待完成");
            return;
        }

        // 重置 service 状态，允许重新启动
        compressionService.reset();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择输出目录");
        File dir = chooser.showDialog(null);

        if (dir == null) {
            outputDirectory = Paths.get(System.getProperty("user.home"));
        } else {
            outputDirectory = dir.toPath();
        }

        List<Path> files = fileEntries.stream()
            .map(e -> Paths.get(e.getFilePath()))
            .collect(Collectors.toList());

        CompressorFactory.CompressorType type = algorithmComboBox.getValue();

        appendLog("=".repeat(50));
        appendLog("开始压缩任务...");
        appendLog("算法: " + type.getName());
        appendLog("输出目录: " + outputDirectory);
        appendLog("=".repeat(50));

        progressBar.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setVisible(true);
        progressLabel.setText("准备中...");
        statusLabel.setText("压缩中...");

        compressionService.configure(files, type, outputDirectory, this::appendLog);
        compressionService.start();
    }

    @FXML
    private void handleStopCompression(ActionEvent event) {
        if (compressionService.isRunning()) {
            compressionService.cancel();
        }
    }

    @FXML
    private void handleRemoveSelected(ActionEvent event) {
        FileEntry selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileEntries.remove(selected);
            updateStatistics();
        }
    }

    @FXML
    private void handleClearList(ActionEvent event) {
        fileEntries.clear();
        updateStatistics();
        consoleOutput.clear();
        if (compressionChart != null) {
            compressionChart.getData().clear();
        }
        chartOriginalSize.setText("0 B");
        chartCompressedSize.setText("0 B");
        chartSavings.setText("0%");
        appendLog("已清空所有文件");
    }

    @FXML
    private void handleRefreshTree() {
        if (lastResult != null) {
            if (lastResult.getHuffmanTree() != null) {
                treeVisualization.setText(lastResult.getHuffmanTree());
            } else if (lastResult.getTrieTree() != null) {
                treeVisualization.setText(lastResult.getTrieTree());
            }
        } else {
            appendLog("请先执行一次压缩以查看树结构");
        }
    }

    private void updateCompressionChart() {
        if (lastResult == null) return;

        compressionChart.getData().clear();

        XYChart.Series<String, Number> originalSeries = new XYChart.Series<>();
        originalSeries.setName("原始大小");

        XYChart.Series<String, Number> compressedSeries = new XYChart.Series<>();
        compressedSeries.setName("压缩后");

        for (CompressionService.CompressionResult.FileResult fr : lastResult.getFileResults()) {
            String shortName = fr.getFileName();
            if (shortName.length() > 15) {
                shortName = shortName.substring(0, 12) + "...";
            }
            originalSeries.getData().add(new XYChart.Data<>(shortName, fr.getOriginalSize()));
            compressedSeries.getData().add(new XYChart.Data<>(shortName, fr.getCompressedSize()));
        }

        compressionChart.getData().addAll(originalSeries, compressedSeries);

        long totalOriginal = lastResult.getTotalOriginalSize();
        long totalCompressed = lastResult.getTotalCompressedSize();
        chartOriginalSize.setText(FileUtils.formatFileSize(totalOriginal));
        chartCompressedSize.setText(FileUtils.formatFileSize(totalCompressed));

        if (totalOriginal > 0) {
            double savings = (1.0 - (double) totalCompressed / totalOriginal) * 100;
            chartSavings.setText(String.format("%.1f%%", savings));
        }
    }

    private void updateTransferReport() {
        if (lastResult == null) {
            transferReport.setText("请先执行压缩以查看传输分析");
            return;
        }

        TransferSimulator.BandwidthProfile profile = bandwidthComboBox.getValue();
        TransferSimulator simulator = new TransferSimulator(profile);

        String report = simulator.generateReport(
            lastResult.getTotalOriginalSize(),
            lastResult.getTotalCompressedSize()
        );

        transferReport.setText(report);
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            consoleOutput.appendText(message + "\n");
            consoleOutput.positionCaret(consoleOutput.getText().length());
        });
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        showAlert("网页资源压缩系统\n\n" +
                  "版本: 1.0.0\n\n" +
                  "支持算法:\n" +
                  "- Huffman: 哈夫曼编码\n" +
                  "- LZ77: 滑动窗口字典压缩\n" +
                  "- WebDict: 网页专用混合压缩");
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Platform.exit();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
