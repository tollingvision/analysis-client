package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive logging system for pattern builder validation and debugging. Provides structured
 * logging for errors, warnings, and user actions to support debugging and user assistance.
 */
public class ValidationLogger {

  private static final Logger LOGGER = Logger.getLogger(ValidationLogger.class.getName());
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  // In-memory log for debugging and support
  private static final List<LogEntry> LOG_ENTRIES = new ArrayList<>();
  private static final int MAX_LOG_ENTRIES = 1000;

  /** Represents a log entry with timestamp and context. */
  public static class LogEntry {
    private final LocalDateTime timestamp;
    private final Level level;
    private final String category;
    private final String message;
    private final String context;

    public LogEntry(Level level, String category, String message, String context) {
      this.timestamp = LocalDateTime.now();
      this.level = level;
      this.category = category;
      this.message = message;
      this.context = context;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public Level getLevel() {
      return level;
    }

    public String getCategory() {
      return category;
    }

    public String getMessage() {
      return message;
    }

    public String getContext() {
      return context;
    }

    @Override
    public String toString() {
      String contextStr = context != null ? " [" + context + "]" : "";
      return String.format(
          "%s [%s] %s: %s%s",
          timestamp.format(TIMESTAMP_FORMAT), level.getName(), category, message, contextStr);
    }
  }

  /**
   * Logs a validation error with context.
   *
   * @param error the validation error
   * @param context additional context information
   */
  public static void logValidationError(ValidationError error, String context) {
    String message =
        String.format("Validation error: %s - %s", error.getType(), error.getMessage());

    LogEntry entry = new LogEntry(Level.SEVERE, "VALIDATION", message, context);
    addLogEntry(entry);

    LOGGER.severe(entry.toString());
  }

  /**
   * Logs a validation warning with context.
   *
   * @param warning the validation warning
   * @param context additional context information
   */
  public static void logValidationWarning(ValidationWarning warning, String context) {
    String message =
        String.format("Validation warning: %s - %s", warning.getType(), warning.getMessage());

    LogEntry entry = new LogEntry(Level.WARNING, "VALIDATION", message, context);
    addLogEntry(entry);

    LOGGER.warning(entry.toString());
  }

  /**
   * Logs a user action for debugging purposes.
   *
   * @param action the user action
   * @param details additional details about the action
   */
  public static void logUserAction(String action, String details) {
    String message = String.format("User action: %s", action);

    LogEntry entry = new LogEntry(Level.INFO, "USER_ACTION", message, details);
    addLogEntry(entry);

    LOGGER.info(entry.toString());
  }

  /**
   * Logs a configuration change.
   *
   * @param component the component that changed
   * @param oldValue the previous value
   * @param newValue the new value
   */
  public static void logConfigurationChange(String component, String oldValue, String newValue) {
    String message = String.format("Configuration changed: %s", component);
    String context = String.format("'%s' → '%s'", oldValue, newValue);

    LogEntry entry = new LogEntry(Level.INFO, "CONFIG_CHANGE", message, context);
    addLogEntry(entry);

    LOGGER.info(entry.toString());
  }

  /**
   * Logs pattern generation results.
   *
   * @param patternType the type of pattern generated
   * @param pattern the generated pattern
   * @param success whether generation was successful
   */
  public static void logPatternGeneration(String patternType, String pattern, boolean success) {
    Level level = success ? Level.INFO : Level.WARNING;
    String message =
        String.format("Pattern generation %s: %s", success ? "succeeded" : "failed", patternType);

    LogEntry entry = new LogEntry(level, "PATTERN_GEN", message, pattern);
    addLogEntry(entry);

    if (success) {
      LOGGER.info(entry.toString());
    } else {
      LOGGER.warning(entry.toString());
    }
  }

  /**
   * Logs file analysis results.
   *
   * @param fileCount number of files analyzed
   * @param tokenCount number of tokens detected
   * @param duration analysis duration in milliseconds
   */
  public static void logFileAnalysis(int fileCount, int tokenCount, long duration) {
    String message =
        String.format("File analysis completed: %d files, %d tokens", fileCount, tokenCount);
    String context = String.format("Duration: %dms", duration);

    LogEntry entry = new LogEntry(Level.INFO, "FILE_ANALYSIS", message, context);
    addLogEntry(entry);

    LOGGER.info(entry.toString());
  }

