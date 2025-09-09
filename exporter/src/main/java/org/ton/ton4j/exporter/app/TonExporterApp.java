package org.ton.ton4j.exporter.app;

import org.ton.ton4j.exporter.Exporter;

/**
 * Command-line application for exporting TON blockchain data.
 *
 * <p>Usage: <br>
 * For file output: java -jar TonExporterApp &lt;ton-db-root-path&gt; file &lt;json|boc&gt; &lt;num-of-threads&gt;
 * &lt;true|false&gt; &lt;output-file-name&gt; <br>
 * For stdout output: java -jar TonExporterApp &lt;ton-db-root-path&gt; stdout &lt;json|boc&gt; &lt;num-of-threads&gt;
 * &lt;true|false&gt;
 *
 * <p>Arguments: - ton-db-root-path: Path to the TON database root directory - file|stdout: Output
 * destination (file or stdout) - json|boc: Output format (json for deserialized blocks, boc for raw
 * hex format) - num-of-threads: Number of parallel threads to use for processing - true|false:
 * Whether to show progress information during export - output-file-name: Name of the output file
 * (required only for file output)
 */
public class TonExporterApp {

  public static void main(String[] args) {
    if (args.length < 4) {
      printUsage();
      System.exit(1);
    }

    try {
      // Parse command line arguments
      String tonDbRootPath = args[0];
      String outputDestination = args[1].toLowerCase();
      String outputFormat = args[2].toLowerCase();
      int numOfThreads = Integer.parseInt(args[3]);
      String showProgressStr = "false"; // Default value for stdout
      String outputFileName = null;

      // For file output, output filename is required
      if (outputDestination.equals("file")) {
        if (args.length < 6) {
          System.err.println("Error: Output file name is required when using 'file' destination");
          printUsage();
          System.exit(1);
        }
        showProgressStr = args[4].toLowerCase();
        outputFileName = args[5];
      } else if (outputDestination.equals("stdout")) {
        // For stdout, use defaults and ignore extra arguments
        if (args.length >= 5) {
          showProgressStr = args[4].toLowerCase();
        }
        // Ignore any additional arguments for stdout
      }

      // Validate arguments
      if (!outputDestination.equals("file") && !outputDestination.equals("stdout")) {
        System.err.println("Error: Output destination must be 'file' or 'stdout'");
        printUsage();
        System.exit(1);
      }

      if (!outputFormat.equals("json") && !outputFormat.equals("boc")) {
        System.err.println("Error: Output format must be 'json' or 'boc'");
        printUsage();
        System.exit(1);
      }

      if (numOfThreads <= 0) {
        System.err.println("Error: Number of threads must be a positive integer");
        printUsage();
        System.exit(1);
      }

      if (!showProgressStr.equals("true") && !showProgressStr.equals("false")) {
        // For stdout, default to false if invalid value provided
        if (outputDestination.equals("stdout")) {
          showProgressStr = "false";
        } else {
          System.err.println("Error: Show progress must be 'true' or 'false'");
          printUsage();
          System.exit(1);
        }
      }

      // Parse show progress flag
      boolean showProgress = Boolean.parseBoolean(showProgressStr);

      // Create exporter instance
      Exporter exporter =
          Exporter.builder().tonDatabaseRootPath(tonDbRootPath).showProgress(showProgress).build();

      // Determine if output should be deserialized (json = true, boc = false)
      boolean deserialized = outputFormat.equals("json");

      // Execute appropriate export method based on destination and format
      if (outputDestination.equals("file")) {
        if (outputFileName == null) {
          System.err.println("Error: Output file name is required for file output");
          printUsage();
          System.exit(1);
        }
        exporter.exportToFile(outputFileName, outputFormat.equals("json"), numOfThreads);
      } else {
        exporter.exportToStdout(outputFormat.equals("json"), numOfThreads);
      }

      System.err.println("Export completed successfully.");

    } catch (NumberFormatException e) {
      System.err.println("Error: Invalid number format for threads: " + args[3]);
      printUsage();
      System.exit(1);
    } catch (Exception e) {
      System.err.println("Error during export: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.err.println("Usage:");
    System.err.println(
        "  For file output: java -jar TonExporterApp <ton-db-root-path> file <json|boc> <num-of-threads> <true|false> <output-file-name>");
    System.err.println(
        "  For stdout output: java -jar TonExporterApp <ton-db-root-path> stdout <json|boc> <num-of-threads>");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println("  ton-db-root-path  : Path to the TON database root directory");
    System.err.println("  file|stdout       : Output destination (file or stdout)");
    System.err.println(
        "  json|boc         : Output format (json for deserialized, boc for raw hex)");
    System.err.println("  num-of-threads   : Number of parallel threads to use");
    System.err.println("  true|false       : Whether to show progress information during export");
    System.err.println(
        "  output-file-name : Name of the output file (required only for file output)");
    System.err.println();
    System.err.println("Examples:");
    System.err.println("  java -jar TonExporterApp /var/ton-work/db file json 4 true blocks.json");
    System.err.println("  java -jar TonExporterApp /var/ton-work/db stdout boc 8");
  }
}
