import java.util.List;


/**
 * Tokenized message containing the original message
 * and the lists of tokens derived from it.
 */
public class TokenizedMessage extends Message {

    /**
     * List of tokens from tokenization of the message subject.
     */
    public List<String> subjectTokens;

    /**
     * List of tokens from tokenization of the message body.
     */
    public List<String> bodyTokens;

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
    }

    @Override
    public String toString() {
        return "TokenizedMessage{" +
                "subjectTokens=" + subjectTokens +
                ", bodyTokens=" + bodyTokens +
                ", FILE_NAME='" + FILE_NAME + '\'' +
                ", FILE_PATH='" + FILE_PATH + '\'' +
                ", ENCODING=" + ENCODING +
                ", subject='" + subject + '\'' +
                ", body=" + body +
                '}';
    }
}
