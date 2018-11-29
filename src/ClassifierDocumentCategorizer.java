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

public class ClassifierDocumentCategorizer {

    /**
     * Path to store temporary data, like generated models.
     */
    private Path tempDir;

    private List<TokenizedMessage> tkTrainMessages;

    private List<TokenizedMessage> tkTestMessages;

    //---------------------+
    //    CONSTRUCTORS    /
    //-------------------+

    /**
     * Constructor.
     */
    public ClassifierDocumentCategorizer(List<TokenizedMessage> tkMessages) {

        this.tkTrainMessages = tkMessages;

        // TODO Set up temporary directory for storing models?
        try {
            this.tempDir = Files.createTempDirectory("docCatTemp");
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        // Reformat messages to proper training data file format.
        File trainingFile = generateTrainingFile(tkMessages);

        // Train the model with passed-in training data.
        train(trainingFile);
    }

    public boolean predict(TokenizedMessage tkMessage) {

        List<String> allTokens = tkMessage.getAllTokens();
        String[] allTokensArr = allTokens.toArray(new String[0]);

        InputStream modelIn = null;
        DoccatModel model = null;

        try {
            modelIn = new FileInputStream(new File(tempDir + "/en-email.model"));
        } catch (IOException ex) {
            // Handle exceptions
        }

        try {
            model = new DoccatModel(modelIn);
        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        // Create instance of the categorizer.
        // TODO Move this to the class so it's only created once?
        DocumentCategorizerME categorizer = new DocumentCategorizerME(model);
        double[] outcomes = categorizer.categorize(allTokensArr);

        // Store outcome data.
        Map<String, Double> categoryOutcomes = new HashMap<>();
        for (int i = 0; i < categorizer.getNumberOfCategories(); i++) {
            categoryOutcomes.put(categorizer.getCategory(i), outcomes[i]);
        }

        // Determine predicted category.
        if (categoryOutcomes.get("spam") > categoryOutcomes.get("ham")) {
            return true;
        } else {
            return false;
        }
    }

    private void train(File trainingData) {

        // Create the model.
        DoccatModel model = null;

        try {

            MarkableFileInputStreamFactory dataIn = new MarkableFileInputStreamFactory(
                    new File(trainingData.toString()));

            OutputStream dataOut = new FileOutputStream(new File(tempDir + "/en-email.model"));

            ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);

            // DEBUG
            //String line = lineStream.read();

            //System.out.println(line);

            //while (line != null) {
            //    System.out.println(line);
            //    line = lineStream.read();
            //}

            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

            model = DocumentCategorizerME.train("en",
                    sampleStream, TrainingParameters.defaultParams(), new DoccatFactory());

            BufferedOutputStream modelOut = new BufferedOutputStream(dataOut);
            model.serialize(modelOut);

        } catch (IOException ex) {
            // TODO Handle exceptions.
            System.err.println(ex.toString());
        }
    }

    private File generateTrainingFile(List<TokenizedMessage> tkMessages) {

        File trainingFile = new File(tempDir + "/en-email.train");

        // Get handle for file for buffered writer.
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(trainingFile));

            // Write each message as a line in the file with the appropriate label.
            for (TokenizedMessage msg : tkMessages) {

                // Label
                if (msg.isSpam()) {
                    writer.write("spam ");
                } else {
                    writer.write("ham ");
                }

                // Body
                List<String> body = msg.getBody();
                for (String line : body) {
                    writer.write(line);
                }

                writer.newLine();

            }

            writer.close();

        } catch (IOException ex) {
            // TODO Handle exception...
            System.err.println(ex);
        }

        return trainingFile;
    }
}
