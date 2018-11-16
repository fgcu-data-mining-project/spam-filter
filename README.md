Email Classifier
===============

The Email Classifier (TODO better name?) provides (multinomial?) classification of a given set of email messages. The messages are labeled spam or not spam using two core classification alogrithms and X experimental algorithms,

Usage
-----

The general usage of the commandline application is:

```
Usage: Classify [-hV] [-v]... [-a=<algorithm>] [-k=<kforKNN>] PATH
      PATH            A single path to a directory of emails to classify.
  -a, --algorithm=<algorithm>
                      KNN, NB, TODO EXPERIMENTAL...
  -h, --help          Show this help message and exit.
  -k, --k=<kforKNN>   Number of nearest neighbors - the K in KNN.
  -v, --verbose       Verbose mode. Multiple -v options increase the verbosity.
  -V, --version       Print version information and exit.
```

### Core Algorithms

Two core classification algorithms are provided: K-Nearest Neighbors (KNN) and Naive Bayes (NB). 

To classify messages using KNN with default K of 2:

```
java Classify -a knn ./data
```

To classify messages using KNN with K of 5:

```
java Classify -a knn -k 5 ./data
```

To classify messages using Naive Bayes:

```
java Classify -a NB ~/some/place/with/data
```

### Experimental Algorithms

TODO
