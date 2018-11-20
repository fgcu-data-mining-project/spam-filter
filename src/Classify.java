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

    @Option(names = {"-a", "--algorithm"}, description = "KNN, NB, TODO...")
    private String algorithm = "knn";

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
            // TODO Move all the reporting into the ClassifierKNN class where it belongs.

            int totalNum = 0;
            int numPredictedTrue = 0;
            int numActualTrue = 0;
            int numPredictedFalse = 0;
            int numActualFalse = 0;
            int totalCorrect = 0;
            int totalIncorrect = 0;
            int numTP = 0;
            int numTN = 0;
            int numFP = 0;
            int numFN = 0;

            double nullErrorRate = 0;
            for (TokenizedMessage testMessage : wrangledTestMessages) {
                // Predict label of test message.
                boolean label = knn.predict(testMessage);

                // Count total number of messages classified.
                totalNum++;

                // Count numbers of predictions.
                if (label) {
                    numPredictedTrue++;
                } else {
                    numPredictedFalse++;
                }

                // Count actual labels.
                if (testMessage.isSpam()) {
                    numActualTrue++;
                } else {
                    numActualFalse++;
                }

                // Count correct/incorrect predictions,
                // and catch TP, TN, FP, FN while at it.
                if (label == testMessage.isSpam()) {
                    totalCorrect++;

                    if (label) {
                        numTP++;
                    } else {
                        numTN++;
                    }
                } else {
                    totalIncorrect++;

                    if (label) {
                        numFP++;
                    } else {
                        numFN++;
                    }
                }
            }

            // Calculate null error rate.
            String majClass = null;
            if (numActualTrue > numActualFalse) {
                // spam = true is the majority class.
                majClass = "true";
                nullErrorRate = numActualFalse / (double) totalNum;

            } else {
                // spam = false is the majority class.
                majClass = "false";
                nullErrorRate = numActualTrue / (double) totalNum;
            }

            //-------------------------------------+
            //    PRODUCE REPORT OF THE THINGS    /
            //-----------------------------------+

            // Print confusion matrix.
            System.out.println("CONFUSION MATRIX");
            System.out.println("================");
            System.out.println();
            System.out.println(String.format("%-8s   %8s   %8s", "", "Spam", "Not Spam"));
            System.out.println(String.format("%-8s | %8s | %8s |" , "Spam", "TP " + numTP, "FP " + numFP));
            System.out.println(String.format("%-8s | %8s | %8s |" , "Not Spam", "FN " + numFN, "TN " + numTN));

            System.out.println();

            System.out.println("STATISTICS");
            System.out.println("==========");
            System.out.println();
            System.out.println(String.format("%-25s %d", "Messages Classified: ", totalNum));
            System.out.println(String.format("%-25s %d", "Correct Predictions: ", totalCorrect));
            System.out.println(String.format("%-25s %d", "Incorrect Predictions: ", totalIncorrect));
            System.out.println(String.format("%-25s %f", "Accuracy: ", (totalCorrect/ (double) totalNum)));
            System.out.println(String.format("%-25s %f", "Misclassification: ", totalIncorrect / (double) totalNum));
            System.out.println(String.format("%-25s %f", "Precision: ", numActualTrue / (double) numPredictedTrue));
            System.out.println(String.format("%-25s %f", "Recall: ", numTP / (double) (numTP + numFN)));
            System.out.println(String.format("%-25s %f", "Null Error Rate (" + majClass + "): ", nullErrorRate));

        }

        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("nb")) {

            // TODO NB goes here for now.
            System.out.println("TODO NB");

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

    /**
     * Create list of Message objects from directory of text files.
     * @param fullPathToData full path to the directory
     * @return list of Message objects
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
            //for (Message message : messages) {
            //    System.out.println(message);
            //}
        } catch (IOException | DirectoryIteratorException ex) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(ex);
        }

        return messages;
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

}
