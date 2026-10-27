/**
 * Represents a node in the Huffman Tree.
 */
public class HuffmanNode implements Comparable<HuffmanNode> {
    public char character;
    public int frequency;
    public char minChar; // Smallest character in this subtree
    public String code;  // Binary code for this node
    public HuffmanNode left;
    public HuffmanNode right;

    public HuffmanNode() {
        this.character = '\0';
    }

    public HuffmanNode(char character, int frequency) {
        this.character = character;
        this.frequency = frequency;
        this.minChar = character;
    }

    public HuffmanNode(int frequency, HuffmanNode left, HuffmanNode right) {
        this.character = '\0'; // Internal node
        this.frequency = frequency;
        this.left = left;
        this.right = right;
        this.minChar = (char) Math.min(left.minChar, right.minChar);
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        if (this.frequency != other.frequency) {
            return Integer.compare(this.frequency, other.frequency);
        }
        return Character.compare(this.minChar, other.minChar);
    }
}
