package com.smartcloudsolutions.tollingvision.samples;

import com.google.protobuf.ByteString;
import com.smartcloudsolutions.tollingvision.EventRequest;
import com.smartcloudsolutions.tollingvision.EventResponse;
import com.smartcloudsolutions.tollingvision.EventResult;
import com.smartcloudsolutions.tollingvision.PartialResult;
import com.smartcloudsolutions.tollingvision.Status;
import com.smartcloudsolutions.tollingvision.TollingVisionServiceGrpc;
import com.smartcloudsolutions.tollingvision.TollingVisionServiceGrpc.TollingVisionServiceStub;
import com.smartcloudsolutions.tollingvision.samples.model.ImageGroupResult;
import com.smartcloudsolutions.tollingvision.samples.ui.MainScreen;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

/**
 * TollingVision Analysis Client - Main application class. Handles JavaFX startup and coordinates
 * between UI and processing logic.
 */
public class AnalysisSampleApp extends Application {

  private ResourceBundle messages;
  private MainScreen mainScreen;
  private volatile boolean stopRequested = false;
  private ManagedChannel currentChannel = null;
  private Task<Void> currentTask = null;
  private BufferedWriter csvWriter = null;

  /**
   * Starts the JavaFX application and initializes the main screen.
   *
   * @param primaryStage the primary stage for this application
   */
  @Override
  public void start(Stage primaryStage) {
    // Set up file logging for Windows MSI (no console available)
    setupFileLogging();

    // Load resource bundle
    messages = ResourceBundle.getBundle("messages");

    // Create main screen
    mainScreen = new MainScreen(primaryStage, messages);

    // Set up processing handlers
    mainScreen.setOnStartProcessing(this::startProcessing);
    mainScreen.setOnStopProcessing(this::stopProcessing);

    // Set up application close handler to save configuration
    primaryStage.setOnCloseRequest(
        event -> {
          mainScreen.saveConfiguration();
        });

    // Show the main screen
    mainScreen.show();
  }

  private void stopProcessing() {
    stopRequested = true;
    log("Stop requested - cancelling processing...");

    // Immediately flush and close CSV writer
    if (csvWriter != null) {
      try {
        csvWriter.flush();
        csvWriter.close();
        csvWriter = null;
      } catch (IOException e) {
        log("Error closing CSV writer: " + e.getMessage());
      }
    }

    // Cancel current task
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }

    // Close gRPC channel
    if (currentChannel != null && !currentChannel.isShutdown()) {
      currentChannel.shutdownNow();
    }

