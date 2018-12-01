package classifier.messagetypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * Tokenized message containing the original message
 * and the lists of tokens derived from it.
 */
public class TokenizedMessage extends Message {

    /**
     * List of tokens from tokenization of the message subject.
     */
    private List<String> subjectTokens;

    /**
     * List of tokens from tokenization of the message body.
     */
    private List<String> bodyTokens;

    //-----------------------+
    //     CONSTRUCTORS     /
    //---------------------+

    /**
     * Constructor
     * @param message email message
     */
    public TokenizedMessage(Message message) {
        this.FILE_NAME = message.FILE_NAME;
        this.FILE_PATH = message.FILE_PATH;
        this.ENCODING = message.ENCODING;
        this.subject = message.subject;
        this.body = message.body;
        this.spam = message.spam;
    }


    //-------------------------+
    //     PUBLIC METHODS     /
    //-----------------------+

    /**
     * Gets all tokens for message in one list.
     * @return all tokens for message
     */
    public List<String> getAllTokens() {
        // New list with subject tokens, then add all body tokens.
        List<String> allTokens = new ArrayList<>(this.subjectTokens);
        allTokens.addAll(this.bodyTokens);

        // Remove duplicates.
        allTokens = new ArrayList<>(new HashSet<>(allTokens));

        return allTokens;
    }

    /**
     * Print formatted list of all tokens for message.
     */
    public void printAllTokens() {

        List<String> allTokens = this.getAllTokens();

        System.out.println("========================================");
        System.out.println("= TOKENS FOR: " + this.getFILE_NAME());
        System.out.println("=");
        int numTokens = 0;
        for (String token : allTokens) {
            System.out.println(token);
            numTokens++;
        }
        System.out.println("=");
        System.out.println("= NUM TOKENS: " + numTokens);
        System.out.println("========================================");
    }

    @Override
    public String toString() {
        return "classifier.messagetypes.TokenizedMessage{" +
                "subjectTokens=" + subjectTokens +
                ", bodyTokens=" + bodyTokens +
                ", FILE_NAME='" + FILE_NAME + '\'' +
                ", FILE_PATH='" + FILE_PATH + '\'' +
                ", ENCODING=" + ENCODING +
                ", subject='" + subject + '\'' +
                ", body=" + body + '\'' +
                ", spam=" +
                '}';
    }


    //--------------------------+
    //     PRIVATE METHODS     /
    //------------------------+


    //----------------------------+
    //     GETTERS & SETTERS     /
    //--------------------------+

    public List<String> getSubjectTokens() {
        return subjectTokens;
    }

    public void setSubjectTokens(List<String> subjectTokens) {
        this.subjectTokens = subjectTokens;
    }

    public List<String> getBodyTokens() {
        return bodyTokens;
    }

    public void setBodyTokens(List<String> bodyTokens) {
        this.bodyTokens = bodyTokens;
    }

}
