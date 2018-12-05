package classifier.classifiers.experimental;

import classifier.classifiers.interfaces.GenericClassifier;
import classifier.messagetypes.TokenizedMessage;
import opennlp.tools.doccat.*;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO Create a Classifier base class?

/**
 * An integration of Apache OpenNLP's Document Categorizer.
 */
public class DocumentCategorizer implements GenericClassifier {

    /**
     * Path to store temporary data, like generated models.
     */
    private Path tempDir;

    /**
     * Map of statistics of training data.
     */
    private Map<String,Object> trainStats;

    /**
     * Map of statistics of test data.
     * // TODO Store test stats.
     */
    //private Map<String,Object> testStats;

    //---------------------+
    //    CONSTRUCTORS    /
    //-------------------+

    /**
     * Constructor.
     * Trains model with passed-in set of tokenized messages upon instantiation.
     * @param trainData tokenized messages
     */
    public DocumentCategorizer(List<TokenizedMessage> trainData) {
        // Create the maps to store stats.
        trainStats = new HashMap<>();

        // TODO Make data set class to wrap list of messages and auto-calculate some stats?

        // Calculate initial statistics on training data.
        // TOOD Move this into train(File) method?
        // Counts.
        // Total number of train messages.
        trainStats.put("numTotal", trainData.size());

        // Number of actual true.
        int numActualTrue = 0;
        for (TokenizedMessage tkMessage : trainData) {
            if (tkMessage.isSpam()) { numActualTrue++; }
        }
        trainStats.put("numActualTrue", numActualTrue);

        // Number of actual false.
        int numActualFalse = trainData.size() - numActualTrue;
        trainStats.put("numActualFalse", numActualFalse);

        // Null error rate.
        String majClass = null;
        double nullErrorRate = 0.0;
        if (numActualTrue > numActualFalse) {
            // spam = true is the majority class.
            majClass = "true";
            nullErrorRate = numActualFalse / (double) trainData.size();
        } else {
            // spam = false is the majority class.
            majClass = "false";
            nullErrorRate = numActualTrue / (double) trainData.size();
        }
        trainStats.put("majorityClass", majClass);
        trainStats.put("nullErrorRate", nullErrorRate);

        printTrainStats();

        // Set up temporary directory for storing model.
        try {
            this.tempDir = Files.createTempDirectory("OpenNLPDocCat");
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        // Reformat messages to proper training data file format.
        File trainingFile = generateTrainingFile(trainData);

        // Train the model with passed-in training data.
        train(trainingFile);
    }


    //-----------------------+
    //    PUBLIC METHODS    /
    //---------------------+

    /**
     * Predict label for message.
     * @param tkMessage tokenized message
     * @return true is message labeled as spam
     */
    public boolean predict(TokenizedMessage tkMessage) {

        // Get all tokens from messages, convert to array of
        // strings which is required by DocumentCategorizerME.
        List<String> allTokens = tkMessage.getAllTokens();
        String[] allTokensArr = allTokens.toArray(new String[0]);

        // Get the model created by the train(File) method.
        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream(new File(tempDir + "/en-email.model"));
        } catch (IOException ex) {
            // TODO Handle exception...
        }

        // Wrap input stream in the model class required by DocumentCategorizerME.
        DoccatModel model = null;
        try {
            model = new DoccatModel(modelIn);
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        // Create instance of the categorizer and run it.
        // TODO Move this to the class so it's only created once?
        DocumentCategorizerME categorizer = new DocumentCategorizerME(model);
        double[] outcomes = categorizer.categorize(allTokensArr);

        // Store outcome data in a map.
        Map<String, Double> categoryOutcomes = new HashMap<>();
        for (int i = 0; i < categorizer.getNumberOfCategories(); i++) {
            categoryOutcomes.put(categorizer.getCategory(i), outcomes[i]);

            // TODO Add stats to the messages in the list? Create a new list to hold stats?

        }

        // Determine predicted category and return.
        // TODO Should identical outcomes be considered a spam or ham outcome?
        return (categoryOutcomes.get("spam") > categoryOutcomes.get("ham"));
    }

    /**
     * Predict class of each message in a list of messages,
     * print report. TODO printing should be moved out of this method.
     * @param tkMessages list of tokenized messages
     */
    public void predictDataSet(List<TokenizedMessage> tkMessages) {

        // Counts.
        int totalNum = 0;
        int numActualTrue = 0;
        int numActualFalse = 0;
        int numTP = 0;
        int numTN = 0;
        int numFP = 0;
        int numFN = 0;

        // Classify test messages.
        System.out.println("============================================");
        for (TokenizedMessage tkTestkMsg : tkMessages) {

            boolean label = predict(tkTestkMsg);

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
        double nullErrorRate;
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


    //------------------------+
    //    PRIVATE METHODS    /
    //----------------------+

    /**
     * Train the model for Apache OpenNLP's Document Categorizer.
     * @param trainingData a single file containing training data in the format specified by OpenNLP
     */
    private void train(File trainingData) {
        // Create the model.
        DoccatModel model = null;
        try {
            // Build stream of training data required for the document categorizer train method.
            MarkableFileInputStreamFactory dataIn =
                    new MarkableFileInputStreamFactory(new File(trainingData.toString()));
            ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);
            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

            // Build output stream for writing model.
            OutputStream dataOut = new FileOutputStream(new File(tempDir + "/en-email.model"));

            // Train the model.
            // TODO Refactor this to allow parameters to be adjusted.
            // TODO Where the hell is documentation for the parameters?
            // Parameters:
            //  TrainerType  = Event,
            //  Cutoff       = 5,
            //  Iterations   = 100,
            //  Algorithm    = MAXENT
            //TrainingParameters trainingParams = TrainingParameters.defaultParams();
            model = DocumentCategorizerME.train("en",
                    sampleStream, TrainingParameters.defaultParams(), new DoccatFactory());

            // Serialize the model.
            BufferedOutputStream modelOut = new BufferedOutputStream(dataOut);
            model.serialize(modelOut);

            // Clean up.
            lineStream.close();
            sampleStream.close();
            dataOut.close();

        } catch (IOException ex) {
            // TODO Handle exceptions.
            System.err.println(ex.toString());
        }
    }

    /**
     * Generate training file from messages in format required by OpenNLP Document Categorizer.
     * @param tkMessages tokenized messages
     * @return Handle on training file
     */
    private File generateTrainingFile(List<TokenizedMessage> tkMessages) {
        // Create handle for training file.
        File trainingFile = new File(tempDir + "/en-email.train");

        // Wrap handle with a buffered writer, write file.
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(trainingFile));

            // Write each message as a single line in the file with
            // the appropriate label prepended.
            for (TokenizedMessage msg : tkMessages) {
                // Write label.
                if (msg.isSpam()) {
                    writer.write("spam ");
                } else {
                    writer.write("ham ");
                }

                // Write body.
                // TODO Include subject?
                List<String> body = msg.getBody();
                for (String line : body) {
                    writer.write(line);
                }

                writer.newLine();
            }

            // Clean up.
            writer.close();
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        return trainingFile;
    }

    /**
     * Print train data set statistics to standard out.
     */
    private void printTrainStats() {
        // TODO Format output.
        for (String key : trainStats.keySet()) {
            System.out.println(key + ": " + trainStats.get(key));
        }
    }
}
