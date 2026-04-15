package compressor.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private FileUtils() {
    }

    public static byte[] readFileToBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static byte[] readFileToBytes(String path) throws IOException {
        return readFileToBytes(Paths.get(path));
    }

    public static void writeBytesToFile(Path path, byte[] data) throws IOException {
        Files.write(path, data);
    }

    public static void writeBytesToFile(String path, byte[] data) throws IOException {
        writeBytesToFile(Paths.get(path), data);
    }

    public static String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    public static String getFileExtension(String path) {
        return getFileExtension(Paths.get(path));
    }

    public static String getFileName(Path path) {
        return path.getFileName().toString();
    }

    public static long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
