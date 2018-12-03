package classifier;

import classifier.messagetypes.TokenizedMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaiveBayes {

  // Maps token symbols to token objects
  private Map<String, Token> tokens;

  // <some_counter_label, current_value>
  // counter_labels: 'spam', 'ham', 'totalObjects', 'totalTokens'
  private Map<String, Integer> objectCounts; // what a terrible name ;;

  /********* config settings ***********/
  // specify the level of metadata to add
  // level 0 - no tagging
  // level 2 - append labels to tokens
  private int tagging = 0;
  private final String MODEL_SAVE_DIR = "./NB_models";
  private String smoothing = "laplace";

  private class Token <T> implements Comparable<Token<T>>{
    T token; // token symbol
    String activeLabel;
    int total;

    // String is a label of whatever you feel like counting
    HashMap<String, Integer> count; // <label, frequency_count>
    HashMap<String, Double> probability; // <descriptive_id, calculated_probability>

    public Token(T token) {
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
    objectCounts = new HashMap<>();
    objectCounts.put("totalObjects", 0);
    objectCounts.put("totalTokens", 0);
  }

  /***
   * Disabling this for now. TODO: fix model saving and loading
   * @param token
   * @param label
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


  // Processes the tokens in a message and updates the proper tables for future use.
  void train(TokenizedMessage message) {
    String label = (message.isSpam())? "spam" : "ham";

    // increment message counters
    objectCounts.put(label, objectCounts.getOrDefault(label,0) +1);
    objectCounts.put("totalObjects", objectCounts.get("totalObjects") +1);

    // Learn each distinct token, if tagging is >= 2 tokens are prepended with
    // (hopefully) unique strings for subject or body.
    // WANT: Is there a better way to tag tokens?
    if(tagging == 0) {
      message.getAllTokens().forEach(t->learn(t,label));
    } else if(tagging >=2) {
      HashSet<String> tokenSet = new HashSet<>(message.getSubjectTokens());
      tokenSet.forEach(t->learn("##subject##" + t,label));

      tokenSet = new HashSet<>(message.getBodyTokens());
      tokenSet.forEach(t->learn("##body##" + t,label));
    }
//    System.out.println("breakpoint");
  }

  // If this were to be used in a live environment it would need to learn
  // from newly predicted messages. But here we will only be learning training
  // messages.
  private void learn(String tk, String label){
    if (!tokens.containsKey(tk)) tokens.put(tk, new Token(tk));
    tokens.get(tk).increment(label);
    objectCounts.put("totalTokens", objectCounts.get("totalTokens") +1);
  }

  void calculatePriors(){
    // update probabilities
    if (smoothing == "laplace"){
      Double p = 1.0;
      Integer totalObjects = objectCounts.get("totalObjects");
      Integer totalTokens = objectCounts.get("totalTokens");
    }
    //Double pSpam = (tokens.get() == 0)? 0.0 : 1.0 * getSpamTkCount(tk)/getSpamMsgCount();
    //Double tkProb = pSpam / (pSpam + pHam);
  }


  // Process a message and try to predict it's label based on the training data.
  void test(TokenizedMessage message){
    String label = (message.isSpam())? "spam" : "ham";
    //Double prediction = predict(message.getAllTokens());

    // DEBUG
    //System.out.println(message.FILE_NAME+": "+prediction);
  }


/*
    // TODO refactor this to use the new Token class
  Double predict(List<String> tks){
    Double prediction = 0.0;
    prediction = tks.parallelStream()
        .map(this::getProbability)
        .filter(Objects::isNull)
        .peek(System.out::println)
        .reduce(1.0, (x,y)-> x*y);
    return prediction;
  }
*/

  //******************
  // Helper functions
  //******************

  void printTokenReport(){
    // TODO
  }

}