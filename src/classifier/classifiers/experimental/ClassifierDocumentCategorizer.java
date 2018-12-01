package classifier.classifiers.experimental;

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

// TODO Create a Classifier interface or base class?

/**
 * An integration of Apache OpenNLP's Document Categorizer.
 */
public class ClassifierDocumentCategorizer {

    /**
     * Path to store temporary data, like generated models.
     */
    private Path tempDir;


    //---------------------+
    //    CONSTRUCTORS    /
    //-------------------+

    /**
     * Constructor.
     * Trains model with passed-in set of tokenized messages upon instantiation.
     * @param tkMessages tokenized messages
     */
    public ClassifierDocumentCategorizer(List<TokenizedMessage> tkMessages) {
        // Set up temporary directory for storing model.
        try {
            this.tempDir = Files.createTempDirectory("OpenNLPDocCat");
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        // Reformat messages to proper training data file format.
        File trainingFile = generateTrainingFile(tkMessages);

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

        // Get the model created by the generateTrainingFile(List<classifier.messagetypes.TokenizedMessage>) method.
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
}
