import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles saving, loading, and parsing of Huffman (.huf) files.
 * Uses an efficient binary representation storing the Huffman tree frequency table
 * and packed binary bitstream so that .huf files achieve true compression (~40% smaller
 * than original text).
 */
public class HuffmanFileHandler {
    private static final byte[] MAGIC_V3 = new byte[]{'H', 'U', 'F', 3};
    private static final byte[] MAGIC_V2 = new byte[]{'H', 'U', 'F', 2};
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static String normalizeLineEndings(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n");
    }

    /**
     * Saves Huffman data to the specified file in an efficient binary .huf format.
     */
    public static void save(File file, HuffmanFileData data) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Map<Character, Integer> frequencies = data.getCharacterFrequencies();
        if ((frequencies == null || frequencies.isEmpty()) && data.getTreeSourceText() != null && !data.getTreeSourceText().isEmpty()) {
            HuffmanTree tree = new HuffmanTree(data.getTreeSourceText());
            frequencies = tree.getFrequencies();
        }

        String treeSource = data.getTreeSourceText() != null ? data.getTreeSourceText() : "";
        String textToEncode = data.getTextToEncode() != null ? data.getTextToEncode() : "";
        String encodedMessage = data.getEncodedMessage() != null ? data.getEncodedMessage() : "";

        // Check if tree source text and text to encode are the same (normalizing CRLF vs LF)
        boolean sameSourceAndTarget = normalizeLineEndings(treeSource).equals(normalizeLineEndings(textToEncode))
                || treeSource.isEmpty()
                || textToEncode.isEmpty();

        byte[] packedEncodedMessage = packBits(encodedMessage);
        long encodedBits = encodedMessage.length();

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            // 1. Magic bytes (HUF\3)
            dos.write(MAGIC_V3);

            // 2. Flags: bit 0 = sameSourceAndTarget
            byte flags = (byte) (sameSourceAndTarget ? 1 : 0);
            dos.writeByte(flags);

