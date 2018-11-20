Email Classifier
===============

The Email Classifier (TODO better name?) provides classification of a given set of email messages. The messages are labeled spam or not spam using two core classification algorithms and X experimental algorithms,

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

Data
----

The data directory provided should have the structure:

```
data
├── test
│   ├── 3-426msg2.txt
│   ├── 3-426msg3.txt
│   ├── 3-429msg0.txt
│   ├── ...
│   ├── spmsgc144.txt
│   ├── spmsgc145.txt
│   └── spmsgc146.txt
└── train
    ├── 3-1msg1.txt
    ├── 3-1msg2.txt
    ├── 3-1msg3.txt
    ├── ...
    ├── spmsgb97.txt
    ├── spmsgb98.txt
    └── spmsgb99.txt
```

The label as spam is inferred from the filename - spam messages should begin with an 's'.

Algorithms
----------

### Core Algorithms

Two core classification algorithms are provided: K-Nearest Neighbors (KNN) and Naive Bayes (NB). 

To classify messages using KNN with default K of 3:

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
