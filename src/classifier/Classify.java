package classifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import classifier.classifiers.core.KNN;
import classifier.classifiers.experimental.DocumentCategorizer;
import classifier.messagetypes.Message;
import classifier.messagetypes.TokenizedMessage;
import classifier.utils.Tokenizer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static java.util.stream.Collectors.toList;


@Command(name = "classifier.Classify", mixinStandardHelpOptions = true,
        version = "Email Classifier 0.1.1")
public class Classify implements Runnable {

    @Option(names = { "-s", "--stopwords" },
        description = "Remove stopwords from messages during tokens wrangling.")
    private boolean removeStopWords = false;

    @Option(names = { "-v", "--verbose" },
            description = "Verbose mode. Multiple -v options increase the verbosity.")
    private boolean[] verbose = new boolean[0];

    @Option(names = {"-a", "--algorithm"}, description = "KNN, NB, DC")
    private String algorithm = "knn";

    // TODO Add options for setting OpenNLP DocumentCategorizerME parameters.

    // TODO Add option for running cross-validation.

    @Option(names = {"-k", "--k"}, description = "Number of nearest neighbors - the K in KNN.")
    private int kforKNN = 3;

    @Option(names = {"--trainPath"}, description = "Path within data folder to training data.")
    private String trainDataPath = "train";

    @Option(names = {"--testPath"}, description = "Path within data folder to test data.")
    private String testDataPath = "test";

    @Parameters(arity = "1", paramLabel = "PATH",
            description = "A single path to a directory containing training and testing sets.")
    private Path inputPath;

    /**
     * The full path to the training data,
     * which is assembled from arguments,
     * or defaults to ./data/train.
     */
    private Path trainFullPath;

    /**
     * The full path to the training data,
     * which is assembled from arguments,
     * or defaults to ./data/test.
     */
    private Path testFullPath;

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

    /**
     * The main point of entry for the application.
     * @param args arguments
     */
    public static void main(String[] args) {
        CommandLine.run(new Classify(), args);
    }

    @Override
    public void run() {

        //-------------------------+
        //    SET UP THE SETUP    /
        //-----------------------+

        // TODO Clean this up and add optional params to pass in paths.
        trainFullPath = Paths.get(inputPath.toString(), trainDataPath);
        testFullPath = Paths.get(inputPath.toString(), testDataPath);


        //------------------------------------+
        //    DO VERBOSE THINGS IF NEEDED    /
        //----------------------------------+

        printVerboseHeader();


        //---------------------+
        //    GET THE DATA    /
        //-------------------+

        // Training data.
        trainMessages = loadData(trainFullPath);

        // Testing data.
        testMessages = loadData(testFullPath);


        //-------------------------+
        //    WRANGLE THE DATA    /
        //-----------------------+

        // Get wrangled training set of messages.
        List<TokenizedMessage> wrangledTrainMessages = runTheWranglePipeline(trainMessages);

        // get wrangled test set of messages.
        List<TokenizedMessage> wrangledTestMessages = runTheWranglePipeline(testMessages);


        //--------------------------------+
        //    CLASSIFY ALL THE THINGS    /
        //------------------------------+

        // KNN
        if (algorithm.toLowerCase().equals("knn")) {
            // Create auto-trained instance of the KNN classifer, then
            // classify all the things.
            KNN knn = new KNN(wrangledTrainMessages, kforKNN);
            knn.predictDataSet(wrangledTestMessages);
        }

        // Apache OpenNLP Document Categorizer.
        if (algorithm.toLowerCase().equals("dc")) {
            // Create the auto-trained instance of the categorizer, then
            // classify all the things.
            DocumentCategorizer dc = new DocumentCategorizer(wrangledTrainMessages);
            dc.predictDataSet(wrangledTestMessages);
        }

        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("nb")) {
            NaiveBayes nb = new NaiveBayes();

            wrangledTrainMessages.forEach(nb::train);
            wrangledTestMessages.forEach(nb::test);
        }
    }

    /**
     * Prints verbose header.
     */
    private void printVerboseHeader() {

        if (verbose.length > 0) {
            System.out.println("Input path: " + inputPath.toString());
            System.out.println("Algorithm: " + algorithm);
            if (algorithm.equals("knn")) {
                System.out.println("K: " + kforKNN);
            }
        }

        // If very verbose, print paths to all files in input path directory, also.
        if (verbose.length > 1) {

            int trainfileCount = new File(trainFullPath.toString()).list().length;
            System.out.println("Number of training messages: " + trainfileCount);

            System.out.println("Training messages: ");
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(trainFullPath)) {
                for (Path file: stream) {
                    System.out.println("    " + file.getFileName());
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(ex);
            }

            int testfileCount = new File(testFullPath.toString()).list().length;
            System.out.println("Number of test messages: " + testfileCount);

            System.out.println("Test messages: ");
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(testFullPath)) {
                for (Path file: stream) {
                    System.out.println("    " + file.getFileName());
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(ex);
            }
        }
    }

    /**
     * Wrangle messages:
     *  - tokenize
     *  - normalize
     *  - remove stop words
     * @param messages list of messages
     * @return list of wrangled messages
     */
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
        //for (classifier.messagetypes.TokenizedMessage wrMessage : wrangledMessages) {
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
        //for (classifier.messagetypes.TokenizedMessage wrMessage : wrangledMessages) {
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
                    if (removeStopWords) subjectTokens.remove(i);
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
                    if (removeStopWords) bodyTokens.remove(i);
                }
            }
            // Replace with pared down list.
            tkMessage.setBodyTokens(bodyTokens);
            // DEBUG
            //System.out.println("Removed " + count + " stop words from body.");
        }


        //=======================+
        //     TODO Stemming     |
        //=======================+


        //=============================+
        //     TODO TERM WEIGHTING     |
        //=============================+

        return wrangledMessages;
    }

    /**
     * Create list of classifier.messagetypes.Message objects from directory of text files.
     * @param fullPathToData full path to the directory
     * @return list of classifier.messagetypes.Message objects
     */
    private ArrayList<Message> loadData(Path fullPathToData) {
        // Create list to store messages.
        ArrayList<Message> messages = new ArrayList<>();

        // Get handle to directory, create message objects from files
        // and add to training messages list.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fullPathToData)) {
            // Add messages to messages ArrayList.
            for (Path file: stream) {
                messages.add(new Message(file, StandardCharsets.UTF_8));
            }

            // DEBUG
            //for (classifier.messagetypes.Message message : messages) {
            //    System.out.println(message);
            //}
        } catch (IOException | DirectoryIteratorException ex) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(ex);
        }

        return messages;
    }
}
