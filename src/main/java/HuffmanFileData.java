import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Data model representing the serialized state of a Huffman file (.huf).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HuffmanFileData {
    private String treeSourceText;
    private Map<Character, Integer> characterFrequencies;
    private Map<Character, String> characterCodes;
    private String textToEncode;
    private String encodedMessage;

    public HuffmanFileData() {
    }

    public HuffmanFileData(String treeSourceText,
                           Map<Character, Integer> characterFrequencies,
                           Map<Character, String> characterCodes,
                           String textToEncode,
                           String encodedMessage) {
        this.treeSourceText = treeSourceText;
        this.characterFrequencies = characterFrequencies;
        this.characterCodes = characterCodes;
        this.textToEncode = textToEncode;
        this.encodedMessage = encodedMessage;
    }

    public String getTreeSourceText() {
        return treeSourceText;
    }

    public void setTreeSourceText(String treeSourceText) {
        this.treeSourceText = treeSourceText;
    }

    public Map<Character, Integer> getCharacterFrequencies() {
        return characterFrequencies;
    }

    public void setCharacterFrequencies(Map<Character, Integer> characterFrequencies) {
        this.characterFrequencies = characterFrequencies;
    }

    public Map<Character, String> getCharacterCodes() {
        return characterCodes;
    }

    public void setCharacterCodes(Map<Character, String> characterCodes) {
        this.characterCodes = characterCodes;
    }

    public String getTextToEncode() {
        return textToEncode;
    }

    public void setTextToEncode(String textToEncode) {
        this.textToEncode = textToEncode;
    }

    public String getEncodedMessage() {
        return encodedMessage;
    }

    public void setEncodedMessage(String encodedMessage) {
        this.encodedMessage = encodedMessage;
    }
}
