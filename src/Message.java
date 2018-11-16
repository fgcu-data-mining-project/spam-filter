import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Message containing the data and metadata from
 * a parsed email message.
 */
public class Message {

    /**
     * Name of file.
     */
    public String FILE_NAME;

    /**
     * Absolute path to file.
     */
    public String FILE_PATH;

    /**
     * Encoding of file.
     */
    public Charset ENCODING;

    /**
     * The subject line of the message.
     */
    public String subject;

    /**
     * The body of the message.
     */
    public List<String> body;

    /**
     * Constructor for empty object.
     */
    public Message() { }

    /**
     * Constructor.
     * @param message path to the message to be parsed
     */
    public Message(Path message, Charset encoding) {

        // Set metadata.
        FILE_NAME = message.getFileName().toString();
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

    @Override
    public String toString() {
        return "Message{" +
                "FILE_NAME='" + FILE_NAME + '\'' +
                ", FILE_PATH='" + FILE_PATH + '\'' +
                ", ENCODING=" + ENCODING +
                ", subject='" + subject + '\'' +
                ", body=" + body +
                '}';
    }
}
