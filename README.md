Email Classifier
===============

The Email Classifier (TODO better name?) provides binary classification of a given set of email messages. The messages are labeled spam or not spam using two core classification alogrithms and X experimental algorithms,

Usage
-----

### Core Algorithms

Two core classification algorithms are provided: K-Nearest Neighbors (KNN) and Naive Bayes (NB). 

The general usage of the commandline application is:

```
java classify ALGORITHM [OPTIONS] PATH_TO_ DATA_DIR
```

To classify messages using KNN with default K of 2:


```
java classify knn ./data
```

To classify messages using Naive Bayes:

```
java classify NB ~/MyHome/downloads/data
```

### Experimental Algorithms

TODO
