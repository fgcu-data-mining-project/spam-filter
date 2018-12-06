package classifier.classifiers.core;

import classifier.messagetypes.TokenizedMessage;

import java.util.*;

public class NaiveBayes {

  // Maps token symbols to token objects
  private Map<String, Token<String>> tokens;

  // objectCount ids: 'spam', 'ham', 'totalObjects', 'totalTokens',
  //    'totalTokensspam', 'totalTokensham'
  private Map<String, Integer> objectCounts; // <id, count>
  private Map<String, Double> pLabel; // = labelMessageCount / totalMessageCount

  /********* ***********  ***********  ***********
   * NaiveBayes Class level configuration settings
   ************  ***********  ***********  ***********/

  /* tagging sets the level of metadata to add to tokens
  * level 0 - no tagging
  * level 2 - append labels to tokens
  */
  private int tagging = 0;

  /* verbosity controls the level of output detail
  * see setVerbosity method for specific levels
  */
  private int verbosity;

  // private final String MODEL_SAVE_DIR = "./NB_models"; // disabled for now
  private Double alpha = 1.0;   // see laplace smoothing (or additive smoothing)
  private Double alphaD = 1.0;  // see laplace smoothing (or additive smoothing)

  /**
   * Inner Class. Tokens class because this is Java and over-verbosity is
   * a requirement per the software license.
   * @param <T> Generic for fun
   */
  private class Token <T> implements Comparable<Token<T>>{
    T token;              // token symbol
    String activeLabel;   // Needed for comparing token objects.
    int total;            // sum of all label counts

    HashMap<String, Integer> count;       // <label, frequency_count>
    HashMap<String, Double> probability;  // <id, calculated_probability>

    /**
     * Default Token object constructor.
     * @param token
     */
    Token(T token) {
      this.token = token;
      this.activeLabel = "spam"; // defaulting to spam for no good reason
      this.total = 0;
      this.count = new HashMap<>();
      this.probability = new HashMap<>();
    }


    /**
     * Increment the message/occurrence counter for specified label and total
     * for the model.
     * @param label String label of message the token is in
     * @return int new count for the given label
     */
    int increment(String label){
      this.total++;
      String l = label.toLowerCase();
      count.put(l, count.getOrDefault(l,0)+1);
      return count.get(l);
    }


    /**
     * Return the number of messages for the given label set that this token
     * occurs in or 0 if there is no entry in the counts table.
     * @param label String label of the message set
     * @return int number of the label messages this token appears in
     */
    int getCount(String label){
      return count.getOrDefault(label.toLowerCase(), 0);
    }


    /**
     * Return the total number of messages this token occurs in.
     * Same as count['spam'] + count['ham']
     * @return int total count of token in all labels
     */
    int getTotal(){ return total; }


    /**
     * Inserts the given probability value under the given id.
     * @param pTokenLabel String id of requested probability
     * @param p Double probability associated with this label
     */
    void updateProbability(String pTokenLabel, Double p){
      probability.put(pTokenLabel.toLowerCase(), p);
    }


    /**
     * Returns a known probability for this token or 0.0 if the token wasn't
     * found in the label's training set.
     * Possible ids [pToken, pTokeSpam, pTokenHam]
     * @param pTokenLabel String id of requested probability
     * @return Double probability associated with this label
     */
    Double getProbabiltiy(String pTokenLabel){
      return probability.getOrDefault(pTokenLabel.toLowerCase(), 0.0);
    }


    /**
     * Changes the label that Token objects are compared in.
     * @param label
     * @return String new activeLabel
     */
    String setActiveLabel(String label){
      this.activeLabel = label.toLowerCase();
      return activeLabel;
    }


    /**
     * Feature meant to be used in prediction. By selecting some number of the
     * most 'interesting' tokens (those with the highest probability for the
     * given label) the response time should improve without sacrificing
     * accuracy too much.
     * NOTE: Make sure activeLabel is set to the desired label before comparing.
     * @param t other token object to compare this token with
     * @return representative of the Token order based on polarity
     */
    @Override
    public int compareTo(Token<T> t){
      // if this is larger returns > 0
      // if equal returns 0
      // if this is less than t returns < 0
      return (int) (t.probability.getOrDefault(activeLabel,0.0)
                  - this.probability.getOrDefault(activeLabel,0.0));
    }
  }


