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
  private Map<String, Double> probabilities;

  private final String MODEL_SAVE_DIR = "./NB_models";

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
    hamCounts = new TreeMap<>();
    spamCounts = new TreeMap<>();
    probabilities = new TreeMap<>();
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

  int getHamMsgCount() { return hamCounts.get("##hamMsgCount##"); }
  void incHamMsgCount() { hamCounts.put("##hamMsgCount##", getHamMsgCount() + 1); }

  int getSpamMsgCount() { return spamCounts.get("##spamMsgCount##"); }
  void incSpamMsgCount() { spamCounts.put("##spamMsgCount##", getSpamMsgCount() + 1); }

  public void setProbability(String token, Double prob) {
    probabilities.put(token, prob);
  }

  public Double getProbability(String token) {
    return probabilities.get(token);
  }


  // Take the messages from Classify. Stream through the messages twice,
  //  once to process spam, second time for ham.
  void train(List<TokenizedMessage> messages) {
//    1) loop through all the messages
//      1.1) loop through the tokens in each message
//      1.2) increment the token count in the respective mapCount
//      1.3) update the frequency value in the frequencyMap
//    2) save the state of this nb
    String label;
    for (TokenizedMessage msg: messages) {
      label = (msg.isSpam())? "spam" : "ham";
      for (String tk: msg.getSubjectTokens()){
        learn("##subject##$tk", label);
      }
      for (String tk: msg.getBodyTokens()){
        learn("##body##$tk", label);
      }
    }


//    messages.stream().filter(Message::isSpam)
//        .mapToInt(t -> 1).sum();
//
//    messages.stream().filter(t -> !t.isSpam())
//        .mapToInt(t -> 1).sum();
  }

  void learn(String tk, String label){
    // add token to the proper countsMap
    // then update the probability
  }

  // need 3 token counts in messages (I think): spam, ham, totals
  // can take a message as input then return a set<'token'>
//  void accumulateTokens(ArrayList<String> tokens, String msgType){
//    HashSet<String> tkSet = new HashSet<>();
//    tkSet.addAll(tokens);
//    tkSet.forEach(tk -> tokenCounts.putIfAbsent(tk, new Integer[]{1,1}));
//  }

}