  /**
   * Logs preview update results.
   *
   * @param totalFiles total number of files in preview
   * @param matchedFiles number of successfully matched files
   * @param groupCount number of groups found
   */
  public static void logPreviewUpdate(int totalFiles, int matchedFiles, int groupCount) {
    String message =
        String.format(
            "Preview updated: %d/%d files matched, %d groups",
            matchedFiles, totalFiles, groupCount);

    LogEntry entry = new LogEntry(Level.INFO, "PREVIEW_UPDATE", message, null);
    addLogEntry(entry);

    LOGGER.fine(entry.toString());
  }

  /**
   * Logs an exception with context.
   *
   * @param exception the exception that occurred
   * @param context the context in which the exception occurred
   */
  public static void logException(Exception exception, String context) {
    String message = String.format("Exception occurred: %s", exception.getMessage());

    LogEntry entry = new LogEntry(Level.SEVERE, "EXCEPTION", message, context);
    addLogEntry(entry);

    LOGGER.log(Level.SEVERE, entry.toString(), exception);
  }

  /**
   * Logs performance metrics.
   *
   * @param operation the operation being measured
   * @param duration duration in milliseconds
   * @param details additional performance details
   */
  public static void logPerformance(String operation, long duration, String details) {
    String message = String.format("Performance: %s took %dms", operation, duration);

    LogEntry entry = new LogEntry(Level.FINE, "PERFORMANCE", message, details);
    addLogEntry(entry);

    LOGGER.fine(entry.toString());
  }

  /**
   * Gets recent log entries for debugging.
   *
   * @param maxEntries maximum number of entries to return
   * @return list of recent log entries
   */
  public static List<LogEntry> getRecentLogEntries(int maxEntries) {
    synchronized (LOG_ENTRIES) {
      int startIndex = Math.max(0, LOG_ENTRIES.size() - maxEntries);
      return new ArrayList<>(LOG_ENTRIES.subList(startIndex, LOG_ENTRIES.size()));
    }
  }

  /**
   * Gets log entries for a specific category.
   *
   * @param category the log category to filter by
   * @param maxEntries maximum number of entries to return
   * @return list of log entries for the category
   */
  public static List<LogEntry> getLogEntriesByCategory(String category, int maxEntries) {
    synchronized (LOG_ENTRIES) {
      return LOG_ENTRIES.stream()
          .filter(entry -> category.equals(entry.getCategory()))
          .skip(Math.max(0, LOG_ENTRIES.size() - maxEntries))
          .toList();
    }
  }

  /** Clears the in-memory log entries. */
  public static void clearLogEntries() {
    synchronized (LOG_ENTRIES) {
      LOG_ENTRIES.clear();
    }
  }

  /**
   * Gets a summary of recent validation issues.
   *
   * @return summary of errors and warnings
   */
  public static String getValidationSummary() {
    synchronized (LOG_ENTRIES) {
      long errorCount =
          LOG_ENTRIES.stream()
              .filter(
                  entry ->
                      entry.getLevel() == Level.SEVERE && "VALIDATION".equals(entry.getCategory()))
              .count();

      long warningCount =
          LOG_ENTRIES.stream()
              .filter(
                  entry ->
                      entry.getLevel() == Level.WARNING && "VALIDATION".equals(entry.getCategory()))
              .count();

      if (errorCount == 0 && warningCount == 0) {
        return "No recent validation issues";
      }

      return String.format(
          "Recent validation issues: %d errors, %d warnings", errorCount, warningCount);
    }
  }

  /** Adds a log entry to the in-memory log, maintaining size limit. */
  private static void addLogEntry(LogEntry entry) {
    synchronized (LOG_ENTRIES) {
      LOG_ENTRIES.add(entry);

      // Maintain size limit
      while (LOG_ENTRIES.size() > MAX_LOG_ENTRIES) {
        LOG_ENTRIES.remove(0);
      }
    }
  }

  /**
   * Formats log entries for export or display.
   *
   * @param entries the log entries to format
   * @return formatted log text
   */
  public static String formatLogEntries(List<LogEntry> entries) {
    StringBuilder sb = new StringBuilder();
    sb.append("Pattern Builder Validation Log\n");
    sb.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\n");
    sb.append("=".repeat(50)).append("\n\n");

    for (LogEntry entry : entries) {
      sb.append(entry.toString()).append("\n");
    }

    return sb.toString();
  }
}
