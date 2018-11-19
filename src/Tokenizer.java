import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Tokenization provider.
 */
public class Tokenizer {

    /**
     * Tokenizes email messages of type Message
     * @param message email message to tokenize
     * @return the tokenized message
     */
    public static TokenizedMessage tokenize(Message message) {

        // New tokenized message with the existing message data.
        TokenizedMessage tkMessage = new TokenizedMessage(message);

        // Tokenize subject.
        tkMessage.setSubjectTokens(
                Arrays.asList(message.subject.split("\\s+")));

        // Tokenize body.
        List<String> bodyTokens = new ArrayList<String>();
        for (String line : message.body) {

            List<String> lineTokens = Arrays.asList(line.split("\\s+"));

            bodyTokens.addAll(lineTokens);
        }
        tkMessage.setBodyTokens(bodyTokens);

        return tkMessage;
    }
}