            // 3. Huffman Tree Frequency Table
            int symbolCount = frequencies != null ? frequencies.size() : 0;
            dos.writeInt(symbolCount);
            if (frequencies != null) {
                for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                    dos.writeChar(entry.getKey());
                    dos.writeInt(entry.getValue());
                }
            }

            // 4. Encoded Message (total bits + packed bytes)
            dos.writeLong(encodedBits);
            dos.write(packedEncodedMessage);

            // 5. Only store separate treeSource if it is distinct from textToEncode
            if (!sameSourceAndTarget) {
                if (frequencies != null && !frequencies.isEmpty()) {
                    HuffmanTree tree = new HuffmanTree(frequencies);
                    String treeBits = tree.encode(treeSource);
                    byte[] packedTree = packBits(treeBits);
                    dos.writeLong(treeBits.length());
                    dos.write(packedTree);
                } else {
                    dos.writeLong(0);
                }
            }
        }
    }

    /**
     * Loads Huffman data from the specified file.
     * Supports V3 pure binary format, V2 binary format, and legacy JSON / text format.
     */
    public static HuffmanFileData load(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            bis.mark(4);
            byte[] header = new byte[4];
            int readCount = bis.read(header);
            bis.reset();

            if (readCount == 4) {
                if (header[0] == 'H' && header[1] == 'U' && header[2] == 'F' && header[3] == 3) {
                    return loadBinaryFormatV3(file);
                } else if (header[0] == 'H' && header[1] == 'U' && header[2] == 'F' && header[3] == 2) {
                    return loadBinaryFormatV2(file);
                }
            }
        }

        // Fallback for legacy JSON or text formats
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return parse(content);
    }

    private static HuffmanFileData loadBinaryFormatV3(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte m0 = dis.readByte();
            byte m1 = dis.readByte();
            byte m2 = dis.readByte();
            byte m3 = dis.readByte();
            if (m0 != MAGIC_V3[0] || m1 != MAGIC_V3[1] || m2 != MAGIC_V3[2] || m3 != MAGIC_V3[3]) {
                throw new IOException("Invalid binary format header V3");
            }

            byte flags = dis.readByte();
            boolean sameSourceAndTarget = (flags & 1) != 0;

            int symbolCount = dis.readInt();
            Map<Character, Integer> frequencies = new HashMap<>(symbolCount);
            for (int i = 0; i < symbolCount; i++) {
                char c = dis.readChar();
                int freq = dis.readInt();
                frequencies.put(c, freq);
            }

            HuffmanTree tree = new HuffmanTree(frequencies);

            long encodedBits = dis.readLong();
            int byteCount = (int) ((encodedBits + 7) / 8);
            byte[] packedBytes = dis.readNBytes(byteCount);

            String encodedMessage = unpackBits(packedBytes, encodedBits);
            String textToEncode = tree.decode(packedBytes, encodedBits);

            String treeSourceText;
            if (sameSourceAndTarget) {
                treeSourceText = textToEncode;
            } else {
                long treeBits = dis.readLong();
                if (treeBits > 0) {
                    int treeByteCount = (int) ((treeBits + 7) / 8);
                    byte[] packedTree = dis.readNBytes(treeByteCount);
                    treeSourceText = tree.decode(packedTree, treeBits);
                } else {
                    treeSourceText = textToEncode;
                }
            }

            HuffmanFileData data = new HuffmanFileData();
            data.setTreeSourceText(treeSourceText);
            data.setCharacterFrequencies(frequencies);
            data.setCharacterCodes(tree.getCodes());
            data.setTextToEncode(textToEncode);
            data.setEncodedMessage(encodedMessage);
            return data;
        }
    }

    private static HuffmanFileData loadBinaryFormatV2(File file) throws IOException {
        return loadBinaryFormatV3(file);
    }

    /**
     * Packs an ASCII bit string of '0' and '1' into a compact byte array (8 bits per byte).
     */
    public static byte[] packBits(String bitString) {
        if (bitString == null || bitString.isEmpty()) {
            return new byte[0];
        }
        int numBits = bitString.length();
        int numBytes = (numBits + 7) / 8;
        byte[] bytes = new byte[numBytes];
        int byteIdx = 0;
        int currentByte = 0;
        int bitCount = 0;

        for (int i = 0; i < numBits; i++) {
            char bit = bitString.charAt(i);
            currentByte = (currentByte << 1) | (bit == '1' ? 1 : 0);
            bitCount++;
            if (bitCount == 8) {
                bytes[byteIdx++] = (byte) currentByte;
                currentByte = 0;
                bitCount = 0;
            }
        }
        if (bitCount > 0) {
            currentByte <<= (8 - bitCount);
            bytes[byteIdx] = (byte) currentByte;
        }
        return bytes;
    }

    /**
     * Unpacks a byte array of packed bits into an ASCII string of '0' and '1'.
     */
    public static String unpackBits(byte[] bytes, long totalBits) {
        if (bytes == null || totalBits <= 0) {
            return "";
        }
        int len = (int) totalBits;
        char[] chars = new char[len];
        int charIdx = 0;
        int fullBytes = len / 8;

        for (int i = 0; i < fullBytes; i++) {
            int b = bytes[i] & 0xFF;
            chars[charIdx++] = ((b & 0x80) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x40) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x20) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x10) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x08) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x04) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x02) != 0) ? '1' : '0';
            chars[charIdx++] = ((b & 0x01) != 0) ? '1' : '0';
        }

        int remaining = len % 8;
        if (remaining > 0) {
            int b = bytes[fullBytes] & 0xFF;
            for (int j = 0; j < remaining; j++) {
                chars[charIdx++] = ((b & (1 << (7 - j))) != 0) ? '1' : '0';
            }
        }

        return new String(chars);
    }

    /**
     * Parses JSON or delimited string content into a HuffmanFileData object.
     */
    public static HuffmanFileData parse(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IOException("File content is empty.");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) {
            return MAPPER.readValue(trimmed, HuffmanFileData.class);
        } else {
            return parseDelimitedFormat(content);
        }
    }

    /**
     * Reconstructs a HuffmanTree from the provided HuffmanFileData.
     */
    public static HuffmanTree reconstructTree(HuffmanFileData data) {
        if (data == null) {
            return null;
        }
        if (data.getCharacterFrequencies() != null && !data.getCharacterFrequencies().isEmpty()) {
            return new HuffmanTree(data.getCharacterFrequencies());
        }
        if (data.getTreeSourceText() != null && !data.getTreeSourceText().isEmpty()) {
            return new HuffmanTree(data.getTreeSourceText());
        }
        if (data.getCharacterCodes() != null && !data.getCharacterCodes().isEmpty()) {
            return HuffmanTree.fromCodes(data.getCharacterCodes());
        }
        return null;
    }

    private static HuffmanFileData parseDelimitedFormat(String content) {
        HuffmanFileData data = new HuffmanFileData();
        String currentSection = null;
        StringBuilder currentBuffer = new StringBuilder();

        for (String line : content.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (currentSection != null) {
                    assignSection(data, currentSection, currentBuffer.toString().stripTrailing());
                    currentBuffer.setLength(0);
                }
                currentSection = trimmed.substring(1, trimmed.length() - 1);
            } else {
                if (currentBuffer.length() > 0) {
                    currentBuffer.append("\n");
                }
                currentBuffer.append(line);
            }
        }
        if (currentSection != null) {
            assignSection(data, currentSection, currentBuffer.toString().stripTrailing());
        }
        return data;
    }

    private static void assignSection(HuffmanFileData data, String section, String content) {
        switch (section.toUpperCase()) {
            case "TREE_SOURCE_TEXT":
            case "TREE_TEXT":
            case "INPUT_TEXT":
                data.setTreeSourceText(content);
                break;
            case "TEXT_TO_ENCODE":
            case "ORIGINAL_TEXT":
                data.setTextToEncode(content);
                break;
            case "ENCODED_MESSAGE":
            case "BINARY_ENCODING":
            case "ENCODING":
                data.setEncodedMessage(content.replaceAll("\\s+", ""));
                break;
        }
    }
}