  /**
   * False if tokens map doesn't have an entry for this key (tk).
   * @param tk
   * @return true if there is a token object
   */
  private Boolean tokenIsKnown(String tk){
    return (tokens.get(tk) != null);
  }


  /**
   * Returns false if not found in the token map OR token.count map
   * doesn't have an entry for this label.
   * @param tk
   * @param label
   * @return true if there is a token object and has a token count for this label
   */
  private Boolean tokenIsKnown(String tk, String label){
    return (tokens.get(tk) != null && tokens.get(tk).getCount(label) != 0);
  }


  /**
   * Default constructor. Initializes to an untrained model.
   */
  public NaiveBayes() {
    reset();
  }


  /**
   * Clears prior data objects and resets control values to defaults
   */
  private void reset(){
    tokens = new HashMap<>();
    pLabel = new HashMap<>();
    objectCounts = new HashMap<>();
    objectCounts.put("totalObjects", 0);
    objectCounts.put("totalTokens", 0);
    verbosity = 0;
  }


  /**
   * Level of reporting details displayed (output is cumulative):
   * 0 -- displays nothing atm
   * 1 -- displays the prediction accuracy
   * 2 -- for each message displays
   *      [filename, label, predicted label, and if correct or wrong]
   * 3 -- for each token displays
   *      ['>' token '<', the accumulated probability, this token's probability]
   *          ('>' and '<' are delimiters to bookend the token)
   * @param verbosity
   */
  public void setVerbosity(int verbosity){
    this.verbosity = verbosity;
  }


  /**
   * Train the model with a List of message objects.
   * @param messages message objects with tokens to process
   */
  public void train(List<TokenizedMessage> messages) {
    messages.forEach(this::processMessage);
    calculatePriors();
  }


  /**
   * Processes the tokens in a message and updates the proper tables for
   * future use.
   * @param message message object with tokens to process
   */
  private void processMessage(TokenizedMessage message) {
    // I'm cheating here...
    String label = (message.isSpam())? "spam" : "ham";

    // add an entry in the pLabels table if needed
    this.pLabel.putIfAbsent(label, 0.0);

    // increment message counters
    objectCounts.put(label, objectCounts.getOrDefault(label,0) +1);
    objectCounts.put("totalObjects", objectCounts.get("totalObjects") +1);

    // Learn each distinct token, if tagging is >= 2 tokens are prepended with
    // (hopefully) unique strings for subject or body.
    if(tagging < 2) {
      message.getAllTokens().forEach(t->learn(t,label));
    } else if(tagging >= 2) {
      HashSet<String> tokenSet = new HashSet<>(message.getSubjectTokens());
      tokenSet.forEach(t->learn("##subject##" + t,label));

      tokenSet = new HashSet<>(message.getBodyTokens());
      tokenSet.forEach(t->learn("##body##" + t,label));
    }
  }


  /**
   * If this were to be used in a live environment it would need to learn
   * from newly predicted messages. But here we will only be learning training
   * messages.
   * @param tk    String the token to learn
   * @param label String the class the token belongs to
   */
  private void learn(String tk, String label){
    if (!tokens.containsKey(tk)) tokens.put(tk, new Token<>(tk));
    tokens.get(tk).increment(label);
    objectCounts.put("totalTokens"+label, objectCounts.getOrDefault("totalTokens"+label,0) +1);
    objectCounts.put("totalTokens", objectCounts.get("totalTokens") +1);
  }


  /**
   * Uses the default Laplace smoothing level.
   */
  private void calculatePriors(){
    calculatePriors(this.alpha);
  }


