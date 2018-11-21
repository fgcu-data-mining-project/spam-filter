Email Classifier
===============

The Email Classifier (TODO better name?) provides classification of a given set of email messages. The messages are labeled spam or not spam using two core classification algorithms and X experimental algorithms,

Usage
-----

The general usage of the commandline application is:

```
Usage: Classify [-hV] [-v]... [--testPath=<testDataPath>]
                [--trainPath=<trainDataPath>] [-a=<algorithm>] [-k=<kforKNN>]
                PATH
      PATH            A single path to a directory containing training and testing
                        sets.
      --testPath=<testDataPath>
                      Path within data folder to test data.
      --trainPath=<trainDataPath>
                      Path within data folder to training data.
  -a, --algorithm=<algorithm>
                      KNN, NB, TODO...
  -h, --help          Show this help message and exit.
  -k, --k=<kforKNN>   Number of nearest neighbors - the K in KNN.
  -v, --verbose       Verbose mode. Multiple -v options increase the verbosity.
  -V, --version       Print version information and exit.
```

Data
----

The data directory expected by the default settings has the structure:

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

Otherwise, the `--trainPath` and `testPath` options can be used to specify different locations within the data directory.

### Label Inference

The label as spam is inferred from the filename - spam messages should begin with an 's'. The presence of the 's' character at the beginning of the filename of a message will cause that message to be labeled as spam.

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
