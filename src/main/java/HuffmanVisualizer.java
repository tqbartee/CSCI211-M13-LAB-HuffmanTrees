import javax.swing.*;
import java.awt.*;

/**
 * A JPanel that draws a graphical representation of the Huffman Tree.
 */
public class HuffmanVisualizer extends JPanel {
    private final HuffmanNode root;
    private final int nodeRadius = 20;
    private final int verticalGap = 60;

    public HuffmanVisualizer(HuffmanNode root) {
        this.root = root;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawNode(g2d, root, getWidth() / 2, 40, getWidth() / 4);
        }
    }

    private void drawNode(Graphics2D g, HuffmanNode node, int x, int y, int hGap) {
        if (node == null) return;

        // Draw edges to children
        if (node.left != null) {
            int childX = x - hGap;
            int childY = y + verticalGap;
            g.setColor(Color.BLACK);
            g.drawLine(x, y, childX, childY);
            g.drawString("0", (x + childX) / 2 - 10, (y + childY) / 2);
            drawNode(g, node.left, childX, childY, hGap / 2);
        }

        if (node.right != null) {
            int childX = x + hGap;
            int childY = y + verticalGap;
            g.setColor(Color.BLACK);
            g.drawLine(x, y, childX, childY);
            g.drawString("1", (x + childX) / 2 + 10, (y + childY) / 2);
            drawNode(g, node.right, childX, childY, hGap / 2);
        }

        // Draw the node itself
        g.setColor(node.isLeaf() ? new Color(173, 216, 230) : Color.LIGHT_GRAY);
        g.fillOval(x - nodeRadius, y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
        g.setColor(Color.BLACK);
        g.drawOval(x - nodeRadius, y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

        // Text in node
        String label = String.valueOf(node.frequency);
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g.drawString(label, x - labelWidth / 2, y + fm.getAscent() / 2 - 2);

        // Character label and code for leaves
        if (node.isLeaf()) {
            String charDisplay = node.character == ' ' ? "[space]" : 
                               node.character == '\n' ? "\\n" : String.valueOf(node.character);
            g.setColor(Color.BLUE);
            String labelStr = "'" + charDisplay + "'";
            g.drawString(labelStr, x - fm.stringWidth(labelStr) / 2, y + nodeRadius + 15);
            
            g.setColor(new Color(0, 100, 0)); // Dark green for the code
            String codeStr = "(" + node.code + ")";
            g.drawString(codeStr, x - fm.stringWidth(codeStr) / 2, y + nodeRadius + 30);
        }
    }

    /**
     * Shows the tree in a separate window.
     */
    public static void showTree(HuffmanNode root) {
        JFrame frame = new JFrame("Huffman Tree Visualization");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        HuffmanVisualizer visualizer = new HuffmanVisualizer(root);
        JScrollPane scrollPane = new JScrollPane(visualizer);
        frame.add(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
