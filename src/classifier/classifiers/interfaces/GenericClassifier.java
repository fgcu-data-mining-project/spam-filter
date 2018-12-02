package classifier.classifiers.interfaces;

import classifier.messagetypes.TokenizedMessage;

import java.util.List;

public interface GenericClassifier {

    boolean predict(TokenizedMessage tkTestMessage);

    void predictDataSet(List<TokenizedMessage> tkMessages);
}