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
//  private Map<String, Double> probabilities;
  private ArrayList<Entry<String>> probabilities;

  private final String MODEL_SAVE_DIR = "./NB_models";

  private class Entry <T> implements Comparable<Entry<T>>{
    public T token;
    public Double probability;
    public Entry(T token, Double p){
      this.token=token;
      probability=p;
    }
    public int compareTo(Entry<T> e){
      // if this is larger returns > 0
      // if equal returns 0
      // if this is less than e returns < 0
      return (int) (e.probability - this.probability);
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
    probabilities = new ArrayList<>();
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
        probabilities.add(new Entry<>(s[0], Double.valueOf(s[1])));
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
    probabilities.add(token, prob);
  }

  public Double getProbability(String token) {
    return probabilities.get(token);
  }


  // Take the messages from Classify. Stream through the messages twice,
  //  once to process spam, second time for ham.
  void train(List<TokenizedMessage> messages) {
//    1)
//      1.1)
//      1.2) increment the token count in the respective mapCount
//      1.3) update the frequency value in the frequencyMap
//    2) save the state of this nb
    String label;
    for (TokenizedMessage msg: messages) {
      label = (msg.isSpam())? "spam" : "ham";
      HashSet<String> tknSet = new HashSet<>();
      tknSet.addAll(msg.getSubjectTokens());

      // increment this label's message counter
      if (label.equals("ham")){
        incHamMsgCount();
      } else {
        incSpamMsgCount();
      }

      for (String tk: tknSet){
        learn("##subject##"+tk, label);
      }
      for (String tk: tknSet){
        learn("##body##"+tk, label);
      }
    }


    System.out.println("breakpoint");
  }

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

  // need 3 token counts in messages (I think): spam, ham, totals
  // can take a message as input then return a set<'token'>
//  void accumulateTokens(ArrayList<String> tokens, String msgType){
//    HashSet<String> tkSet = new HashSet<>();
//    tkSet.addAll(tokens);
//    tkSet.forEach(tk -> tokenCounts.putIfAbsent(tk, new Integer[]{1,1}));
//  }

}
