package org.ton.ton4j.exporter.app;

import com.google.gson.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.ton.ton4j.exporter.Exporter;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.adapters.ByteArrayToHexTypeAdapter;
import org.ton.ton4j.utils.Utils;

/**
 * Command-line application for exporting TON blockchain data.
 *
 * <p>Usage: <br>
 * For file output: java -jar TonExporterApp &lt;ton-db-root-path&gt; file &lt;json|boc&gt;
 * &lt;num-of-threads&gt; &lt;true|false&gt; &lt;output-file-name&gt; [last] <br>
 * For stdout output: java -jar TonExporterApp &lt;ton-db-root-path&gt; stdout &lt;json|boc&gt;
 * &lt;num-of-threads&gt; [&lt;true|false&gt;] [last]
 *
 * <p>Arguments: - ton-db-root-path: Path to the TON database root directory - file|stdout: Output
 * destination (file or stdout) - json|boc: Output format (json for deserialized blocks, boc for raw
 * hex format) - num-of-threads: Number of parallel threads to use for processing - true|false:
 * Whether to show progress information during export - output-file-name: Name of the output file
 * (required only for file output) - last: Optional flag to get only the last block (ignores
 * progress and uses 1 thread)
 *
 * <p>Last Mode: When 'last' is specified as the final argument, the application will retrieve only
 * the most recent master chain block from the database. In this mode, progress display is disabled
 * and only 1 thread is used. For stdout output with json format, additional information is
 * displayed including block sequence number, transactions and messages count.
 */
public class TonExporterApp {

  // Statistics tracking variables
  private static final AtomicBoolean exportInterrupted = new AtomicBoolean(false);
  private static final AtomicBoolean exportCompleted = new AtomicBoolean(false);
  private static final AtomicLong startTime = new AtomicLong(0);
  private static volatile String currentOutputFile = null;
  private static volatile String currentOutputDestination = null;
  private static volatile Exporter currentExporter = null;
  private static volatile Thread shutdownHook = null;

