package classifier.classifiers.interfaces;

import classifier.messagetypes.TokenizedMessage;

public interface GenericClassifier {

    boolean predict(TokenizedMessage tkTestMessage);

}