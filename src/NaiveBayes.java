import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaiveBayes {

  Set<String> distinctTokens; // set of all distinct tokens

  // Maps token to number of messages it has appeared in
  private Map<String, Integer> hamCounts;
  private Map<String, Integer> spamCounts;

  // number of messages of each type
  private Integer spamMsgCount;
  private Integer hamMsgCount;

  // Maps token to probability it will appear in that type of message
  private Map<String, Double> probabilities;

  // default constructor. initializes to an untrained model
  NaiveBayes() {
    reset();
  }

  // Reload a trained model
  public NaiveBayes (String filename){
    // TODO: load saved data into the appropriate fields
    // TODO: refactor to use a better external datastore rather than flatfiles
    String prefix = (filename != null) ? filename.toLowerCase().strip() : "default";

    // initialize empty maps
    reset();

    // all this just to use one construct to load three files
    String[] fileTypes = {"ham","spam","probs"};
    Map[] trees = {hamCounts, spamCounts, probabilities};

    // try to load the saved maps
    // Java is still an ugly horrible language...
    for (int i=0;i<fileTypes.length;i++) {
      try (Stream<String> fileStream = Files.lines(Paths.get("$filename." + fileTypes[i]))) {
        Map tree = trees[i];
        fileStream.forEachOrdered(line -> {
          String s[] = line.split(",");
          tree.put(s[0], Integer.valueOf(s[1]));
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void reset(){
    hamCounts = new TreeMap<>();
    spamCounts = new TreeMap<>();
    probabilities = new TreeMap<>();
    spamMsgCount = 0;
    hamMsgCount = 0;
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

  private void incHamMsgCount(){ hamMsgCount++; }
  private void incSpamMsgCount(){ spamMsgCount++; }

  public void setProbability(String token, Double prob) {
    probabilities.put(token, prob);
    //probabilities[token] = prob;
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
        learn(tk, label);
      }
      for (String tk: msg.getBodyTokens()){

      }
    }


//    messages.stream().filter(Message::isSpam)
//        .mapToInt(t -> 1).sum();
//
//    messages.stream().filter(t -> !t.isSpam())
//        .mapToInt(t -> 1).sum();
  }

  // need 3 token counts in messages (I think): spam, ham, totals
  // can take a message as input then return a set<'token'>
//  void accumulateTokens(ArrayList<String> tokens, String msgType){
//    HashSet<String> tkSet = new HashSet<>();
//    tkSet.addAll(tokens);
//    tkSet.forEach(tk -> tokenCounts.putIfAbsent(tk, new Integer[]{1,1}));
//  }

}
