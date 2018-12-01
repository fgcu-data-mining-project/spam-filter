package classifier.classifiers.core;

import classifier.messagetypes.TokenizedMessage;

import java.util.*;
import java.util.stream.Collectors;


/**
 * KNN classification of plain text messages.
 * TODO Create a Classifier interface or base class?
 */
public class ClassifierKNN {

    /**
     * The k for KNN.
     */
    private int kforKNN;

    /**
     * List of wrangled training messages passed during
     * instatiation. These messages are used for
     * comparison to test messages.
     */
    private List<TokenizedMessage> tokenizedTrainMessages;

    //---------------------+
    //    CONSTRUCTORS    /
    //-------------------+

    // TODO Which stats to store in the class?

    /**
     * KNN Classifier for tokenized messages.
     * @param tokenizedMessages a classifier.messagetypes.TokenizedMessage
     * @param k k for knn
     */
    public ClassifierKNN(List<TokenizedMessage> tokenizedMessages, int k) {

        // Set the k.
        this.kforKNN = k;

        // Vector similarity w/o weighting.

        this.tokenizedTrainMessages = tokenizedMessages;
    }


    //-----------------------+
    //    PUBLIC METHODS    /
    //---------------------+

    /**
     * Predict label for a message based on training data
     * passed in during instantiation of classifier.classifiers.core.ClassifierKNN.
     * @param tkTestMessage a classifier.messagetypes.TokenizedMessage
     * @return the label - true if spam
     */
    public Boolean predict(TokenizedMessage tkTestMessage) {

        // Map for storing similarities.
        Map<String, Double> similarities = new HashMap<>();

        // Calculate similarity with all train messages.
        for (TokenizedMessage tkTrainMessage : tokenizedTrainMessages) {
            double similarity = similarity(tkTestMessage, tkTrainMessage);
            similarities.put(tkTrainMessage.getFILE_NAME(), similarity);
        }

        // DEBUG
        //System.out.println(similarities);

        // Find the k most-similar messages.
        // Sort in descending order by similarity.
        // Get top k from map.
        Map<String, Double> topKs = similarities.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(kforKNN)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Threshold for label.
        // Leaving this as an int ensures it will automatically
        // floor, so classification is always decisive for odd k.
        int labelThreshold = kforKNN / 2;
        int votesForSpam = 0;
        boolean isSpam = false;

        // DEBUG
        //System.out.println("Top k similarities: ");
        Iterator it = topKs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            // Count number of votes for spam.
            if (pair.getKey().toString().charAt(0) == 's') {
                votesForSpam++;
            }

            // DEBUG
            //System.out.println("  " + pair.getKey() + " = " + pair.getValue());
        }

        // DEBUG
        //System.out.println("Votes for spam: " + votesForSpam);
        //System.out.println("Label threshold: " + labelThreshold);

        // The label that appears most among the k most similar messages
        // is the label returned.
        if (votesForSpam > labelThreshold) { isSpam = true; }

        // Debug
        //System.out.println("Label: " + isSpam);

        return isSpam;
    }


    //------------------------+
    //    PRIVATE METHODS    /
    //----------------------+

    /**
     * Calculate (cosine angle) similarity between tokenized messages.
     * TODO Make public and static?
     * TODO Add term weighting?
     * @param tkMessage1 tokenized message
     * @param tkMessage2 tokenized message
     * @return completely dissimilar 0.0 to identical 1.0
     */
    private double similarity(TokenizedMessage tkMessage1, TokenizedMessage tkMessage2) {

        // Get tokens.
        List<String> allTokensSet1 = tkMessage1.getAllTokens();
        List<String> allTokensSet2 = tkMessage2.getAllTokens();

        // Get length of vectors.
        double lengthMssge1 = Math.sqrt(allTokensSet1.size());
        double lengthMssge2 = Math.sqrt(allTokensSet2.size());

        // Find intersection between sets of body tokens.
        List<String> intersection = new ArrayList<>(allTokensSet1);
        intersection.retainAll(allTokensSet2);

        // With all token weights 1, the size of the intersection of the
        // sets of tokens is equivalent to A dot B.
        // Return cosine angle as measure of similarity by dividing the
        // dot product by the product of the lengths of the vectors of tokens.
        return ((double) intersection.size()) / (lengthMssge1 * lengthMssge2);
    }


    //--------------------------+
    //    GETTERS & SETTERS    /
    //------------------------+

    public int getKforKNN() {
        return kforKNN;
    }

    public List<TokenizedMessage> getTokenizedTrainMessages() {
        return tokenizedTrainMessages;
    }
}
