package compressor.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceDispatcherTest {

    @TempDir
    Path tempDir;

    private ResourceDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = ResourceDispatcher.getInstance();
    }

    @Test
    void classifyWebAndTextExtensions() {
        assertEquals(ResourceDispatcher.ResourceType.WEB_TEXT,
            dispatcher.classifyResourceType(tempDir.resolve("index.html")));
        assertEquals(ResourceDispatcher.ResourceType.WEB_TEXT,
            dispatcher.classifyResourceType(tempDir.resolve("styles.css")));
        assertEquals(ResourceDispatcher.ResourceType.WEB_TEXT,
            dispatcher.classifyResourceType(tempDir.resolve("app.js")));
        assertEquals(ResourceDispatcher.ResourceType.WEB_TEXT,
            dispatcher.classifyResourceType(tempDir.resolve("data.json")));
        assertEquals(ResourceDispatcher.ResourceType.WEB_TEXT,
            dispatcher.classifyResourceType(tempDir.resolve("notes.txt")));
    }

    @Test
    void classifyImageExtensions() {
        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(tempDir.resolve("photo.jpg")));
        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(tempDir.resolve("icon.png")));
        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(tempDir.resolve("anim.gif")));
        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(tempDir.resolve("preview.webp")));
        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(tempDir.resolve("bitmap.bmp")));
    }

    @Test
    void classifyBinaryAndFontExtensions() {
        assertEquals(ResourceDispatcher.ResourceType.BINARY_FONT,
            dispatcher.classifyResourceType(tempDir.resolve("font.woff")));
        assertEquals(ResourceDispatcher.ResourceType.BINARY_FONT,
            dispatcher.classifyResourceType(tempDir.resolve("font.ttf")));
        assertEquals(ResourceDispatcher.ResourceType.BINARY_FONT,
            dispatcher.classifyResourceType(tempDir.resolve("font.otf")));
        assertEquals(ResourceDispatcher.ResourceType.BINARY_FONT,
            dispatcher.classifyResourceType(tempDir.resolve("payload.bin")));
    }

    @Test
    void classifyNoExtensionImageByMagicNumber() throws IOException {
        Path file = tempDir.resolve("image_without_extension");
        Files.write(file, new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        });

        assertEquals(ResourceDispatcher.ResourceType.IMAGE,
            dispatcher.classifyResourceType(file));
    }

    @Test
    void classifyUnknownExtensionWithoutKnownMagicAsOther() throws IOException {
        Path file = tempDir.resolve("payload.custom");
        Files.writeString(file, "not an image");

        assertEquals(ResourceDispatcher.ResourceType.OTHER,
            dispatcher.classifyResourceType(file));
    }
}
