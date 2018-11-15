import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "Email Classifier", mixinStandardHelpOptions = true,
        version = "Email Classifier 0.1.1")
public class Classify implements Runnable {

    @Option(names = { "-v", "--verbose" },
            description = "Verbose mode. Multiple -v options increase the verbosity.")
    private boolean[] verbose = new boolean[0];

    @Parameters(arity = "1", paramLabel = "PATH",
            description = "A single path to a directory of emails to classify.")
    private Path inputPath;

    @Option(names = {"-a", "--algorithm"}, description = "KNN, NB, TODO EXPERIMENTAL...")
    private String algorithm = "KNN";

    public static void main(String[] args) {
        CommandLine.run(new Classify(), args);
    }

    @Override
    public void run() {

        // If verbose, print input path.
        if (verbose.length > 0) {
            System.out.println("Input path: " + inputPath.toString());
            System.out.println("Algorithm: " + algorithm);
        }

        // If very verbose, print paths to all files in input path directory.
        if (verbose.length > 1) {

            int fileCount = new File(inputPath.toString()).list().length;
            System.out.println("Number of messages to classify: " + fileCount);

            System.out.println("Messages:\n");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
                for (Path file: stream) {
                    System.out.println(file.getFileName());
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(ex);
            }
        }


        // TODO Read data based on args.

        // TODO Clean/prepare data.

            // TODO Tokenize.

            // TODO Normalize.

            // TODO Remove stopwords.

        // TODO Load algo classifier based on args.

        // TODO Classify.

        // TODO Return data / Produce report.
    }
}
