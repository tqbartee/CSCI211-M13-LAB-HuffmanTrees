import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Main GUI for the Huffman Tree application.
 */
public class HuffmanApp extends JFrame {
    private final JTextArea inputForTree;
    private final JTextArea treeDisplay;
    private final JTextArea inputForEncoding;
    private final JTextArea encodingOutput;
    private HuffmanTree currentTree;
    private String fullEncodedMessage = "";

    public HuffmanApp() {
        setTitle("Huffman Tree Application");
        setSize(650, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel for Huffman Tree creation
        JPanel topPanel = new JPanel(new BorderLayout());
        inputForTree = new JTextArea(5, 40);
        JScrollPane inputScroll = new JScrollPane(inputForTree);
        topPanel.add(new JLabel("Enter text to create Huffman Tree:"), BorderLayout.NORTH);
        topPanel.add(inputScroll, BorderLayout.CENTER);

        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton readBtn = new JButton("Read from File");
        readBtn.addActionListener(e -> readFromFile());
        topButtonPanel.add(readBtn);

        JButton generateBtn = new JButton("Generate Huffman Tree");
        generateBtn.addActionListener(e -> generateHuffmanTree());
        topButtonPanel.add(generateBtn);

        topPanel.add(topButtonPanel, BorderLayout.SOUTH);

        // Center panel for Tree Display
        JPanel centerPanel = new JPanel(new BorderLayout());
        treeDisplay = new JTextArea(5, 40);
        treeDisplay.setEditable(false);
        JScrollPane treeScroll = new JScrollPane(treeDisplay);
        centerPanel.add(new JLabel("Huffman Tree (Codes):"), BorderLayout.NORTH);
        centerPanel.add(treeScroll, BorderLayout.CENTER);

        // Encoding panel
        JPanel encodingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weighty = 0.0;
        encodingPanel.add(new JLabel("Text to be encoded:"), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.3;
        inputForEncoding = new JTextArea(5, 40);
        JScrollPane encodeInputScroll = new JScrollPane(inputForEncoding);
        encodingPanel.add(encodeInputScroll, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.0;
        JPanel encodeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton readForEncodingBtn = new JButton("Read from File");
        readForEncodingBtn.addActionListener(e -> readFromFileForEncoding());
        encodeButtonPanel.add(readForEncodingBtn);

        JButton encodeBtn = new JButton("Encode Text");
        encodeBtn.addActionListener(e -> encodeText());
        encodeButtonPanel.add(encodeBtn);
        encodingPanel.add(encodeButtonPanel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 0.0;
        encodingPanel.add(new JLabel("Binary Encoding Output (first 1000 binary):"), gbc);

        gbc.gridy = 4;
        gbc.weighty = 0.3;
        encodingOutput = new JTextArea(5, 40);
        encodingOutput.setEditable(false);
        // Disable lineWrap for massive performance gains on very large strings
        encodingOutput.setLineWrap(false);
        JScrollPane encodingScroll = new JScrollPane(encodingOutput);
        encodingPanel.add(encodingScroll, gbc);

        gbc.gridy = 5;
        gbc.weighty = 0.0;
        JPanel saveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton saveBtn = new JButton("Save to File");
        saveBtn.addActionListener(e -> saveToFile());
        saveButtonPanel.add(saveBtn);
        encodingPanel.add(saveButtonPanel, gbc);

        // Assembly
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.add(topPanel);
        mainContent.add(centerPanel);
        mainContent.add(encodingPanel);

        add(new JScrollPane(mainContent), BorderLayout.CENTER);
    }

    private void generateHuffmanTree() {
        String text = inputForTree.getText();
        if (text == null || text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter some text to generate a tree.");
            return;
        }

        try {
            currentTree = new HuffmanTree(text);
            displayTree();
            // Pop up the graphical visualization window in parallel
            if (!GraphicsEnvironment.isHeadless()) {
                HuffmanVisualizer.showTree(currentTree.getRoot());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating Huffman Tree: " + ex.getMessage());
        }
    }

    private void displayTree() {
        if (currentTree == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Character Codes:\n");
        Map<Character, String> codes = currentTree.getCodes();
        codes.forEach((c, code) -> {
            String charDisplay = c == '\n' ? "\\n" : c == '\r' ? "\\r" : c == '\t' ? "\\t" : String.valueOf(c);
            sb.append("'").append(charDisplay).append("': ").append(code).append("\n");
        });
        
        sb.append("\nVisual Tree Representation:\n");
        formatTree(currentTree.getRoot(), "", true, sb);
        
        treeDisplay.setText(sb.toString());
        treeDisplay.setCaretPosition(0);
    }

    private void formatTree(HuffmanNode node, String prefix, boolean isLeft, StringBuilder sb) {
        if (node == null) return;

        sb.append(prefix);
        sb.append(isLeft ? "├── " : "└── ");
        
        if (node.isLeaf()) {
            String charDisplay = node.character == '\n' ? "\\n" : node.character == '\r' ? "\\r" : node.character == '\t' ? "\\t" : String.valueOf(node.character);
            sb.append("'").append(charDisplay).append("' (").append(node.frequency).append(")\n");
        } else {
            sb.append("Internal (").append(node.frequency).append(")\n");
            formatTree(node.left, prefix + (isLeft ? "│   " : "    "), true, sb);
            formatTree(node.right, prefix + (isLeft ? "│   " : "    "), false, sb);
        }
    }

    private void encodeText() {
        if (currentTree == null) {
            JOptionPane.showMessageDialog(this, "Please generate a Huffman Tree first.");
            return;
        }

        String text = inputForEncoding.getText();
        if (text == null || text.isEmpty()) {
            text = inputForTree.getText();
            if (text != null && !text.isEmpty()) {
                inputForEncoding.setText(text);
                inputForEncoding.setCaretPosition(0);
            } else {
                updateEncodingOutput("");
                return;
            }
        }

        final String textToEncode = text;
        encodingOutput.setText("Encoding in progress, please wait...");
        
        // Using SwingWorker to perform encoding on a background thread
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return currentTree.encode(textToEncode);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    updateEncodingOutput(result);

                    // Compression Summary Calculation
                    int numChars = textToEncode.length();
                    int numBits = result.length();
                    int originalBytes = numChars;
                    int encodedBytes = (int) Math.ceil(numBits / 8.0);
                    
                    double originalBitsTotal = numChars * 8.0;
                    double improvement = ((originalBitsTotal - numBits) / originalBitsTotal) * 100.0;

                    String summary = String.format(
                        "Compression Summary:\n\n" +
                        "Original Characters: %d\n" +
                        "Original Size: %d bytes (%d bits)\n" +
                        "Encoded Bits: %d\n" +
                        "Encoded Size: %d bytes\n\n" +
                        "Efficiency Improvement: %.2f%%",
                        numChars, originalBytes, (int)originalBitsTotal, numBits, encodedBytes, improvement
                    );

                    JOptionPane.showMessageDialog(HuffmanApp.this, summary, "Compression Efficiency", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    encodingOutput.setText("Error during encoding: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void readFromFile() {
        JFileChooser fileChooser = createFileChooser("Select a Text (.txt) or Huffman (.huf) File");
        FileNameExtensionFilter combinedFilter = new FileNameExtensionFilter("Supported Files (*.txt, *.huf)", "txt", "huf");
        fileChooser.addChoosableFileFilter(combinedFilter);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Huffman Files (*.huf)", "huf"));
        fileChooser.setFileFilter(combinedFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists()) {
                JOptionPane.showMessageDialog(this, "Selected file does not exist.", "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            loadFile(selectedFile);
        }
    }

    private void loadFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) {
            loadTextFile(file);
        } else if (name.endsWith(".huf")) {
            loadHuffmanFile(file);
        } else {
            try {
                String sample = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                if (sample.startsWith("{") || sample.contains("[TREE_SOURCE_TEXT]") || sample.contains("[ENCODED_MESSAGE]")) {
                    loadHuffmanFile(file);
                } else {
                    loadTextFile(file);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadTextFile(File file) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String content = get();
                    inputForTree.setText(content);
                    inputForTree.setCaretPosition(0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HuffmanApp.this, "Error reading text file: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadHuffmanFile(File file) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<HuffmanFileData, Void> worker = new SwingWorker<HuffmanFileData, Void>() {
            @Override
            protected HuffmanFileData doInBackground() throws Exception {
                return HuffmanFileHandler.load(file);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    HuffmanFileData data = get();
                    applyHuffmanFileData(data);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HuffmanApp.this, "Error loading .huf file: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void readFromFileForEncoding() {
        JFileChooser fileChooser = createFileChooser("Select a Text (.txt) or Huffman (.huf) File");
        FileNameExtensionFilter combinedFilter = new FileNameExtensionFilter("Supported Files (*.txt, *.huf)", "txt", "huf");
        fileChooser.addChoosableFileFilter(combinedFilter);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Huffman Files (*.huf)", "huf"));
        fileChooser.setFileFilter(combinedFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists()) {
                JOptionPane.showMessageDialog(this, "Selected file does not exist.", "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            loadFileForEncoding(selectedFile);
        }
    }

    private void loadFileForEncoding(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) {
            loadTextFileForEncoding(file);
        } else if (name.endsWith(".huf")) {
            loadHuffmanFileForEncoding(file);
        } else {
            try {
                String sample = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                if (sample.startsWith("{") || sample.contains("[TREE_SOURCE_TEXT]") || sample.contains("[ENCODED_MESSAGE]")) {
                    loadHuffmanFileForEncoding(file);
                } else {
                    loadTextFileForEncoding(file);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadTextFileForEncoding(File file) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String content = get();
                    inputForEncoding.setText(content);
                    inputForEncoding.setCaretPosition(0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HuffmanApp.this, "Error reading text file: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadHuffmanFileForEncoding(File file) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                HuffmanFileData data = HuffmanFileHandler.load(file);
                String text = data.getTextToEncode();
                if (text == null || text.isEmpty()) {
                    text = data.getTreeSourceText();
                }
                return text;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String decodedText = get();
                    inputForEncoding.setText(decodedText != null ? decodedText : "");
                    inputForEncoding.setCaretPosition(0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HuffmanApp.this, "Error loading .huf file: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void applyHuffmanFileData(HuffmanFileData data) {
        if (data == null) return;

        // 1. Reconstruct Huffman Tree
        try {
            currentTree = HuffmanFileHandler.reconstructTree(data);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to reconstruct Huffman Tree: " + ex.getMessage(), "Reconstruction Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Put components in respective panes:
        // Pane 1: Enter Text to Create Huffman Tree
        if (data.getTreeSourceText() != null) {
            inputForTree.setText(data.getTreeSourceText());
            inputForTree.setCaretPosition(0);
        }

        // Pane 2: Huffman Tree (Codes)
        if (currentTree != null) {
            displayTree();
            if (!GraphicsEnvironment.isHeadless()) {
                HuffmanVisualizer.showTree(currentTree.getRoot());
            }
        }

        // Pane 3: Text to be Encoded
        String textToEncode = data.getTextToEncode();
        if (textToEncode == null && data.getEncodedMessage() != null && currentTree != null) {
            textToEncode = currentTree.decode(data.getEncodedMessage());
        }
        if (textToEncode != null) {
            inputForEncoding.setText(textToEncode);
            inputForEncoding.setCaretPosition(0);
        }

        // Pane 4: Binary Encoding Output (first 1000 binary)
        String encodedMessage = data.getEncodedMessage();
        if (encodedMessage == null && textToEncode != null && currentTree != null) {
            encodedMessage = currentTree.encode(textToEncode);
        }
        updateEncodingOutput(encodedMessage);
    }

    private void updateEncodingOutput(String fullMessage) {
        this.fullEncodedMessage = fullMessage != null ? fullMessage : "";
        if (this.fullEncodedMessage.isEmpty()) {
            encodingOutput.setText("");
        } else {
            int limit = Math.min(1000, this.fullEncodedMessage.length());
            encodingOutput.setText(this.fullEncodedMessage.substring(0, limit));
        }
        encodingOutput.setCaretPosition(0);
    }

    private void saveToFile() {
        if (currentTree == null) {
            JOptionPane.showMessageDialog(this, "Please generate or load a Huffman Tree first.", "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = inputForEncoding.getText();
        if (text == null || text.isEmpty()) {
            text = inputForTree.getText();
        }
        final String textToEncode = text;

        String encoded = fullEncodedMessage;
        if (encoded == null || encoded.trim().isEmpty()) {
            encoded = encodingOutput.getText();
        }
        if (encoded == null || encoded.trim().isEmpty()) {
            if (textToEncode != null && !textToEncode.trim().isEmpty()) {
                encoded = currentTree.encode(textToEncode);
                updateEncodingOutput(encoded);
            } else {
                JOptionPane.showMessageDialog(this, "Please encode some text before saving to a .huf file.", "Save Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        final String encodedMessage = encoded;

        JFileChooser fileChooser = createFileChooser("Save Huffman (.huf) File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Huffman Files (*.huf)", "huf"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            final File targetFile = selectedFile.getName().toLowerCase().endsWith(".huf")
                    ? selectedFile
                    : new File(selectedFile.getParentFile(), selectedFile.getName() + ".huf");

            if (targetFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "The file '" + targetFile.getName() + "' already exists.\nDo you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            String treeSource = inputForTree.getText();
            if (treeSource == null || treeSource.isEmpty()) {
                treeSource = currentTree.getSourceText();
            }

            HuffmanFileData data = new HuffmanFileData(
                treeSource,
                currentTree.getFrequencies(),
                currentTree.getCodes(),
                textToEncode,
                encodedMessage
            );

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<Void, Void> saveWorker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    HuffmanFileHandler.save(targetFile, data);
                    return null;
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        get();
                        long origBytes = textToEncode != null ? textToEncode.getBytes(StandardCharsets.UTF_8).length : 0;
                        long compBytes = targetFile.length();
                        double savings = origBytes > 0 ? ((origBytes - compBytes) * 100.0 / origBytes) : 0;
                        String msg = String.format(
                            "Huffman file saved successfully!\n\n" +
                            "File: %s\n" +
                            "Original size: %,d bytes\n" +
                            "Compressed size: %,d bytes\n" +
                            "Space saved: %.2f%%",
                            targetFile.getName(), origBytes, compBytes, savings
                        );
                        JOptionPane.showMessageDialog(HuffmanApp.this, msg, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(HuffmanApp.this, "Failed to save file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            saveWorker.execute();
        }
    }

    private JFileChooser createFileChooser(String title) {
        File defaultDir = new File("datafiles");
        if (!defaultDir.exists()) {
            defaultDir = new File(".");
        }
        JFileChooser chooser = new JFileChooser(defaultDir);
        chooser.setDialogTitle(title);
        return chooser;
    }
}
