package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javafx.concurrent.Task;

/**
 * Background service for performing file analysis operations without blocking the UI. Provides
 * cancellation support, progress tracking, and memory management for large datasets.
 */
public class BackgroundAnalysisService {

  private static final int MAX_FILES_FOR_ANALYSIS = 500;
  private static final long ESTIMATED_MEMORY_PER_FILE = 1024; // 1KB per filename

  private final FilenameTokenizer tokenizer;
  private final TokenizationCache cache;

  // Current running tasks
  private Task<?> currentAnalysisTask;
  private Task<?> currentPreviewTask;

  /**
   * Creates a new background analysis service.
   *
   * @param tokenizer the tokenizer to use for analysis
   * @param cache the cache to use for storing results
   */
  public BackgroundAnalysisService(FilenameTokenizer tokenizer, TokenizationCache cache) {
    this.tokenizer = tokenizer;
    this.cache = cache;
  }

  /**
   * Creates a task for analyzing files in a directory.
   *
   * @param directory the directory to analyze
   * @return JavaFX Task for background execution
   */
  public Task<TokenAnalysis> createFileAnalysisTask(Path directory) {
    return new Task<TokenAnalysis>() {
      @Override
      protected TokenAnalysis call() throws Exception {
        updateMessage("Loading files from directory...");
        updateProgress(0, 100);

        // Load image files with limit
        List<String> imageFiles = loadImageFiles(directory);

        if (isCancelled()) {
          return null;
        }

        if (imageFiles.isEmpty()) {
          throw new RuntimeException("No image files found in directory");
        }

        updateMessage("Analyzing " + imageFiles.size() + " files...");
        updateProgress(10, 100);

        // Check cache first
        String cacheKey = cache.generateFilenamesKey(imageFiles);
        TokenAnalysis cachedAnalysis = cache.getCachedAnalysis(cacheKey);

        if (cachedAnalysis != null) {
          updateMessage("Using cached analysis results");
          updateProgress(100, 100);
          return cachedAnalysis;
        }

        // Perform analysis with progress tracking
        TokenAnalysis analysis = performAnalysisWithProgress(imageFiles);

        if (isCancelled()) {
          return null;
        }

        // Cache the results
        cache.cacheAnalysis(cacheKey, analysis);

        updateMessage("Analysis complete");
        updateProgress(100, 100);

        return analysis;
      }

      /** Loads image files from directory with size limit based on memory availability. */
      private List<String> loadImageFiles(Path directory) throws IOException {
        // Calculate memory-based limit
        int memoryBasedLimit = MemoryManager.getRecommendedMaxItems(ESTIMATED_MEMORY_PER_FILE);
        int effectiveLimit = Math.min(MAX_FILES_FOR_ANALYSIS, memoryBasedLimit);

        updateMessage("Loading files (limit: " + effectiveLimit + " based on available memory)...");

        try (Stream<Path> files = Files.list(directory)) {
          return files
              .filter(Files::isRegularFile)
              .map(Path::getFileName)
              .map(Path::toString)
              .filter(BackgroundAnalysisService.this::isImageFile)
              .limit(effectiveLimit)
              .toList();
        }
      }

      /** Performs analysis with progress updates. */
      private TokenAnalysis performAnalysisWithProgress(List<String> filenames) {
        // First pass: tokenize individual files with caching
        updateMessage("Tokenizing filenames...");

        List<String> uncachedFiles = new ArrayList<>();
        for (int i = 0; i < filenames.size(); i++) {
          if (isCancelled()) {
            return null;
          }

          String filename = filenames.get(i);
          List<FilenameToken> cachedTokens = cache.getCachedTokens(filename);

          if (cachedTokens == null) {
            uncachedFiles.add(filename);
          }

          // Update progress for tokenization phase (10-60%)
          updateProgress(10 + (i * 50 / filenames.size()), 100);
        }

        // Tokenize uncached files with memory monitoring
        if (!uncachedFiles.isEmpty()) {
          updateMessage("Tokenizing " + uncachedFiles.size() + " new files...");

          for (int i = 0; i < uncachedFiles.size(); i++) {
            if (isCancelled()) {
              return null;
            }

            // Check memory before processing more files
            if (MemoryManager.isMemoryCritical()) {
              updateMessage("Memory critically low, suggesting garbage collection...");
              MemoryManager.suggestGarbageCollection();

              // Wait a bit for GC to complete
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
              }
            }

            String filename = uncachedFiles.get(i);
            List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);
            cache.cacheTokens(filename, tokens);

            // Update progress within tokenization
            updateProgress(60 + (i * 20 / uncachedFiles.size()), 100);
          }
        }

        // Second pass: perform full analysis
        updateMessage("Analyzing patterns and generating suggestions...");
        updateProgress(80, 100);

        if (isCancelled()) {
          return null;
        }

        // Use the tokenizer's analysis method
        TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

        updateProgress(95, 100);
        return analysis;
      }
    };
  }

  /**
   * Creates a task for updating preview with incremental processing.
   *
   * @param config the pattern configuration
   * @param filenames the filenames to process
   * @param maxPreviewItems maximum items to include in preview
   * @return JavaFX Task for background execution
   */
  public Task<List<FilenamePreview>> createPreviewUpdateTask(
      PatternConfiguration config, List<String> filenames, int maxPreviewItems) {

    return new Task<List<FilenamePreview>>() {
      @Override
      protected List<FilenamePreview> call() throws Exception {
        updateMessage("Processing preview...");
        updateProgress(0, 100);

        List<FilenamePreview> previews = new ArrayList<>();
        RuleEngine ruleEngine = new RuleEngine();

        // Limit files for performance
        List<String> limitedFiles =
            filenames.size() > maxPreviewItems ? filenames.subList(0, maxPreviewItems) : filenames;

        // Process files in batches for better progress feedback
        int batchSize = Math.max(1, limitedFiles.size() / 20); // 20 progress updates

        for (int i = 0; i < limitedFiles.size(); i += batchSize) {
          if (isCancelled()) {
            return null;
          }

          int endIndex = Math.min(i + batchSize, limitedFiles.size());
          List<String> batch = limitedFiles.subList(i, endIndex);

          // Process batch
          for (String filename : batch) {
            if (isCancelled()) {
              return null;
            }

            FilenamePreview preview = processFilenameForPreview(filename, config, ruleEngine);
            previews.add(preview);
          }

          // Update progress
          updateProgress((i + batchSize) * 100 / limitedFiles.size(), 100);
          updateMessage(
              String.format(
                  "Processed %d of %d files",
                  Math.min(i + batchSize, limitedFiles.size()), limitedFiles.size()));
        }

        updateMessage("Preview processing complete");
        updateProgress(100, 100);

        return previews;
      }
    };
  }

  /** Processes a single filename for preview display. */
  private FilenamePreview processFilenameForPreview(
      String filename, PatternConfiguration config, RuleEngine ruleEngine) {
    FilenamePreview preview = new FilenamePreview(filename);

    try {
      // Extract group ID using pattern
      String groupId = extractGroupId(filename, config.getGroupPattern());

      // Classify role using rules
      ImageRole role = null;
      if (!config.getRoleRules().isEmpty()) {
        role = ruleEngine.classifyFilename(filename, config.getRoleRules());
      }

      // Determine success
      boolean matched = groupId != null && !groupId.trim().isEmpty();

      if (matched) {
        preview.setSuccess(groupId, role);
      } else {
        preview.setError("Could not extract group ID");
      }

    } catch (Exception e) {
      preview.setError("Processing error: " + e.getMessage());
    }

    return preview;
  }

  /** Extracts group ID from filename using regex pattern. */
  private String extractGroupId(String filename, String groupPattern) {
    if (groupPattern == null || groupPattern.trim().isEmpty()) {
      return null;
    }

    try {
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(groupPattern);
      java.util.regex.Matcher matcher = pattern.matcher(filename);

      if (matcher.find() && matcher.groupCount() >= 1) {
        return matcher.group(1);
      }
    } catch (java.util.regex.PatternSyntaxException e) {
      return null;
    }

    return null;
  }

  /**
   * Executes a file analysis task in the background.
   *
   * @param task the task to execute
   * @return the task being executed
   */
  public Task<TokenAnalysis> executeAnalysisTask(Task<TokenAnalysis> task) {
    // Cancel any existing analysis task
    if (currentAnalysisTask != null && !currentAnalysisTask.isDone()) {
      currentAnalysisTask.cancel(true);
    }

    // Execute the task in a virtual thread
    Thread.startVirtualThread(task);
    currentAnalysisTask = task;

    return task;
  }

  /**
   * Executes a preview update task in the background.
   *
   * @param task the task to execute
   * @return the task being executed
   */
  public Task<List<FilenamePreview>> executePreviewTask(Task<List<FilenamePreview>> task) {
    // Cancel any existing preview task
    if (currentPreviewTask != null && !currentPreviewTask.isDone()) {
      currentPreviewTask.cancel(true);
    }

    // Execute the task in a virtual thread
    Thread.startVirtualThread(task);
    currentPreviewTask = task;

    return task;
  }

  /** Cancels all running background tasks. */
  public void cancelAllTasks() {
    if (currentAnalysisTask != null && !currentAnalysisTask.isDone()) {
      currentAnalysisTask.cancel(true);
    }

    if (currentPreviewTask != null && !currentPreviewTask.isDone()) {
      currentPreviewTask.cancel(true);
    }
  }

  /** Checks if a filename represents an image file. */
  private boolean isImageFile(String filename) {
    String lower = filename.toLowerCase();
    return lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".png")
        || lower.endsWith(".bmp")
        || lower.endsWith(".tiff")
        || lower.endsWith(".gif");
  }

  /**
   * Gets the maximum number of files that will be analyzed.
   *
   * @return maximum file limit
   */
  public static int getMaxFilesForAnalysis() {
    return MAX_FILES_FOR_ANALYSIS;
  }

  /** Shuts down the background service and cancels all tasks. */
  public void shutdown() {
    cancelAllTasks();
    // Virtual threads don't need explicit shutdown
  }
}
