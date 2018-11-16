import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static java.util.stream.Collectors.toList;


@Command(name = "Classify", mixinStandardHelpOptions = true,
        version = "Email Classifier 0.1.1")
public class Classify implements Runnable {

    @Option(names = { "-v", "--verbose" },
            description = "Verbose mode. Multiple -v options increase the verbosity.")
    private boolean[] verbose = new boolean[0];

    @Parameters(arity = "1", paramLabel = "PATH",
            description = "A single path to a directory of emails to classify.")
    private Path inputPath;

    @Option(names = {"-a", "--algorithm"}, description = "KNN, NB, TODO EXPERIMENTAL...")
    private String algorithm = "KNN";

    // TODO Add option for setting k with the default of 2.
    @Option(names = {"-k", "--k"}, description = "Number of nearest neighbors - the K in KNN.")
    private int kforKNN = 2;

    /**
     * The set of parsed messages.
     */
    private ArrayList<Message> messages = new ArrayList<>();

    /**
     * The set of tokenized messages.
     */
    private ArrayList<TokenizedMessage> tokenizedMessages = new ArrayList<>();

    public static void main(String[] args) {
        CommandLine.run(new Classify(), args);
    }

    @Override
    public void run() {

        //------------------------------------+
        //    DO VERBOSE THINGS IF NEEDED    /
        //----------------------------------+

        // If verbose, print input path.
        if (verbose.length > 0) {
            System.out.println("Input path: " + inputPath.toString());
            System.out.println("Algorithm: " + algorithm);
            if (algorithm.equals("KNN")) {
                System.out.println("K: " + kforKNN);
            }
        }

        // If very verbose, print paths to all files in input path directory, also.
        if (verbose.length > 1) {

            int fileCount = new File(inputPath.toString()).list().length;
            System.out.println("Number of messages to classify: " + fileCount);

            System.out.println("Messages: ");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
                for (Path file: stream) {
                    System.out.println("    " + file.getFileName());
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(ex);
            }
        }

        //---------------------+
        //    GET THE DATA    /
        //-------------------+

        // Get handle to directory, create message objects from files
        // and add to messages list.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
            // Add messages to messages ArrayList.
            for (Path file: stream) {
                messages.add(new Message(file, StandardCharsets.UTF_8));
            }

            // DEBUG
            //for (Message message : messages) {
            //    System.out.println(message);
            //}
        } catch (IOException | DirectoryIteratorException ex) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(ex);
        }

        //-------------------------+
        //    WRANGLE THE DATA    /
        //-----------------------+

        // Tokenize.
        // Populate tokenizedMessages ArrayList with tokenized messages.
        for (Message message : messages) {
            tokenizedMessages.add(Tokenizer.tokenize(message));
        }

        // DEBUG
        //for (TokenizedMessage tkMessage : tokenizedMessages) {
        //    System.out.println(tkMessage);
        //}

        // Normalize:
        // - Covert to lowercase.
        // - Remove stop words.
        for (TokenizedMessage tkMessage : tokenizedMessages) {
            tkMessage.subjectTokens = tkMessage.subjectTokens.stream()
                    .map(String::toLowerCase)
                    .collect(toList());

            tkMessage.bodyTokens = tkMessage.bodyTokens.stream()
                    .map(String::toLowerCase)
                    .collect(toList());
        }

        // DEBUG
        //for (TokenizedMessage tkMessage : tokenizedMessages) {
        //    System.out.println(tkMessage);
        //}

        // TODO Remove stopwords.

        // TODO Refer to list of top stop words?
        String[] defaultStopWords = {"i", "a", "about", "an",
                "are", "as", "at", "be", "by", "com", "for", "from", "how",
                "in", "is", "it", "of", "on", "or", "that", "the", "this",
                "to", "was", "what", "when", "where", "who", "will", "with"};
        Set stopWords = new HashSet<>(Arrays.asList(defaultStopWords));

        // TODO More efficient way to do this?
        for (TokenizedMessage tkMessage : tokenizedMessages) {

            int count = 0;
            for (int i = 0; i < tkMessage.subjectTokens.size(); i++) {

                if (stopWords.contains(tkMessage.subjectTokens.get(i))) {
                    count++;
                    // DEBUG
                    //System.out.println("REMOVING: " + tkMessage.subjectTokens.get(i));
                    tkMessage.subjectTokens.remove(i);
                }
            }
            // DEBUG
            //System.out.println("Remove " + count + " stopwords from subject.");

            count = 0;
            for (int i = 0; i < tkMessage.bodyTokens.size(); i++) {

                if (stopWords.contains(tkMessage.bodyTokens.get(i))) {
                    count++;
                    // DEBUG
                    //System.out.println("REMOVING: " + tkMessage.bodyTokens.get(i));
                    tkMessage.bodyTokens.remove(i);
                }
            }
            // DEBUG
            //System.out.println("Remove " + count + " stopwords from body.");
        }


        //---------------------------------+
        //    TODO LOAD THE CLASSIFIER    /
        //-------------------------------+


        //-------------------------------------+
        //    TODO CLASSIFY ALL THE THINGS    /
        //-----------------------------------+


        //---------------------------------------------------+
        //    TODO PRODUCE / RETURN REPORT OF THE THINGS    /
        //-------------------------------------------------+

    }
}
