import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaiveBayes {
  /**
   * probability that the message is spam
   */
  private final String DATA_DIRECTORY_PATH = "";

  Set<String> distinctTokens; // set of all distinct tokens

  private class MsgClassification {
    private String classType; // "spam" or "ham"
    private int msgCount; // total number of messages

    // Dictionaries mapping token to number of messages it has appeared in
    Map<String, Integer> tokenCounts = new TreeMap<>();

    // Dictionaries mapping token to probability it will appear in that type of message
    Map<String, Double> probabilities = new TreeMap<>();

    // Adds the token to the Dictionary if absent and sets it's count to 1
    //  or increments the token's counter by 1.
    public void incTokenCount(String token){
      tokenCounts.put(token, tokenCounts.getOrDefault(token,0)+1);
    }

    public void setProbability(String token, Double prob){
      probabilities.put(token, prob);
    }

    public Double getProbability(String token){
      return probabilities.get(token);
    }

  }

  // Initialize the probability dictionary?
  // TODO: refactor to use a better external datastore rather than flatfiles
  void initNB(String instance){
    // filename
    String datafile = (instance != null) ? instance + ".data" : "default.data";

    // try to load the database
    try (Stream<String> lines = Files.lines(Paths.get(instance))) {

    } catch (IOException e) {
      e.printStackTrace();
    }


  }

  // Take the messages from Classify. Stream through the messages twice,
  //  once to process spam, second time for ham.
  void train(ArrayList<TokenizedMessage> messages){
    messages.stream().filter(t ->  t.FILE_NAME.startsWith("s"))
        .mapToInt(t -> 1).sum();

    messages.stream().filter(t ->  !t.FILE_NAME.startsWith("s"))
        .mapToInt(t -> 1).sum();
  }

  // need 3 token counts in messages (I think): spam, ham, totals
  // can take a message as input then return a set<'token'>
//  void accumulateTokens(ArrayList<String> tokens, String msgType){
//    HashSet<String> tkSet = new HashSet<>();
//    tkSet.addAll(tokens);
//    tkSet.forEach(tk -> tokenCounts.putIfAbsent(tk, new Integer[]{1,1}));
//  }

}
