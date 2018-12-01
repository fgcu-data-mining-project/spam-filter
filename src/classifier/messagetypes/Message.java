package classifier.messagetypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


/**
 * classifier.messagetypes.Message containing the data and metadata from
 * a parsed email message.
 */
public class Message {

    /**
     * Name of file.
     */
    protected String FILE_NAME;

    /**
     * Absolute path to file.
     */
    protected String FILE_PATH;

    /**
     * Encoding of file.
     */
    protected Charset ENCODING;

    /**
     * The subject line of the message.
     */
    protected String subject;

    /**
     * The body of the message.
     */
    protected List<String> body;

    /**
     * True if labeled as spam.
     */
    protected boolean spam = false;


    //-----------------------+
    //     CONSTRUCTORS     /
    //---------------------+

    /**
     * Constructor for empty object.
     */
    public Message() { }

    /**
     * Constructor.
     * @param message path to the message to be parsed
     * @param encoding encoding for a BufferedReader instance
     */
    public Message(Path message, Charset encoding) {

        // Set metadata.
        FILE_NAME = message.getFileName().toString();

        // Extract label from filename.
        // If the first letter is 's', it's spam.
        // TODO This is tightly coupled to this particular set of data.
        if (FILE_NAME.charAt(0) == 's') {
            spam = true;
        }

        FILE_PATH = message.toAbsolutePath().toString();
        ENCODING = encoding;

        // TODO Move parsing out of constructor?
        // Parse out subject line.
        // Everything from "Subject: " up to first newline char is subject.
        try (BufferedReader reader = Files.newBufferedReader(message, ENCODING)){

            // Get entire text by collecting stream to list.
            body = reader.lines().collect(Collectors.toList());

            // Get first line as subject, remove label.
            // TODO Right place to remove subject label?
            subject = body.remove(0).replace("Subject: ", "");

            // Remove empty line between subject and body.
            body.remove(0);

        } catch (IOException ex) {
            // TODO Handle exception.
            ex.printStackTrace();
        }
    }


    //-------------------------+
    //     PUBLIC METHODS     /
    //-----------------------+

    @Override
    public String toString() {
        return "classifier.messagetypes.Message{" +
                "FILE_NAME='" + FILE_NAME + '\'' +
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

    public String getFILE_NAME() {
        return FILE_NAME;
    }

    public String getFILE_PATH() {
        return FILE_PATH;
    }

    public Charset getENCODING() {
        return ENCODING;
    }

    public String getSubject() {
        return subject;
    }

    public List<String> getBody() {
        return body;
    }

    public boolean isSpam() {
        return spam;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(List<String> body) {
        this.body = body;
    }

    public void setSpam(boolean isSpam) {
        this.spam = isSpam;
    }

}
