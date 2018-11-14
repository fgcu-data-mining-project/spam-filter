package org.email.classifier.project;

public class Main {

    public static void main(String[] args) {

        // If args, carry on.
        if (args.length > 0) {
            System.out.println("The command line" +
                    " arguments are:");

            // DEBUG
            for (String val : args) {
                System.out.println(val);
            }

            // TODO Parse cli args.

            // TODO Read data based on args.

            // TODO Clean/prepare data.

                // TODO Tokenize.

                // TODO Normalize.

                // TODO Remove stopwords.

            // TODO Load algo classifier based on args.

            // TODO Classify.

            // TODO Return data / Produce report.

        } else {
            // If no args, print help message.
            System.out.println("Help message here.");
        }
    }
}
