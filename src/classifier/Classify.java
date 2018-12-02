package classifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import classifier.classifiers.core.KNN;
import classifier.classifiers.experimental.ClassifierDocumentCategorizer;
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
            KNN knn = new KNN(wrangledTrainMessages, kforKNN);

            //--------------------------------+
            //    CLASSIFY ALL THE THINGS    /
            //------------------------------+

            // Now run knn for test set.
            // TODO Move all the reporting into the classifier.classifiers.core.KNN class where it belongs.

            int totalNum = 0;
            int numActualTrue = 0;
            int numActualFalse = 0;
            int numTP = 0;
            int numTN = 0;
            int numFP = 0;
            int numFN = 0;
            double nullErrorRate = 0;

            System.out.println("============================================");
            for (TokenizedMessage tkTestkMsg : wrangledTestMessages) {
                // Predict label of test message.
                boolean label = knn.predict(tkTestkMsg);

                // Count total number of messages classified.
                totalNum++;

                // Count actual labels.
                if (tkTestkMsg.isSpam()) {
                    numActualTrue++;
                } else {
                    numActualFalse++;
                }

                String isCorrect = null;

                // Count correct/incorrect predictions,
                // and catch TP, TN, FP, FN while at it.
                if (label == tkTestkMsg.isSpam()) {
                    isCorrect = "correct";

                    if (label) {
                        numTP++;
                    } else {
                        numTN++;
                    }
                } else {
                    isCorrect = "INCORRECT";

                    if (label) {
                        numFP++;
                    } else {
                        numFN++;
                    }
                }

                // Print stats.
                System.out.print(String.format("| %-16s | %8s | %10s |\n", tkTestkMsg.getFILE_NAME(), label, isCorrect));
            }
            System.out.println("============================================\n");

            // Calculate null error rate.
            String majClass = null;
            if (numActualTrue > numActualFalse) {
                // spam = true is the majority class.
                majClass = "true";
                nullErrorRate = numActualTrue / (double) totalNum;

            } else {
                // spam = false is the majority class.
                majClass = "false";
                nullErrorRate = numActualFalse / (double) totalNum;
            }

            //-------------------------------------+
            //    PRODUCE REPORT OF THE THINGS    /
            //-----------------------------------+

            // Print confusion matrix.
            System.out.println("CONFUSION MATRIX");
            System.out.println("================");
            System.out.println();
            System.out.println(String.format("  %-8s   %8s   %8s", "", "Spam", "Not Spam"));
            System.out.println("==================================");
            System.out.println(String.format("| %-8s | %8s | %8s |" , "Spam", "TP " + numTP, "FP " + numFP));
            System.out.println("+================================+");
            System.out.println(String.format("| %-8s | %8s | %8s |" , "Not Spam", "FN " + numFN, "TN " + numTN));
            System.out.println("==================================");

            System.out.println();

            System.out.println("STATISTICS");
            System.out.println("==========");
            System.out.println();
            System.out.println(String.format("%-25s %d", "Messages Classified: ", totalNum));
            System.out.println(String.format("%-25s %d", "Correct Predictions: ", (numTP + numTN)));
            System.out.println(String.format("%-25s %d", "Incorrect Predictions: ", (numFP + numFN)));
            System.out.println(String.format("%-25s %f", "Accuracy: ", ((numTP + numTN) / (double) totalNum)));
            System.out.println(String.format("%-25s %f", "Misclassification: ", (numFP + numFN) / (double) totalNum));
            System.out.println(String.format("%-25s %f", "Precision: ", numTP / (double) (numTP + numFP)));
            System.out.println(String.format("%-25s %f", "Recall: ", numTP / (double) (numTP + numFN)));
            System.out.println(String.format("%-25s %f", "Null Error Rate (Majority " + majClass + "): ", nullErrorRate));

        }

        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("dc")) {
            // Create the auto-trained instance of the categorizer.
            ClassifierDocumentCategorizer dc = new ClassifierDocumentCategorizer(wrangledTrainMessages);

            // Classify test messages.
            // Counts.
            int totalNum = 0;
            int numActualTrue = 0;
            int numActualFalse = 0;
            int numTP = 0;
            int numTN = 0;
            int numFP = 0;
            int numFN = 0;
            double nullErrorRate = 0;

            // Classify test messages.
            System.out.println("============================================");
            for (TokenizedMessage tkTestkMsg : wrangledTestMessages) {

                boolean label = dc.predict(tkTestkMsg);

                // Count total number of messages classified.
                totalNum++;

                // Count actual labels.
                if (tkTestkMsg.isSpam()) {
                    numActualTrue++;
                } else {
                    numActualFalse++;
                }

                String isCorrect = null;

                // Count correct/incorrect predictions,
                // and catch TP, TN, FP, FN while at it.
                if (label == tkTestkMsg.isSpam()) {
                    isCorrect = "correct";

                    if (label) {
                        numTP++;
                    } else {
                        numTN++;
                    }
                } else {
                    isCorrect = "INCORRECT";

                    if (label) {
                        numFP++;
                    } else {
                        numFN++;
                    }
                }

                // Print stats.
                System.out.print(String.format("| %-16s | %8s | %10s |\n", tkTestkMsg.getFILE_NAME(), label, isCorrect));
                //System.out.println(categoryOutcomes);
            }

            // Calculate null error rate.
            String majClass = null;
            if (numActualTrue > numActualFalse) {
                // spam = true is the majority class.
                majClass = "true";
                nullErrorRate = numActualTrue / (double) totalNum;

            } else {
                // spam = false is the majority class.
                majClass = "false";
                nullErrorRate = numActualFalse / (double) totalNum;
            }

            //-------------------------------------+
            //    PRODUCE REPORT OF THE THINGS    /
            //-----------------------------------+

            System.out.println("============================================\n");

            // Print confusion matrix.
            System.out.println("CONFUSION MATRIX");
            System.out.println("================");
            System.out.println();
            System.out.println(String.format("  %-8s   %8s   %8s", "", "Spam", "Not Spam"));
            System.out.println("==================================");
            System.out.println(String.format("| %-8s | %8s | %8s |" , "Spam", "TP " + numTP, "FP " + numFP));
            System.out.println("+================================+");
            System.out.println(String.format("| %-8s | %8s | %8s |" , "Not Spam", "FN " + numFN, "TN " + numTN));
            System.out.println("==================================");

            System.out.println();

            System.out.println("STATISTICS");
            System.out.println("==========");
            System.out.println();
            System.out.println(String.format("%-25s %d", "Messages Classified: ", totalNum));
            System.out.println(String.format("%-25s %d", "Correct Predictions: ", (numTP + numTN)));
            System.out.println(String.format("%-25s %d", "Incorrect Predictions: ", (numFP + numFN)));
            System.out.println(String.format("%-25s %f", "Accuracy: ", ((numTP + numTN) / (double) totalNum)));
            System.out.println(String.format("%-25s %f", "Misclassification: ", (numFP + numFN) / (double) totalNum));
            System.out.println(String.format("%-25s %f", "Precision: ", numTP/ (double) (numTP + numFP)));
            System.out.println(String.format("%-25s %f", "Recall: ", numTP / (double) (numTP + numFN)));
            System.out.println(String.format("%-25s %f", "Null Error Rate (Majority " + majClass + "): ", nullErrorRate));
        }

        // TODO Refactor all of this away to appropriate places.
        if (algorithm.toLowerCase().equals("nb")) {
            NaiveBayes nb = new NaiveBayes();
            nb.train(wrangledTrainMessages);

//        int spamCount = tokenizedMessages.stream().filter(t ->  t.FILE_NAME.startsWith("s")).mapToInt(t -> 1).sum();
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


    private void classifyMessages(List<TokenizedMessage> tkTrainMessages,
                                  List<TokenizedMessage> tkTestMessages) {



    }
}