    // Update UI to stopped state
    Platform.runLater(
        () -> {
          mainScreen.processingProperty().set(false);
          mainScreen.getProgressBar().progressProperty().unbind();
          mainScreen.getProgressBar().setProgress(0);
          mainScreen.getProgressBar().setVisible(false);
          log("Processing stopped by user");
        });
  }

  private void startProcessing() {
    stopRequested = false; // Reset stop flag

    // Save current configuration before starting processing
    mainScreen.saveConfiguration();

    mainScreen.processingProperty().set(true);
    mainScreen.getLogItems().clear();
    resetCounters();
    mainScreen.getProgressBar().setVisible(true);

    File folder = new File(mainScreen.getInputFolder());
    String url = mainScreen.getServiceUrl();
    boolean tls = mainScreen.isTlsEnabled();
    boolean insecure = mainScreen.isInsecureAllowed();
    File csvOut = new File(mainScreen.getCsvOutput());
    int maxPar = mainScreen.getMaxParallel();
    String[] patterns = {
      mainScreen.getGroupPattern(),
      mainScreen.getFrontPattern(),
      mainScreen.getRearPattern(),
      mainScreen.getOverviewPattern()
    };

    ManagedChannel ch0 = null;
    TollingVisionServiceStub stub0 = null;
    try {
      ch0 =
          tls
              ? insecure
                  ? NettyChannelBuilder.forTarget(url)
                      .useTransportSecurity()
                      .sslContext(
                          GrpcSslContexts.forClient()
                              .trustManager(InsecureTrustManagerFactory.INSTANCE)
                              .build())
                      .build()
                  : NettyChannelBuilder.forTarget(url).useTransportSecurity().build()
              : ManagedChannelBuilder.forTarget(url).usePlaintext().build();
      stub0 = TollingVisionServiceGrpc.newStub(ch0);
    } catch (Exception ex) {
      log(String.format(messages.getString("message.error.connection"), ex.getMessage()));
      mainScreen.processingProperty().set(false);
      return;
    }
    final ManagedChannel ch = ch0;
    currentChannel = ch; // Store reference for stopping
    final TollingVisionServiceStub stub = stub0;
    currentTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              validate(folder, csvOut, patterns);
              Map<String, List<Path>> buckets = null;
              try {
                buckets = bucketize(folder.toPath(), Pattern.compile(patterns[0]));
              } catch (Exception e) {
                log(
                    "ERROR during file grouping: "
                        + e.getClass().getName()
                        + ": "
                        + e.getMessage());
                e.printStackTrace();
                return null;
              }

              int total = buckets.size();
              Platform.runLater(() -> mainScreen.groupsDiscoveredProperty().set(total));
              log(String.format("Discovered %d groups from pattern: %s", total, patterns[0]));

              if (total == 0) {
                log(messages.getString("message.no.buckets"));
                return null;
              }

              log(String.format(messages.getString("message.processing.start"), total, maxPar));

              // Initialize CSV writer and write header
              initializeCsvWriter(csvOut.toPath());

              Semaphore sem = new Semaphore(maxPar);
              CountDownLatch done = new CountDownLatch(total);
              AtomicInteger cnt = new AtomicInteger();

              for (var e : buckets.entrySet()) {
                if (stopRequested) {
                  log("Processing stopped - remaining tasks cancelled");
                  break;
                }

                String bucket = e.getKey();
                List<Path> imgs = e.getValue();
                Thread.startVirtualThread(
                    () -> {
                      try {
                        if (stopRequested) {
                          done.countDown();
                          sem.release();
                          return;
                        }

                        sem.acquire();

                        if (stopRequested) {
                          done.countDown();
                          sem.release();
                          return;
                        }

                        EventRequest eventRequest =
                            buildReq(imgs, patterns[1], patterns[2], patterns[3]);
                        if (eventRequest == null) {
                          log("Skip " + bucket);
                          done.countDown();
                          sem.release();
                          return;
                        }

                        Platform.runLater(
                            () ->
                                mainScreen
                                    .requestsSentProperty()
                                    .set(mainScreen.requestsSentProperty().get() + 1));

                        stub.analyze(
                            eventRequest,
                            new StreamObserver<>() {
                              EventResult eventResult = null;
                              List<PartialResult> partialResults = new ArrayList<>();

                              @Override
                              public void onNext(EventResponse r) {
                                if (r.hasEventResult()) {
                                  eventResult = r.getEventResult();
                                } else if (r.hasPartialResult()) {
                                  PartialResult partial = r.getPartialResult();
                                  if (partial.hasResult()
                                      && partial.getResult().getStatus() == Status.RESULT) {
                                    partialResults.add(partial.toBuilder().build());
                                  }
                                }
                              }

                              @Override
                              public void onError(Throwable t) {
                                if (!stopRequested) {
                                  log("ERR " + bucket + ": " + t.getMessage());
                                  Platform.runLater(
                                      () ->
                                          mainScreen
                                              .responsesErrorProperty()
                                              .set(mainScreen.responsesErrorProperty().get() + 1));
                                }
                                done.countDown();
                                sem.release();
                              }

                              @Override
                              public void onCompleted() {
                                if (eventResult != null && !stopRequested) {
                                  ImageGroupResult result =
                                      new ImageGroupResult(
                                          bucket,
                                          eventResult,
                                          imgs,
                                          patterns[1],
                                          patterns[2],
                                          patterns[3]);

                                  // Create SearchResponse data from EventResult for each image
                                  // This extracts the analysis data and associates it with
                                  // individual images
                                  populateSearchResponseData(
                                      result, eventRequest, eventResult, partialResults, imgs);

                                  // Write CSV row immediately and flush
                                  writeCsvRowIncremental(result);

                                  Platform.runLater(
                                      () -> {
                                        mainScreen.getLogItems().add(result);
                                        mainScreen
                                            .responsesOkProperty()
                                            .set(mainScreen.responsesOkProperty().get() + 1);
                                      });
                                  updateProgress(cnt.incrementAndGet(), total);
                                }
                                done.countDown();
                                sem.release();
                              }
                            });
                      } catch (InterruptedException | IOException ignored) {
                        done.countDown();
                        sem.release();
                      }
                    });
              }

              done.await();

              // Close CSV writer
              if (csvWriter != null) {
                try {
                  csvWriter.flush();
                  csvWriter.close();
                  csvWriter = null;
                  if (!stopRequested) {
                    log(String.format(messages.getString("message.csv.saved"), csvOut.toString()));
                  }
                } catch (IOException e) {
                  log("Error closing CSV writer: " + e.getMessage());
                }
              }
            } catch (Exception ex) {
              log("ERROR: " + ex.getClass().getName() + ": " + ex.getMessage());
              ex.printStackTrace(); // Print to stderr for MSI debugging
              // Also log stack trace to UI for visibility
              StringBuilder stackTrace = new StringBuilder();
              stackTrace
                  .append(ex.getClass().getName())
                  .append(": ")
                  .append(ex.getMessage())
                  .append("\n");
              for (StackTraceElement element : ex.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
                if (stackTrace.length() > 500) break; // Limit size
              }
              log("Stack trace: " + stackTrace.toString());
            }
            return null;
          }

          @Override
          protected void done() {
            // Ensure CSV writer is closed
            if (csvWriter != null) {
              try {
                csvWriter.flush();
                csvWriter.close();
                csvWriter = null;
              } catch (IOException e) {
                log("Error closing CSV writer in done(): " + e.getMessage());
              }
            }

            Platform.runLater(
                () -> {
                  mainScreen.processingProperty().set(false);
                  mainScreen.getProgressBar().progressProperty().unbind();
                  mainScreen.getProgressBar().setProgress(0);
                  mainScreen.getProgressBar().setVisible(false);
                });
            ch.shutdownNow();
          }
        };
    mainScreen.getProgressBar().progressProperty().bind(currentTask.progressProperty());
    new Thread(currentTask).start();
  }

  private void resetCounters() {
    mainScreen.groupsDiscoveredProperty().set(0);
    mainScreen.requestsSentProperty().set(0);
    mainScreen.responsesOkProperty().set(0);
    mainScreen.responsesErrorProperty().set(0);
  }

  // Helper methods for processing
  private Map<String, List<Path>> bucketize(Path root, Pattern pattern) throws IOException {
    Map<String, List<Path>> map = new HashMap<>();
    Files.walk(root)
        .filter(Files::isRegularFile)
        .forEach(
            f -> {
              Matcher m = pattern.matcher(f.getFileName().toString());
              if (m.find()) map.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(f);
            });
    return map;
  }

  private EventRequest buildReq(
      List<Path> files, String frontPattern, String rearPattern, String overviewPattern)
      throws IOException {
    Pattern fp = Pattern.compile(frontPattern);
    Pattern rp = Pattern.compile(rearPattern);
    Pattern op = Pattern.compile(overviewPattern);
    EventRequest.Builder b = EventRequest.newBuilder();
    int cnt = 0;
    for (Path p : files) {
      byte[] d;
      try {
        d = Files.readAllBytes(p);
      } catch (IOException e) {
        continue;
      }
      com.smartcloudsolutions.tollingvision.Image img =
          com.smartcloudsolutions.tollingvision.Image.newBuilder()
              .setName(p.getFileName().toString())
              .setData(ByteString.copyFrom(d))
              .build();
      String n = p.getFileName().toString();
      if (op.matcher(n).find()) {
        b.addOverviewImage(img);
        cnt++;
      } else if (fp.matcher(n).find()) {
        b.addFrontImage(img);
        cnt++;
      } else if (rp.matcher(n).find()) {
        b.addRearImage(img);
        cnt++;
      }
    }
    return cnt == 0 ? null : b.build();
  }

  private void initializeCsvWriter(Path out) throws IOException {
    // Create CSV writer and write header only once
    csvWriter = Files.newBufferedWriter(out);
    csvWriter.write(
        "Bucket,Front Images,Rear Images,Overview Images,Front Plate,Front Jurisdiction,Front Plate"
            + " Alt,Front Jurisdiction Alt,Rear Plate,Rear Jurisdiction,Rear Plate Alt,Rear"
            + " Jurisdiction Alt,MMR,MMR Alt\n");
    csvWriter.flush();
  }

  private synchronized void writeCsvRowIncremental(ImageGroupResult result) {
    if (csvWriter == null || result.getBucket().startsWith("[LOG]")) {
      return;
    }

    try {
      csvWriter.write(
          String.format(
              "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
              escapeCSV(result.getBucket()),
              escapeCSV(result.getFrontNames()),
              escapeCSV(result.getRearNames()),
              escapeCSV(result.getOverNames()),
              escapeCSV(result.getFrontPlate()),
              escapeCSV(result.getFrontJurisdiction()),
              escapeCSV(result.getFrontAlt()),
              escapeCSV(result.getFrontJurisdictionAlt()),
              escapeCSV(result.getRearPlate()),
              escapeCSV(result.getRearJurisdiction()),
              escapeCSV(result.getRearAlt()),
              escapeCSV(result.getRearJurisdictionAlt()),
              escapeCSV(result.getMmr()),
              escapeCSV(result.getMmrAlt())));
      csvWriter.flush(); // Ensure data is durable on disk
    } catch (IOException e) {
      log("Error writing CSV row: " + e.getMessage());
    }
  }

  private String escapeCSV(String value) {
    if (value == null) return "";
    // Escape quotes by doubling them
    return value.replace("\"", "\"\"");
  }

  private void validate(File dir, File csv, String[] patterns) {
    if (!dir.isDirectory())
      throw new IllegalArgumentException(messages.getString("message.error.folder.invalid"));
    if (csv.getParentFile() != null && !csv.getParentFile().exists())
      throw new IllegalArgumentException(messages.getString("message.error.csv.directory"));
    for (int i = 0; i < patterns.length; i++)
      if (patterns[i].isBlank())
        throw new IllegalArgumentException(
            String.format(messages.getString("message.error.regex.empty"), String.valueOf(i + 1)));
  }

  private void log(String msg) {
    Platform.runLater(() -> mainScreen.getLogItems().add(new ImageGroupResult("[LOG]", msg)));
  }

  private void populateSearchResponseData(
      ImageGroupResult result,
      EventRequest eventRequest,
      EventResult eventResult,
      List<PartialResult> partialResults,
      List<Path> images) {
    // Create SearchResponse objects from EventResult data
    // Only create vehicle data if we have actual detection results - no hardcoded
    // boxes

    for (Path imagePath : images) {
      eventRequest.getOverviewImageList().stream()
          .filter(img -> img.getName().equals(imagePath.getFileName().toString()))
          .findFirst()
          .ifPresent(
              img -> {
                int idx = eventRequest.getOverviewImageList().indexOf(img);
                PartialResult pr =
                    partialResults.stream()
                        .filter(
                            pr0 ->
                                pr0.hasResult()
                                    && pr0.getResultType()
                                        == com.smartcloudsolutions.tollingvision.ResultType.OVERVIEW
                                    && pr0.getResultIndex() == idx)
                        .findFirst()
                        .orElse(null);
                if (pr != null) {
                  result.addImageAnalysis(imagePath, pr.getResult());
                }
              });

      eventRequest.getFrontImageList().stream()
          .filter(img -> img.getName().equals(imagePath.getFileName().toString()))
          .findFirst()
          .ifPresent(
              img -> {
                int idx = eventRequest.getFrontImageList().indexOf(img);
                PartialResult pr =
                    partialResults.stream()
                        .filter(
                            pr0 ->
                                pr0.hasResult()
                                    && pr0.getResultType()
                                        == com.smartcloudsolutions.tollingvision.ResultType.FRONT
                                    && pr0.getResultIndex() == idx)
                        .findFirst()
                        .orElse(null);
                if (pr != null) {
                  result.addImageAnalysis(imagePath, pr.getResult());
                }
              });

      eventRequest.getRearImageList().stream()
          .filter(img -> img.getName().equals(imagePath.getFileName().toString()))
          .findFirst()
          .ifPresent(
              img -> {
                int idx = eventRequest.getRearImageList().indexOf(img);
                PartialResult pr =
                    partialResults.stream()
                        .filter(
                            pr0 ->
                                pr0.hasResult()
                                    && pr0.getResultType()
                                        == com.smartcloudsolutions.tollingvision.ResultType.REAR
                                    && pr0.getResultIndex() == idx)
                        .findFirst()
                        .orElse(null);
                if (pr != null) {
                  result.addImageAnalysis(imagePath, pr.getResult());
                }
              });
    }
  }

  /**
   * Sets up file logging to capture console output when running from Windows MSI installer (where
   * no console is available). Logs are written to user home directory.
   */
  private void setupFileLogging() {
    try {
      // Get user home directory for log file
      String userHome = System.getProperty("user.home");
      Path logDir = Path.of(userHome, ".tollingvision-client");
      Files.createDirectories(logDir);

      Path logFile = logDir.resolve("application.log");

      // Set up java.util.logging to write to file
      java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
      java.util.logging.FileHandler fileHandler =
          new java.util.logging.FileHandler(
              logFile.toString(),
              1024 * 1024, // 1 MB
              3, // 3 rotating files
              true // append mode
              );
      fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
      rootLogger.addHandler(fileHandler);

      // Also redirect System.out and System.err to the log file
      java.io.PrintStream logStream =
          new java.io.PrintStream(new java.io.FileOutputStream(logFile.toFile(), true), true);
      System.setOut(logStream);
      System.setErr(logStream);

      System.out.println("=== Application started at " + java.time.LocalDateTime.now() + " ===");
      System.out.println("Log file: " + logFile.toString());

    } catch (Exception e) {
      // If logging setup fails, just print to stderr (may not be visible in MSI)
      e.printStackTrace();
    }
  }

  /**
   * Main entry point for the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    launch(args);
  }
}