  /**
   * Overloaded method to update learned probabilities.
   * @param alpha level of Laplace smoothing to apply
   */
  private void calculatePriors(Double alpha){
    Integer totalObjects = objectCounts.get("totalObjects");

    // factor that smooths probability distribution
    alphaD = alpha * objectCounts.get("totalTokens");

    pLabel.keySet().forEach(label->pLabel
        .put(label, 1.0 * objectCounts.get(label) / totalObjects)
    );

    for (Token<String> tk : tokens.values()){
      // update token probability for each label
      tk.count.forEach((label,count)-> tk.updateProbability("pToken"+label,
          (count + alpha)/ (objectCounts.get(label) + alphaD) ));
      // update probability of token in the model
      tk.updateProbability("pToken",1.0 * tk.getTotal()/totalObjects);
    }
  }


  /**
   * Process test messages and try to predict their labels based on the
   * training data.
   * @param messages ArrayList of messages to learn
   */
  public void test(List<TokenizedMessage> messages){
    Map<TokenizedMessage, Map<String, Double>> predictions = new LinkedHashMap<>();
    Map<String, Integer> accuracy = new HashMap<>();
    Map<String, String[]> messageReport = new HashMap<>();

    messages.forEach(m->predictions.put( m, predict(m.getAllTokens()) ));

    predictions.forEach((m,p) -> {
      String label = (m.isSpam()) ? "spam" : "ham";
      String nbPredicted = (p.get("spam") > p.get("ham"))? "spam" : "ham";

      messageReport.put(m.getFILE_NAME(), new String[]{label,nbPredicted});
      accuracy.put("total"+label, accuracy.getOrDefault("total"+label, 0) + 1);

      if (nbPredicted.equals(label)) {
        accuracy.put(label, accuracy.getOrDefault(label, 0) + 1);
      }
    });

    printMessageReport(messageReport);
    printTestReport(predictions.size(), accuracy);
  }


  /**
   * Uses a List of tokens to evaluate for each label the probability the tokens
   * will appear as together in a message.
   * @param tks List of tokens
   * @return Map of [label, probability]
   */
  private Map<String, Double> predict(List<String> tks){
    Map<String, Double> predictions = new HashMap<>(pLabel); //copies the keys(labels)
    Map<String, Double[]> tokenCalculations = new LinkedHashMap<>();

    // factor that smooths probability distribution
    alphaD = alpha * objectCounts.get("totalTokens");

    // calculate p(label|tokens) for all labels and store in predictions map
    for (String label : predictions.keySet()) {
      Double baseProb =  Math.log(1.0 * objectCounts.get(label)/objectCounts.get("totalObjects"));

      predictions.put(label,
          tks.stream() // can't do parallelStream() and retain ordered print data
              .filter(tk->tokenIsKnown(tk, label)) // skip tokens we haven't seen before
              .map(tk -> {
                Map<String, Double> m = new HashMap<>();
                m.put(tk, getTokenLabelCount(tk,label) + alpha);
                return m;
              })
              .peek(m -> {
                String id = m.keySet().toArray()[0].toString();
                m.put(id, m.get(id)/(objectCounts.get("totalTokens"+label) + alphaD));
              })
              .reduce(baseProb,(labelProb,m)-> {
                String id = m.keySet().toArray()[0].toString();
                tokenCalculations.put(id, new Double[]{labelProb, m.get(id)});
                return labelProb + Math.log(m.get(id));
              }, Double::sum) * -1.0
      );
      System.out.println();
    }

    printCalculationsReport(tokenCalculations);
    return predictions;
  }

  /**
   * Gets the number of times this token appears in this label's
   * training messages
   * @param token
   * @param label
   * @return number of times token appears in this label
   */
  private Integer getTokenLabelCount(String token, String label){
    return tokens.get(token).getCount(label);
  }

  /**
   * Gets the prior calculated p(token|label) probability value.
   * @param token
   * @param label
   * @return probability of token occurring for this label
   */
  Double getpTokenLabel(String token, String label){
    return tokens.get(token).getProbabiltiy("pToken" + label);
  }

  /**
   * Gets the prior calculated p(label) probability value.
   * @param label
   * @return probability of label occurring in this model
   */
  Double getpLabel(String label){
    return pLabel.get(label);
  }

  //******************
  // Helper functions
  //******************

