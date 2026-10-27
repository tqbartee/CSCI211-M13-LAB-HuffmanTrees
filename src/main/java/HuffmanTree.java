import java.util.*;

/**
 * Logic for building and using a Huffman Tree.
 */
public class HuffmanTree {
    private final HuffmanNode root;
    private final Map<Character, String> charToCode = new HashMap<>();
    private final Map<Character, Integer> frequencies = new HashMap<>();
    private String sourceText;

    public HuffmanTree(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty.");
        }
        this.sourceText = text;

        for (char c : text.toCharArray()) {
            frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
        }

        this.root = buildTree(this.frequencies);
        generateCodes(this.root, "");
    }

    public HuffmanTree(Map<Character, Integer> frequencies) {
        if (frequencies == null || frequencies.isEmpty()) {
            throw new IllegalArgumentException("Input frequencies cannot be null or empty.");
        }
        this.frequencies.putAll(frequencies);
        this.root = buildTree(this.frequencies);
        generateCodes(this.root, "");
    }

    public HuffmanTree(HuffmanNode root, Map<Character, String> codes) {
        if (root == null) {
            throw new IllegalArgumentException("Root cannot be null.");
        }
        this.root = root;
        if (codes != null) {
            this.charToCode.putAll(codes);
        }
    }

    private HuffmanNode buildTree(Map<Character, Integer> freqMap) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            HuffmanNode t1 = pq.poll();
            HuffmanNode t2 = pq.poll();
            
            // To get a:0, b:10, c:110, d:111:
            // Always put the "alphabetically smaller" node on the left.
            if (t1.minChar < t2.minChar) {
                pq.add(new HuffmanNode(t1.frequency + t2.frequency, t1, t2));
            } else {
                pq.add(new HuffmanNode(t1.frequency + t2.frequency, t2, t1));
            }
        }

        return pq.poll();
    }

    private void generateCodes(HuffmanNode node, String code) {
        if (node == null) return;
        node.code = code.isEmpty() ? "0" : code;
        if (node.isLeaf()) {
            charToCode.put(node.character, node.code);
            return;
        }
        generateCodes(node.left, code + "0");
        generateCodes(node.right, code + "1");
    }

    public String encode(String text) {
        if (text == null || text.isEmpty()) return "";
        
        // Pre-allocate StringBuilder to avoid multiple re-sizing. 
        // A rough estimate is text length * average code length (at least 1 bit per char).
        StringBuilder encoded = new StringBuilder(text.length());
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String code = charToCode.get(c);
            if (code == null) {
                encoded.append("?"); // Char not in tree
            } else {
                encoded.append(code);
            }
        }
        return encoded.toString();
    }

    public String decode(String encodedText) {
        if (encodedText == null || encodedText.isEmpty() || root == null) {
            return "";
        }
        if (root.isLeaf()) {
            // Edge case: single distinct character tree
            return String.valueOf(root.character).repeat(encodedText.length());
        }

        StringBuilder decoded = new StringBuilder(encodedText.length() / 4 + 16);
        HuffmanNode current = root;
        for (int i = 0; i < encodedText.length(); i++) {
            char bit = encodedText.charAt(i);
            if (bit == '0') {
                current = current.left;
            } else if (bit == '1') {
                current = current.right;
            } else {
                continue; // Ignore non-bit characters
            }

            if (current != null && current.isLeaf()) {
                decoded.append(current.character);
                current = root;
            }
        }
        return decoded.toString();
    }

    public String decode(byte[] bytes, long totalBits) {
        if (bytes == null || totalBits <= 0 || root == null) {
            return "";
        }
        if (root.isLeaf()) {
            return String.valueOf(root.character).repeat((int) totalBits);
        }

        StringBuilder decoded = new StringBuilder((int) (totalBits / 4 + 16));
        HuffmanNode current = root;
        int fullBytes = (int) (totalBits / 8);

        for (int i = 0; i < fullBytes; i++) {
            int b = bytes[i] & 0xFF;
            for (int shift = 7; shift >= 0; shift--) {
                current = ((b >> shift) & 1) == 1 ? current.right : current.left;
                if (current.isLeaf()) {
                    decoded.append(current.character);
                    current = root;
                }
            }
        }

        int remaining = (int) (totalBits % 8);
        if (remaining > 0) {
            int b = bytes[fullBytes] & 0xFF;
            for (int shift = 7; shift >= 8 - remaining; shift--) {
                current = ((b >> shift) & 1) == 1 ? current.right : current.left;
                if (current.isLeaf()) {
                    decoded.append(current.character);
                    current = root;
                }
            }
        }

        return decoded.toString();
    }

    public static HuffmanTree fromCodes(Map<Character, String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("Codes cannot be null or empty.");
        }
        HuffmanNode root = new HuffmanNode();
        for (Map.Entry<Character, String> entry : codes.entrySet()) {
            char ch = entry.getKey();
            String code = entry.getValue();
            HuffmanNode curr = root;
            for (int i = 0; i < code.length(); i++) {
                char bit = code.charAt(i);
                if (bit == '0') {
                    if (curr.left == null) {
                        curr.left = new HuffmanNode();
                    }
                    curr = curr.left;
                } else if (bit == '1') {
                    if (curr.right == null) {
                        curr.right = new HuffmanNode();
                    }
                    curr = curr.right;
                }
            }
            curr.character = ch;
            curr.code = code;
            curr.minChar = ch;
        }
        computeMinChars(root);
        return new HuffmanTree(root, codes);
    }

    private static char computeMinChars(HuffmanNode node) {
        if (node == null) return Character.MAX_VALUE;
        if (node.isLeaf()) return node.minChar;
        char leftMin = computeMinChars(node.left);
        char rightMin = computeMinChars(node.right);
        node.minChar = (char) Math.min(leftMin, rightMin);
        return node.minChar;
    }

    public HuffmanNode getRoot() {
        return root;
    }

    public Map<Character, String> getCodes() {
        return Collections.unmodifiableMap(charToCode);
    }

    public Map<Character, Integer> getFrequencies() {
        return Collections.unmodifiableMap(frequencies);
    }

    public String getSourceText() {
        return sourceText;
    }
}
