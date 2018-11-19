import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
            description = "A single path to a directory containing training and testing sets.")
    private Path inputPath;

    @Option(names = {"-a", "--algorithm"}, description = "KNN, NB, TODO EXPERIMENTAL...")
    private String algorithm = "KNN";

    // TODO Add option for setting k with the default of 3.
    @Option(names = {"-k", "--k"}, description = "Number of nearest neighbors - the K in KNN.")
    private int kforKNN = 3;

    // TODO Add option for training data location with default of data/train.

    // TODO Add option for test data location with default of data/test.

    /**
     * The set of parsed messages in training set.
     */
    private ArrayList<Message> trainMessages = new ArrayList<>();

    /**
     * The set of parsed messages in training set.
     */
    private ArrayList<Message> testMessages = new ArrayList<>();

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
            if (algorithm.equals("knn")) {
                System.out.println("K: " + kforKNN);
            }
        }

        // TODO Better place for this?
        Path trainPath = Paths.get(inputPath.toString(), "train");
        Path testPath = Paths.get(inputPath.toString(), "test");

        // If very verbose, print paths to all files in input path directory, also.
        if (verbose.length > 1) {

            int trainfileCount = new File(trainPath.toString()).list().length;
            System.out.println("Number of training messages: " + trainfileCount);

            System.out.println("Training messages: ");
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(trainPath)) {
                for (Path file: stream) {
                    System.out.println("    " + file.getFileName());
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(ex);
            }

            int testfileCount = new File(testPath.toString()).list().length;
            System.out.println("Number of test messages: " + trainfileCount);

            System.out.println("Test messages: ");
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(testPath)) {
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

        // Training data.

        // Get handle to directory, create message objects from files
        // and add to training messages list.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(trainPath)) {
            // Add messages to messages ArrayList.
            for (Path file: stream) {
                trainMessages.add(new Message(file, StandardCharsets.UTF_8));
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

        // Test data.

        // Get handle to directory, create message objects from files
        // and add to testing messages list.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testPath)) {
            // Add messages to messages ArrayList.
            for (Path file: stream) {
                testMessages.add(new Message(file, StandardCharsets.UTF_8));
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

        // Train

        List<TokenizedMessage> wrangledTrainMessages = runTheWranglePipeline(trainMessages);

        // Test

        List<TokenizedMessage> wrangledTestMessages = runTheWranglePipeline(testMessages);

        //----------------------------+
        //    LOAD THE CLASSIFIER    /
        //--------------------------+

        // KNN (for now, will encapsulate eventually...)
        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("knn")) {

            // Create instance of classifier and pass in training data set.
            ClassifierKNN knn = new ClassifierKNN(wrangledTrainMessages, kforKNN);

            //--------------------------------+
            //    CLASSIFY ALL THE THINGS    /
            //------------------------------+

            // Now run knn for test set.

            int totalNum = 0;
            int totalCorrect = 0;
            for (TokenizedMessage testMessage : wrangledTestMessages) {
                // Predict label of test message.
                boolean label = knn.predict(testMessage);
                // DEBUG
                //System.out.println("Predicted label: " + label);
                //System.out.println("Actual label: " + testMessage.isSpam());

                totalNum++;
                if (label == testMessage.isSpam()) { totalCorrect++; }
            }

            // DEBUG
            System.out.println("totalCorrect: " + totalCorrect);
            System.out.println("totalNum: " + totalNum);
            System.out.println("Accuracy: " + (totalCorrect/ (double) totalNum));

            //---------------------------------------------------+
            //    TODO PRODUCE / RETURN REPORT OF THE THINGS    /
            //-------------------------------------------------+

        }

        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("nb")) {

            // TODO NB goes here for now.
            System.out.println("TODO NB");

        }
    }

    // TODO Not a serious method name! Will be refactored away.
    private ArrayList<TokenizedMessage> runTheWranglePipeline(ArrayList<Message> messages) {

        // Create empty list to store wrangled messages.
        ArrayList<TokenizedMessage> wrangledMessages = new ArrayList<>();

        //==================+
        //     Tokenize     |
        //==================+

        // Populate ArrayList with tokenized messages.
        for (Message message : messages) {
            wrangledMessages.add(Tokenizer.tokenize(message));
        }

        // DEBUG
        //for (TokenizedMessage wrMessage : wrangledMessages) {
        //    System.out.println(wrMessage);
        //}

        //===================+
        //     Normalize     |
        //===================+

        // - Covert to lowercase.
        for (TokenizedMessage tkMessage : wrangledMessages) {
            tkMessage.setSubjectTokens(tkMessage.getSubjectTokens().stream()
                    .map(String::toLowerCase)
                    .collect(toList()));

            tkMessage.setBodyTokens(tkMessage.getBodyTokens().stream()
                    .map(String::toLowerCase)
                    .collect(toList()));
        }

        // DEBUG
        //for (TokenizedMessage wrMessage : wrangledMessages) {
        //    System.out.println(wrMessage);
        //}

        //===========================+
        //     Remove Stop Words     |
        //===========================+

        // TODO Refer to list of top stop words?
        String[] defaultStopWords = {"i", "a", "about", "an",
                "are", "as", "at", "be", "by", "com", "for", "from", "how",
                "in", "is", "it", "of", "on", "or", "that", "the", "this",
                "to", "was", "what", "when", "where", "who", "will", "with"};
        Set stopWords = new HashSet<>(Arrays.asList(defaultStopWords));

        // TODO More efficient way to do this?
        for (TokenizedMessage tkMessage : wrangledMessages) {

            // Get Lists of tokens.
            List<String> subjectTokens = tkMessage.getSubjectTokens();
            List<String> bodyTokens = tkMessage.getBodyTokens();

            int count = 0;
            for (int i = 0; i < subjectTokens.size(); i++) {

                if (stopWords.contains(tkMessage.getSubjectTokens().get(i))) {
                    count++;
                    // DEBUG
                    //System.out.println("REMOVING: " + subjectTokens.get(i));
                    subjectTokens.remove(i);
                }
            }
            // Replace with pared down list.
            tkMessage.setSubjectTokens(subjectTokens);
            // DEBUG
            //System.out.println("Removed " + count + " stop words from subject.");

            count = 0;
            for (int i = 0; i < bodyTokens.size(); i++) {

                if (stopWords.contains(bodyTokens.get(i))) {
                    count++;
                    // DEBUG
                    //System.out.println("REMOVING: " + bodyTokens.get(i));
                    bodyTokens.remove(i);
                }
            }
            // Replace with pared down list.
            tkMessage.setBodyTokens(bodyTokens);
            // DEBUG
            //System.out.println("Removed " + count + " stop words from body.");
        }

        return wrangledMessages;
    }
}
