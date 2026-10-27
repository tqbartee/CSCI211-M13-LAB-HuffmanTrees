import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for validating generated student homework files:
 * 1. ParticipationActivity13.4.1.txt existence and content
 * 2. ParticipationActivity13.4.1.huf existence and content
 * 3. CompleteWorksOfWilliamShakespeare.huf existence and content matching CompleteWorksOfWilliamShakespeare.txt
 */
public class HuffmanFileTest {

    /**
     * Helper to search for a file in common project directories (datafiles/ or root),
     * supporting multiple filename variations and case-insensitivity.
     */
    private File findDataFile(String... filenames) {
        File[] searchDirs = new File[]{
                new File("datafiles"),
                new File(".")
        };

        for (String filename : filenames) {
            for (File dir : searchDirs) {
                File directCheck = new File(dir, filename);
                if (directCheck.exists()) {
                    return directCheck;
                }

                if (dir.exists() && dir.isDirectory()) {
                    File[] entries = dir.listFiles();
                    if (entries != null) {
                        for (File entry : entries) {
                            if (entry.getName().equalsIgnoreCase(filename)) {
                                return entry;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Test
    public void testParticipationActivityTxtExistsAndContent() throws IOException {
        File txtFile = findDataFile(
                "ParticipationActivity13.4.1.txt",
                "ParticationActivity13.4.1.txt"
        );

        assertNotNull(txtFile, "ParticipationActivity13.4.1.txt should exist in datafiles directory");
        assertTrue(txtFile.exists() && txtFile.isFile(), "ParticipationActivity13.4.1.txt must exist as a file");
        assertTrue(txtFile.length() > 0, "ParticipationActivity13.4.1.txt should not be empty");

        String content = Files.readString(txtFile.toPath(), StandardCharsets.UTF_8).trim();
        assertEquals("aabbbaaccd", content,
                "Content of ParticipationActivity13.4.1.txt should match 'aabbbaaccd'");
    }

    @Test
    public void testParticipationActivityHufExistsAndContent() throws IOException {
        File hufFile = findDataFile(
                "ParticipationActivity13.4.1.huf",
                "ParticationActivity13.4.1.huf"
        );

        assertNotNull(hufFile, "ParticipationActivity13.4.1.huf should exist in datafiles directory");
        assertTrue(hufFile.exists() && hufFile.isFile(), "ParticipationActivity13.4.1.huf must exist as a file");
        assertTrue(hufFile.length() > 0, "ParticipationActivity13.4.1.huf should not be empty");

        HuffmanFileData data = HuffmanFileHandler.load(hufFile);
        assertNotNull(data, "Loaded Huffman data should not be null");

        String decodedText = data.getTextToEncode();
        if (decodedText == null || decodedText.isEmpty()) {
            decodedText = data.getTreeSourceText();
        }
        assertNotNull(decodedText, "Decoded text should not be null");
        assertEquals("aabbbaaccd", decodedText.trim(),
                "Decoded content of ParticipationActivity13.4.1.huf should match 'aabbbaaccd'");

        assertNotNull(data.getCharacterCodes(), "Character codes should be present in the loaded .huf file");
        assertFalse(data.getCharacterCodes().isEmpty(), "Character codes should not be empty");

        // Verify reconstructing tree and decoding the encoded message
        HuffmanTree reconstructedTree = HuffmanFileHandler.reconstructTree(data);
        assertNotNull(reconstructedTree, "Huffman tree should be reconstructible from the loaded data");
        if (data.getEncodedMessage() != null && !data.getEncodedMessage().isEmpty()) {
            String decodedFromBits = reconstructedTree.decode(data.getEncodedMessage());
            assertEquals("aabbbaaccd", decodedFromBits.trim(),
                    "Decoding encoded bit message with reconstructed tree should yield 'aabbbaaccd'");
        }
    }

    @Test
    public void testCompleteWorksOfWilliamShakespeareHufExistsAndContent() throws IOException {
        File hufFile = findDataFile(
                "CompleteWorksOfWilliamShakespeare.huf",
                "WilliamShakespeare.huf"
        );
        assertNotNull(hufFile, "CompleteWorksOfWilliamShakespeare.huf should exist in datafiles directory");
        assertTrue(hufFile.exists() && hufFile.isFile(), "CompleteWorksOfWilliamShakespeare.huf must exist as a file");
        assertTrue(hufFile.length() > 0, "CompleteWorksOfWilliamShakespeare.huf should not be empty");

        File txtFile = findDataFile(
                "CompleteWorksOfWilliamShakespeare.txt",
                "WilliamShakespeare.txt"
        );
        assertNotNull(txtFile, "CompleteWorksOfWilliamShakespeare.txt should exist in datafiles directory");
        assertTrue(txtFile.exists() && txtFile.isFile(), "CompleteWorksOfWilliamShakespeare.txt must exist as a file");

        String originalText = Files.readString(txtFile.toPath(), StandardCharsets.UTF_8);

        // Uncompress the content of CompleteWorksOfWilliamShakespeare.huf
        HuffmanFileData loadedData = HuffmanFileHandler.load(hufFile);
        assertNotNull(loadedData, "Loaded Huffman data should not be null");

        String uncompressedText = loadedData.getTextToEncode();
        if (uncompressedText == null || uncompressedText.isEmpty()) {
            HuffmanTree tree = HuffmanFileHandler.reconstructTree(loadedData);
            if (tree != null && loadedData.getEncodedMessage() != null) {
                uncompressedText = tree.decode(loadedData.getEncodedMessage());
            }
        }

        assertNotNull(uncompressedText, "Uncompressed text from CompleteWorksOfWilliamShakespeare.huf should not be null");

        // Normalize line endings to ensure tests pass consistently across Windows (CRLF) and Linux/macOS (LF)
        String normalizedExpected = originalText.replace("\r\n", "\n");
        String normalizedActual = uncompressedText.replace("\r\n", "\n");

        assertEquals(normalizedExpected, normalizedActual,
                "Uncompressed content of CompleteWorksOfWilliamShakespeare.huf must match CompleteWorksOfWilliamShakespeare.txt");
    }
}
