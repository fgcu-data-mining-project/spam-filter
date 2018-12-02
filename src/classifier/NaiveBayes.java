package classifier;

import classifier.messagetypes.TokenizedMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaiveBayes {

  // Maps token to number of messages it has appeared in
  private Map<String, Integer> hamCounts;
  private Map<String, Integer> spamCounts;

  // Maps token to probability it will appear in that type of message
  // replacing the Map with an ArrayList using Entry Objects for sorting...
  // this is problematic because there is no easy way to update these... I hate Java
  private Map<String, Double> probabilities;
//  private ArrayList<Entry<String>> probabilities;

  private final String MODEL_SAVE_DIR = "./NB_models";

  // specify the level of metadata to add
  // level 0 - no tagging
  // level 2 - append labels to tokens
  private int tagging = 0;
private class Token <T> implements Comparable<Token<T>>{
  T token; // token symbol
  String activeLabel;

  // String is a label of whatever you feel like counting
  HashMap<String, Integer> count; // <label, frequency_count>
  HashMap<String, Double> probability; // <descriptive_id, calculated_probability>

  public Token(T token) {
    this.token = token;
    this.activeLabel = "spam"; // defaulting to spam for no good reason
    this.count = new HashMap<>();
    this.probability = new HashMap<>();
  }

  // Increment the counter for specified label
  void increment(String label){
    count.put(label, count.getOrDefault(label,0)+1);
  }

  // return the count or 0 if there is no entry in the counts table
  int getCount(String label){
    return count.getOrDefault(label, 0);
  }

  // inserts the given value under the given label
  void updateProbability(String label, Double p){
    probability.put(label, p);
  }

  // returns the value associated with the label or 0.0 if the label is not in the table
  Double getProbabiltiy(String label){
    return probability.getOrDefault(label, 0.0);
  }

  String setActiveLabel(String label){
    this.activeLabel = label;
    return activeLabel;
  }

  // be sure to set the activeLabel before comparing
  public int compareTo(Token<T> t){
      // if this is larger returns > 0
      // if equal returns 0
      // if this is less than t returns < 0
    return (int) (t.probability.getOrDefault(activeLabel,0.0) - this.probability.getOrDefault(activeLabel,0.0));
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
    loadModel(modelName);
  }

  private void reset(){
    hamCounts = new HashMap<>();
    hamCounts.put("##msgCount##", 0);
    spamCounts = new HashMap<>();
    spamCounts.put("##msgCount##", 0);
    probabilities = new HashMap<>(); // I need this to be k,v pairs
  }

  void loadModel (String modelName) {
    // try to load the saved maps
    // Java is still an ugly horrible language...
    Stream<String> fileStream;
    try {
      // load the ham countsMap
      fileStream = Files.lines(Paths.get("$filename.ham"));
      fileStream.forEachOrdered(line -> {
        String s[] = line.split(",");
        hamCounts.put(s[0], Integer.valueOf(s[1]));
      });
      //hamMsgCount = hamCounts.get("##hamMsgCount##");

      // load the spam countsMap
      fileStream = Files.lines(Paths.get("$filename.spam"));
      fileStream.forEachOrdered(line -> {
        String s[] = line.split(",");
        spamCounts.put(s[0], Integer.valueOf(s[1]));
      });
      //spamMsgCount = spamCounts.get("##spamMsgCount##");

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
  }
  // Adds the token to the countsMap if absent and sets it's count to 1
  //  or increments the token's counter by 1.
  private void incTokenCount(String token, String label) {
    if (label.equals("ham")) {
      hamCounts.put(token, hamCounts.getOrDefault(token, 0) + 1);
    } else {
      spamCounts.put(token, spamCounts.getOrDefault(token, 0) + 1);
    }
  }

  int getHamMsgCount() { return hamCounts.get("##msgCount##"); }
  void incHamMsgCount() { hamCounts.put("##msgCount##", getHamMsgCount() + 1); }

  int getSpamMsgCount() { return spamCounts.get("##msgCount##"); }
  void incSpamMsgCount() { spamCounts.put("##msgCount##", getSpamMsgCount() + 1); }

  int getHamTkCount(String tk){ return hamCounts.getOrDefault(tk, 0); }
  int getSpamTkCount(String tk){ return spamCounts.getOrDefault(tk, 0); }


  public void setProbability(String token, Double prob) {
    probabilities.put(token, prob);
  }

  public Double getProbability(String token) {
    return probabilities.get(token);
  }


  // Processes the tokens in a message and updates the proper tables for future use.
  void train(TokenizedMessage message) {
//    1)
//      1.1)
//      1.2)
//      1.3) update the frequency value in the frequencyMap
//    2) save the state of this nb
    String label = (message.isSpam())? "spam" : "ham";

    // increment this label's message counter
    if (label.equals("ham")){
      incHamMsgCount();
    } else {
      incSpamMsgCount();
    }

    // Learn each distinct token, if tagging is >= 2 tokens are prepended
    // (hopefully) unique strings for subject or body
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


  // Process a message and try to predict it's label based on the training data.
  void test(TokenizedMessage message){
    String label = (message.isSpam())? "spam" : "ham";
    //Double prediction = predict(message.getAllTokens());

    // DEBUG
    //System.out.println(message.FILE_NAME+": "+prediction);
  }

  // If this were to be used in a live environment it would need to learn
  // from newly predicted messages. But here we will only be learning training
  // messages.
//  void learn(String tk){
//
//  }

  void learn(String tk, String label){
    // add/increment token to the proper countsMap
    incTokenCount(tk, label);

    // horrible way of handling the divide by zero cases...
    Double pSpam = (getSpamMsgCount() == 0)? 0.0 : 1.0 * getSpamTkCount(tk)/getSpamMsgCount();
    Double pHam = (getHamMsgCount() == 0)? 0.0 : 1.0 * getHamTkCount(tk)/getHamMsgCount();

    Double tkProb = pSpam / (pSpam + pHam);
//
//    Double tkProb = (getSpamTkCount(tk)/getSpamMsgCount())/
//        (1.0 * getSpamTkCount(tk)/getSpamMsgCount() + getHamTkCount(tk)/getHamMsgCount());
    // then update the probability
    setProbability(tk,tkProb);
  }

  Double predict(List<String> tokens){
    Double prediction = 0.0;
    prediction = tokens.parallelStream()
        .map(this::getProbability)
        .filter(Objects::isNull)
        .peek(System.out::println)
        .reduce(1.0, (x,y)-> x*y);
    return prediction;
  }
  //******************
  // Helper functions
  //******************

  void printTokenReport(){
    // TODO
  }

}