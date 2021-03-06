package classifier.utils;

import classifier.messagetypes.Message;
import classifier.messagetypes.TokenizedMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * Tokenization provider.
 */
public class Tokenizer {

    /**
     * Remove duplicate tokens by default.
     */
    private static boolean removeDups = true;


    //---------------------------------+
    //     PUBLIC STATCIC METHODS     /
    //-------------------------------+

    /**
     * Alternate method signature to allow disabling duplicate token removal.
     * @param message classifier.messagetypes.Message object to tokenize
     * @param removeDuplicates remove duplicates if true
     * @return a classifier.messagetypes.TokenizedMessage
     */
    public static TokenizedMessage tokenize(Message message, boolean removeDuplicates) {
        removeDups = removeDuplicates;
        return tokenize(message);
    }

    /**
     * Tokenizes messages of type classifier.messagetypes.Message
     * @param message message to tokenize
     * @return the tokenized message
     */
    public static TokenizedMessage tokenize(Message message) {

        // New tokenized message with existing message data.
        TokenizedMessage tkMessage = new TokenizedMessage(message);

        // Tokenize.
        List<String> subjectTokens;
        List<String> bodyTokens = new ArrayList<>();
        if (removeDups) {
            // Tokenize remove duplicate tokens.
            subjectTokens = new ArrayList<>(new HashSet<>(Arrays.asList(message.getSubject().split("\\s+"))));
            for (String line : message.getBody()) {
                List<String> lineTokens = new ArrayList<>(new HashSet<>(Arrays.asList(line.split("\\s+"))));
                bodyTokens.addAll(lineTokens);
            }
        } else {
            // Tokenize but don't remove duplicate tokens.
            subjectTokens = Arrays.asList(message.getSubject().split("\\s+"));
            for (String line : message.getBody()) {
                List<String> lineTokens = Arrays.asList(line.split("\\s+"));
                bodyTokens.addAll(lineTokens);
            }
        }

        tkMessage.setSubjectTokens(subjectTokens);
        tkMessage.setBodyTokens(bodyTokens);

        return tkMessage;
    }
}
