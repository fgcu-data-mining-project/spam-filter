package classifier;

import classifier.messagetypes.TokenizedMessage;

import java.util.*;
import java.util.stream.Collectors;

public class NaiveBayes {

  // Maps token symbols to token objects
  private Map<String, Token<String>> tokens;

  // <some_counter_label, current_value>
  // counter_labels: 'spam', 'ham', 'totalObjects', 'totalTokens'
  private Map<String, Integer> objectCounts; // what a terrible name ;;
  private Map<String, Double> pLabel; // p(label)= labelMessageCount / totalMessageCount

  /********* config settings ***********/
  // specify the level of metadata to add
  // level 0 - no tagging
  // level 1 - use priors
  // level 2 - append labels to tokens
  private int tagging = 1;
  private final String MODEL_SAVE_DIR = "./NB_models"; //wont work in production
  private Double alpha = 1.0; // see laplace smoothing (or additive smoothing)
  private Double alphaD = 1.0; // see laplace smoothing (or additive smoothing)

  private class Token <T> implements Comparable<Token<T>>{
    T token; // token symbol
    String activeLabel;
    int total;

    // String is a label of whatever you feel like counting
    HashMap<String, Integer> count; // <label, frequency_count>
    HashMap<String, Double> probability; // <descriptive_id, calculated_probability>

    Token(T token) {
      this.token = token;
      this.activeLabel = "spam"; // TODO: I don't think I need this for every object. A model-level(or class) variable might suffice.
      this.total = 0; // sum of all label counts: total count in the model
      this.count = new HashMap<>();
      this.probability = new HashMap<>();
    }

    // Increment the counter for specified label and total for the model.
    // Returns the new count.
    int increment(String label){
      this.total++;
      String l = label.toLowerCase();
      count.put(l, count.getOrDefault(l,0)+1);
      return count.get(l);
    }

    // return the count or 0 if there is no entry in the counts table
    int getCount(String label){
      return count.getOrDefault(label.toLowerCase(), 0);
    }

    // return sum of all label counts: total count in the model
    int getTotal(){ return total; }

    // inserts the given value under the given label
    void updateProbability(String label, Double p){
      probability.put(label.toLowerCase(), p);
    }

    // returns the value associated with the label or 0.0 if the label is not in the table
    Double getProbabiltiy(String label){
      return probability.getOrDefault(label.toLowerCase(), 0.0);
    }

    String setActiveLabel(String label){
      this.activeLabel = label.toLowerCase();
      return activeLabel;
    }

    // be sure to set the activeLabel before comparing
    public int compareTo(Token<T> t){
      // if this is larger returns > 0
      // if equal returns 0
      // if this is less than t returns < 0
      return (int) (t.probability.getOrDefault(activeLabel,0.0)
                    - this.probability.getOrDefault(activeLabel,0.0));
    }
  }

  // False if tokens map doesn't have an entry for this key (tk).
  private Boolean tokenIsKnown(String tk){
    return (tokens.get(tk) != null);
  }

  // False if not found in the token map OR token.count map doesn't have an
  //   entry for this label.
  private Boolean tokenIsKnown(String tk, String label){
    return (tokens.get(tk) != null && tokens.get(tk).getCount(label) != 0);
  }

  // default constructor. initializes to an untrained model
  NaiveBayes() {
    reset();
  }

  // Reload a trained model
  public NaiveBayes (String modelName){
    // TODO: load saved data into the appropriate fields
    // TODO: refactor to use a better external datastore rather than flatfiles
    String prefix = (modelName != null) ? modelName.toLowerCase().strip() : "default";

    // initialize empty maps
    reset();
    //loadModel(modelName);
  }

  private void reset(){
    tokens = new HashMap<>();
    pLabel = new HashMap<>();
    objectCounts = new HashMap<>();
    objectCounts.put("totalObjects", 0);
    objectCounts.put("totalTokens", 0);
  }

  /***
   * Disabling this for now. TODO: fix model saving and loading
   * /  /*
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
      Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.ham"), hamCounts.toString());

      // write the spam countsMap to the file
      Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.spam"), spamCounts.toString());

      // write the probabilities map to the file
      Files.writeString(Paths.get(MODEL_SAVE_DIR,"$filename.ham"), probabilities.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }*/

  // Train the model with a List of messages
  void train(List<TokenizedMessage> messages) {
    messages.forEach(this::processMessage);
    calculatePriors();
  }

  // Processes the tokens in a message and updates the proper tables for future use.
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
    // WANT: Is there a better way to tag tokens?
    if(tagging < 2) {
      message.getAllTokens().forEach(t->learn(t,label));
    } else if(tagging >= 2) {
      HashSet<String> tokenSet = new HashSet<>(message.getSubjectTokens());
      tokenSet.forEach(t->learn("##subject##" + t,label));

      tokenSet = new HashSet<>(message.getBodyTokens());
      tokenSet.forEach(t->learn("##body##" + t,label));
    }
  }

  // If this were to be used in a live environment it would need to learn
  // from newly predicted messages. But here we will only be learning training
  // messages.
  private void learn(String tk, String label){
    if (!tokens.containsKey(tk)) tokens.put(tk, new Token<>(tk));
    tokens.get(tk).increment(label);
    objectCounts.put("totalTokens", objectCounts.get("totalTokens") +1);
  }

  // use the default smoothing level
  private void calculatePriors(){
    calculatePriors(this.alpha);
  }

  // update learned probabilities
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


  // Process test messages and try to predict their labels based on the training data.
  void test(List<TokenizedMessage> messages){
    int test=14;
    System.out.println(messages.get(test).getFILE_NAME()
        +" is "+((messages.get(test).isSpam())?"spam":"ham")
        +" "+predict(messages.get(test).getAllTokens(),false).toString());

//    for (TokenizedMessage msg : messages){
//      System.out.println(msg.getFILE_NAME()+" is "+((msg.isSpam())?"spam":"ham")+" "+predict(msg.getAllTokens(),false).toString());
//    }
  }


  private Map<String, Double> predict(List<String> tks, Boolean verbose){
    Map<String, Double> predictions = new HashMap<>(pLabel); //copies the keys(labels)

    // factor that smooths probability distribution
    alphaD = alpha * objectCounts.get("totalTokens");

    // calculate p(label|tokens) for all labels and store in preditions map
    for (String label : predictions.keySet()) {
      predictions.put(label,
          tks.stream() // need to change this back to parallelStream() when done testing
          .filter(tk->tokenIsKnown(tk, label)) // skip tokens we haven't seen before
              .peek(t-> System.out.printf("%-16s",">"+t+"<"))
          .map(tk -> getpTokenLabel(tk,label))
//              .peek(System.out::println)
          .reduce(1.0, (accumulator,pTokensLabel)-> {
            System.out.printf("[a] %-2.22e - [p] %-2.22f\n",accumulator, pTokensLabel);
            return accumulator * pTokensLabel;
          })
//          * getpLabel(label)
          * (objectCounts.get(label) / objectCounts.get("totalObjects"))
      );
    }

    // Sorted in desc order by value
    return predictions/*.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (oldValue, newValue) -> oldValue, LinkedHashMap::new
        ))*/;
  }

  Double getpTokenLabel(String token, String label){
    return tokens.get(token).getProbabiltiy("pToken" + label);
  }

  Double getpLabel(String label){
    return pLabel.get(label);
  }

  //******************
  // Helper functions
  //******************

  void printTokenReport(){
    // TODO
  }

}