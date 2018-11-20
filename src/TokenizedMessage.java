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

    @Override
    public String toString() {
        return "TokenizedMessage{" +
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