  public static void main(String[] args) {
    // Check for version argument first
    if (args.length == 1 && "-v".equals(args[0])) {
      printVersion();
      System.exit(0);
    }

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
      boolean isLastMode = false;

      // Check if the last argument is "last"
      if (args.length > 0 && "last".equals(args[args.length - 1])) {
        isLastMode = true;
        // In last mode, ignore show progress and use 1 thread
        showProgressStr = "false";
        numOfThreads = 1;
      }

      // For file output, output filename is required
      if (outputDestination.equals("file")) {
        int requiredArgs = isLastMode ? 6 : 6; // Same number of args, but last one might be "last"
        if (args.length < requiredArgs) {
          System.err.println("Error: Output file name is required when using 'file' destination");
          printUsage();
          System.exit(1);
        }
        if (!isLastMode) {
          showProgressStr = args[4].toLowerCase();
          outputFileName = args[5];
        } else {
          // In last mode, the structure is: <path> file <format> <threads> <progress> <filename>
          // last
          if (args.length >= 7) {
            showProgressStr = "false"; // Force to false in last mode
            outputFileName = args[5];
          } else {
            System.err.println(
                "Error: Output file name is required when using 'file' destination with 'last' mode");
            printUsage();
            System.exit(1);
          }
        }
      } else if (outputDestination.equals("stdout")) {
        // For stdout, use defaults and ignore extra arguments
        if (args.length >= 5 && !isLastMode) {
          showProgressStr = args[4].toLowerCase();
        }
        // In last mode, force showProgress to false
        if (isLastMode) {
          showProgressStr = "false";
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

      // Store current export parameters for shutdown hook
      currentOutputDestination = outputDestination;
      currentOutputFile = outputFileName;

      // Set up shutdown hook for graceful interruption handling
      setupShutdownHook();

      // Record start time
      startTime.set(System.currentTimeMillis());

      // Create exporter instance
      currentExporter =
          Exporter.builder().tonDatabaseRootPath(tonDbRootPath).showProgress(showProgress).build();

      // Determine if output should be deserialized (json = true, boc = false)
      boolean deserialized = outputFormat.equals("json");

      System.err.println(
          "Starting export process... Press Ctrl+C to interrupt and show statistics.");

      // Execute appropriate export method based on destination and format
      if (isLastMode) {
        // Last mode: get only the last block
        handleLastMode(outputDestination, outputFormat, outputFileName);
      } else {
        // Normal mode: full export
        if (outputDestination.equals("file")) {
          if (outputFileName == null) {
            System.err.println("Error: Output file name is required for file output");
            printUsage();
            System.exit(1);
          }
          currentExporter.exportToFile(outputFileName, outputFormat.equals("json"), numOfThreads);
        } else {
          currentExporter.exportToStdout(outputFormat.equals("json"), numOfThreads);
        }
      }

      // If we reach here, export completed successfully
      exportCompleted.set(true);

      // Only print completion statistics if export was not interrupted
      if (!exportInterrupted.get()) {
        printFinalStatistics(true);
        System.err.println("Export completed successfully.");
      }

      // Remove shutdown hook since we completed successfully
      removeShutdownHook();

    } catch (NumberFormatException e) {
      System.err.println("Error: Invalid number format for threads: " + args[3]);
      printUsage();
      System.exit(1);
    } catch (Exception e) {
      if (exportInterrupted.get()) {
        // Export was interrupted, statistics already printed
        System.exit(130); // Standard exit code for Ctrl+C
      } else {
        System.err.println("Error during export: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  /** Sets up a shutdown hook to handle Ctrl+C gracefully and display final statistics. */
  private static void setupShutdownHook() {
    shutdownHook =
        new Thread(
            () -> {
              // Only show interrupted statistics if export was not completed successfully
              if (!exportCompleted.get() && !exportInterrupted.getAndSet(true)) {
                System.err.println(
                    "\n\nExport interrupted by user (Ctrl+C). Waiting for threads to finish...");

                // Wait for all threads to finish before showing statistics
                if (currentExporter != null) {
                  currentExporter.waitForThreadsToFinish();
                }

                System.err.println("All threads finished. Showing final statistics...");
                printFinalStatistics(false);
              }
            });
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /** Removes the shutdown hook when export completes successfully. */
  private static void removeShutdownHook() {
    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException e) {
        // Shutdown hook may have already been called, ignore
      }
    }
  }

  /**
   * Prints final export statistics including duration, file size, and success/error rates.
   *
   * @param completed true if export completed successfully, false if interrupted
   */
  private static void printFinalStatistics(boolean completed) {
    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime.get();
    double durationSeconds = durationMs / 1000.0;

    if (completed) {
      System.err.println("Status: COMPLETED");
    } else {
      System.err.println("Status: INTERRUPTED");
    }

    System.err.println("Duration: " + formatDuration(durationSeconds));

    // Show file size if exporting to file
    if ("file".equals(currentOutputDestination) && currentOutputFile != null) {
      File outputFile = new File(currentOutputFile);
      if (outputFile.exists()) {
        long fileSizeBytes = outputFile.length();
        String fileSizeFormatted = formatFileSize(fileSizeBytes);
        System.err.println("Output file: " + currentOutputFile);
        System.err.println("File size: " + fileSizeFormatted + " (" + fileSizeBytes + " bytes)");

        // Calculate throughput
        if (durationSeconds > 0) {
          double throughputMBps = (fileSizeBytes / (1024.0 * 1024.0)) / durationSeconds;
          System.err.println("Write throughput: " + String.format("%.2f MB/s", throughputMBps));
        }
      } else {
        System.err.println("Output file: " + currentOutputFile + " (file not found)");
      }
    } else {
      System.err.println("Output destination: stdout");
    }

    // Show success/error statistics if available from the exporter
    if (currentExporter != null) {
      int parsedBlocks = currentExporter.getParsedBlocksCount();
      int nonBlocks = currentExporter.getNonBlocksCount();
      int errors = currentExporter.getErrorsCount();
      int totalProcessed = currentExporter.getTotalProcessedCount();

      if (totalProcessed > 0) {
        double successRate = currentExporter.getSuccessRate();
        double errorRate = currentExporter.getErrorRate();

        System.err.println("Export statistics:");
        System.err.println(
            "  Blocks processed: "
                + parsedBlocks
                + " successful, "
                + errors
                + " errors, "
                + nonBlocks
                + " non-blocks");
        System.err.println("  Total processed: " + totalProcessed + " entries");
        System.err.println("  Success rate: " + String.format("%.4f%%", successRate));
        System.err.println("  Error rate: " + String.format("%.4f%%", errorRate));
      }
    }

    System.err.println("=".repeat(60));

    if (!completed) {
      System.err.println(
          "Note: Export was interrupted. You can resume the export by running the same command again.");
      System.err.println("The exporter will automatically resume from where it left off.");
    }
  }

  /**
   * Formats duration in seconds to a human-readable string (e.g., "2h 30m 45s")
   *
   * @param seconds the duration in seconds
   * @return formatted duration string
   */
  private static String formatDuration(double seconds) {
    if (seconds < 0) {
      return "N/A";
    }

    long totalSeconds = (long) seconds;
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long secs = totalSeconds % 60;
    long millis = (long) ((seconds - totalSeconds) * 1000);

    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes, secs);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, secs);
    } else if (secs > 0) {
      return String.format("%d.%03ds", secs, millis);
    } else {
      return String.format("%.3fs", seconds);
    }
  }

  /**
   * Formats file size in bytes to a human-readable string with appropriate units.
   *
   * @param bytes the file size in bytes
   * @return formatted file size string
   */
  private static String formatFileSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }

    String[] units = {"B", "KB", "MB", "GB", "TB"};
    int unitIndex = 0;
    double size = bytes;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return String.format("%.2f %s", size, units[unitIndex]);
  }

  /**
   * Handles the "last" mode functionality - gets only the last block from the database and outputs
   * it according to the specified format and destination.
   *
   * @param outputDestination "file" or "stdout"
   * @param outputFormat "json" or "boc"
   * @param outputFileName the output file name (null for stdout)
   * @throws IOException if an error occurs during processing
   */
  private static void handleLastMode(
      String outputDestination, String outputFormat, String outputFileName) throws IOException {
    try {
      if (outputFormat.equals("json")) {
        // Get last block as deserialized Block object
        Block block = currentExporter.getLast();
        if (block == null) {
          System.err.println("No last block found in database");
          return;
        }

        // Create JSON representation
        Gson gson =
            new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
                .create();
        String jsonOutput = gson.toJson(block);

        // Output to file or stdout
        if (outputDestination.equals("file")) {
          try (PrintWriter writer =
              new PrintWriter(new FileWriter(outputFileName, StandardCharsets.UTF_8))) {
            writer.println(jsonOutput);
          }
        } else {
          System.out.println(jsonOutput);
          // When stdout AND json is specified, show additional information at the end
          System.out.printf(
              "(%s,%s,%s) txs %s, msgs %s%n",
              block.getBlockInfo().getShard().getWorkchain(),
              block.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
              block.getBlockInfo().getSeqno(),
              block.getAllTransactions().size(),
              block.getAllMessages().size());
        }

      } else {
        // Get last block as BOC (Bag of Cells)
        byte[] bocData = currentExporter.getLastAsBoc();
        if (bocData == null) {
          System.err.println("No last block found in database");
          return;
        }

        String bocHex = Utils.bytesToHex(bocData);

        // Output to file or stdout
        if (outputDestination.equals("file")) {
          try (PrintWriter writer =
              new PrintWriter(new FileWriter(outputFileName, StandardCharsets.UTF_8))) {
            writer.println(bocHex);
          }
        } else {
          System.out.println(bocHex);
        }
      }

    } catch (Exception e) {
      throw new IOException("Error in last mode: " + e.getMessage(), e);
    }
  }

  private static void printVersion() {
    Properties properties = new Properties();
    try (InputStream inputStream =
        TonExporterApp.class.getClassLoader().getResourceAsStream("application.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
        String version = properties.getProperty("app.version", "unknown");
        String appName = properties.getProperty("app.name", "TonExporterApp");
        System.out.println(appName + " version " + version);
      } else {
        System.out.println("TonExporterApp version unknown (properties file not found)");
      }
    } catch (IOException e) {
      System.out.println(
          "TonExporterApp version unknown (error reading properties: " + e.getMessage() + ")");
    }
  }

  private static void printUsage() {
    System.err.println("Usage:");
    System.err.println("  For version: java -jar TonExporterApp -v");
    System.err.println(
        "  For file output: java -jar TonExporterApp.jar <ton-db-root-path> file <json|boc> <num-of-threads> <true|false> <output-file-name> [last]");
    System.err.println(
        "  For stdout output: java -jar TonExporterApp.jar <ton-db-root-path> stdout <json|boc> <num-of-threads> [<true|false>] [last]");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println("  -v                : Show version information");
    System.err.println("  ton-db-root-path  : Path to the TON database root directory");
    System.err.println("  file|stdout       : Output destination (file or stdout)");
    System.err.println(
        "  json|boc         : Output format (json for deserialized, boc for raw hex)");
    System.err.println("  num-of-threads   : Number of parallel threads to use");
    System.err.println("  true|false       : Whether to show progress information during export");
    System.err.println(
        "  output-file-name : Name of the output file (required only for file output)");
    System.err.println(
        "  last             : Optional flag to get only the last block (ignores progress and uses 1 thread)");
    System.err.println();
    System.err.println("Last Mode:");
    System.err.println("  When 'last' is specified as the final argument:");
    System.err.println("  - Progress display is ignored and set to false");
    System.err.println("  - Number of threads is ignored and set to 1");
    System.err.println("  - Only the most recent block is retrieved and output");
    System.err.println(
        "  - For stdout + json: additional info is shown (seqno, transactions count, messages count)");
    System.err.println();
    System.err.println("Examples:");
    System.err.println("  java -jar TonExporterApp.jar -v");
    System.err.println(
        "  java -jar TonExporterApp.jar /var/ton-work/db file json 4 true blocks.json");
    System.err.println("  java -jar TonExporterApp.jar /var/ton-work/db stdout boc 8");
    System.err.println(
        "  java -jar TonExporterApp.jar /var/ton-work/db file json 1 false last_block.json last");
    System.err.println("  java -jar TonExporterApp.jar /var/ton-work/db stdout json 1 last");
  }
}