  /**
   * Prints the probability calculations for each token and the accumulated value
   * @param tokenCalculations Map <token symbol, [accumulatedProb, tkProb]>
   */
  private void printCalculationsReport(Map<String, Double[]> tokenCalculations) {
    if (verbosity >= 3) {
      tokenCalculations.forEach((tk,probs)-> {
        System.out.printf("%-16s[a] %-2.22e - [p] %-2.22f\n",
          ">" + tk + "<", probs[0], probs[1]);
      });
    }
  }

  /**
   * Prints evaluation statistics for each file tested.
   * @param messageReport Map <fileName, label, predictedLabel>
   */
  private void printMessageReport(Map<String, String[]> messageReport) {
    if (verbosity >= 2) {
      messageReport.forEach((fileName, labels) ->
          System.out.printf("%-17s is %-6s NB says it is %-4s [ %7s ]\n",
              fileName, labels[0] + ".", labels[1],
              (labels[1].equals(labels[0])) ? "Correct" : "Wrong")
      );
    }
  }

  /**
   * Prints statistics on the final results of testing.
   * @param messageCount int number of messages processed in testing
   * @param accuracy Map <label, labelMessageCount>
   */
  private void printTestReport(int messageCount, Map<String, Integer> accuracy){
    if (verbosity >=1) {
        System.out.printf("Spam Accuracy:   %3d / %-3d = %-2.1f %%\n",
          accuracy.get("spam"), accuracy.get("totalspam"),
          (accuracy.get("spam") * 100.0)/accuracy.get("totalspam"));

      System.out.printf("Ham Accuracy:    %3d / %-3d = %-2.1f %%\n",
          accuracy.get("ham"), accuracy.get("totalham"),
          (accuracy.get("ham") * 100.0)/accuracy.get("totalham"));

      System.out.printf("Model Accuracy:  %3d / %-3d = %-2.1f %%\n",
          accuracy.get("spam")+accuracy.get("ham"), messageCount,
          ((accuracy.get("spam")+accuracy.get("ham")) * 100.0)/messageCount);
    }
  }

  /* **************************************************************************
   * A model saving and loading feature was part of the original design,
   * due to time constraints we were unable to complete it. Leaving this code
   * for possible future use.
   ---------------------------------------------------------------------------

  / **
   * Constructor that reloads a previously trained model from a file store.
   * @param modelName name used to identify the model and used for filenames.
   * /
  public NaiveBayes (String modelName){
    // TO DO: load saved data into the appropriate fields
    // TO DO: refactor to use a better external datastore rather than flatfiles
    String prefix = (modelName != null) ? modelName.toLowerCase().strip() : "default";

    // initialize empty maps
    reset();
    loadModel(modelName);
  }

   void loadModel (String modelName) {
   // try to load the saved maps
   Stream<String> fileStream;
   try {

   fileStream = Files.lines(Paths.get("$filename.ham"));
   fileStream.forEachOrdered(line -> {
   String s[] = line.split(",");
   hamCounts.put(s[0], Integer.valueOf(s[1]));
   });

   // load the probabilities map
   fileStream = Files.lines(Paths.get("$filename.probs"));
   fileStream.forEachOrdered(line -> {
   String s[] = line.split(",");
   probabilities.put(s[0], Double.valueOf(s[1]));
   });

   fileStream.close();
   } catch (IOException e) {
   e.printStackTrace();
   }
   }

   void saveModel (String modelName) {
   // try to the current model
   // Java is still an ugly horrible language...

   Stream<String> fileStream;
   try {

   // create the save directory if it doesn't already exist
   if (!Files.exists(Paths.get(MODEL_SAVE_DIR)))
   Files.createDirectory(Paths.get(MODEL_SAVE_DIR));

   // TODO: this doesn't have the correct output format
   // save the ham countsMap
   Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.ham"),
            hamCounts.toString());

   // write the spam countsMap to the file
   Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.spam"),
            spamCounts.toString());

   // write the probabilities map to the file
   Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.ham"),
          probabilities.toString());
   } catch (IOException e) {
   e.printStackTrace();
   }
   }
   * **************************************************************************/
}