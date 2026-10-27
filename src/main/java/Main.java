import javax.swing.SwingUtilities;

/**
 * Entry point for the Huffman Tree application.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HuffmanApp().setVisible(true);
        });
    }
}
