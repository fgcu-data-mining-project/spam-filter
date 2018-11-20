import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Tokenization provider.
 */
public class Tokenizer {

    //---------------------------------+
    //     PUBLIC STATCIC METHODS     /
    //-------------------------------+

    /**
     * Tokenizes email messages of type Message
     * @param message email message to tokenize
     * @return the tokenized message
     */
    public static TokenizedMessage tokenize(Message message) {

        // TODO Move removal of duplicate tokens here?

        // New tokenized message with existing message data.
        TokenizedMessage tkMessage = new TokenizedMessage(message);

        // Tokenize subject.
        tkMessage.setSubjectTokens(
                Arrays.asList(message.subject.split("\\s+")));

        // Tokenize body.
        List<String> bodyTokens = new ArrayList<>();
        for (String line : message.body) {
            List<String> lineTokens = Arrays.asList(line.split("\\s+"));
            bodyTokens.addAll(lineTokens);
        }
        tkMessage.setBodyTokens(bodyTokens);

        return tkMessage;
    }
}
