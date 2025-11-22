package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ValidationLogger functionality. */
class ValidationLoggerTest {

  @BeforeEach
  void setUp() {
    ValidationLogger.clearLogEntries();
  }

  @Test
  void testLogValidationError() {
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationLogger.logValidationError(error, "Test context");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("VALIDATION", entry.getCategory());
    assertTrue(entry.getMessage().contains("NO_GROUP_ID_SELECTED"));
    assertEquals("Test context", entry.getContext());
  }

  @Test
  void testLogValidationWarning() {
    ValidationWarning warning = ValidationWarning.of(ValidationWarningType.UNMATCHED_FILES);
    ValidationLogger.logValidationWarning(warning, "Test context");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("VALIDATION", entry.getCategory());
    assertTrue(entry.getMessage().contains("UNMATCHED_FILES"));
  }

  @Test
  void testLogUserAction() {
    ValidationLogger.logUserAction("Button clicked", "Next button");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("USER_ACTION", entry.getCategory());
    assertTrue(entry.getMessage().contains("Button clicked"));
    assertEquals("Next button", entry.getContext());
  }

  @Test
  void testLogConfigurationChange() {
    ValidationLogger.logConfigurationChange("Group ID", "old_value", "new_value");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("CONFIG_CHANGE", entry.getCategory());
    assertTrue(entry.getMessage().contains("Group ID"));
    assertTrue(entry.getContext().contains("old_value"));
    assertTrue(entry.getContext().contains("new_value"));
  }

  @Test
  void testLogPatternGeneration() {
    ValidationLogger.logPatternGeneration("Group Pattern", "^test_(.+)_\\w+$", true);

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("PATTERN_GEN", entry.getCategory());
    assertTrue(entry.getMessage().contains("succeeded"));
    assertEquals("^test_(.+)_\\w+$", entry.getContext());
  }

  @Test
  void testLogFileAnalysis() {
    ValidationLogger.logFileAnalysis(100, 5, 1500);

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("FILE_ANALYSIS", entry.getCategory());
    assertTrue(entry.getMessage().contains("100 files"));
    assertTrue(entry.getMessage().contains("5 tokens"));
    assertTrue(entry.getContext().contains("1500ms"));
  }

  @Test
  void testLogPreviewUpdate() {
    ValidationLogger.logPreviewUpdate(50, 45, 10);

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("PREVIEW_UPDATE", entry.getCategory());
    assertTrue(entry.getMessage().contains("45/50 files matched"));
    assertTrue(entry.getMessage().contains("10 groups"));
  }

  @Test
  void testLogException() {
    Exception testException = new RuntimeException("Test exception");
    ValidationLogger.logException(testException, "Test operation");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    assertEquals(1, entries.size());

    ValidationLogger.LogEntry entry = entries.get(0);
    assertEquals("EXCEPTION", entry.getCategory());
    assertTrue(entry.getMessage().contains("Test exception"));
    assertEquals("Test operation", entry.getContext());
  }

  @Test
  void testLogEntriesByCategory() {
    ValidationLogger.logUserAction("Action 1", "Context 1");
    ValidationLogger.logUserAction("Action 2", "Context 2");
    ValidationLogger.logConfigurationChange("Config", "old", "new");

    List<ValidationLogger.LogEntry> userActions =
        ValidationLogger.getLogEntriesByCategory("USER_ACTION", 10);
    assertEquals(2, userActions.size());

    List<ValidationLogger.LogEntry> configChanges =
        ValidationLogger.getLogEntriesByCategory("CONFIG_CHANGE", 10);
    assertEquals(1, configChanges.size());
  }

  @Test
  void testValidationSummary() {
    // Initially no issues
    String summary = ValidationLogger.getValidationSummary();
    assertEquals("No recent validation issues", summary);

    // Add some validation issues
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationWarning warning = ValidationWarning.of(ValidationWarningType.UNMATCHED_FILES);

    ValidationLogger.logValidationError(error, "Test");
    ValidationLogger.logValidationWarning(warning, "Test");

    summary = ValidationLogger.getValidationSummary();
    assertTrue(summary.contains("1 errors"));
    assertTrue(summary.contains("1 warnings"));
  }

  @Test
  void testFormatLogEntries() {
    ValidationLogger.logUserAction("Test action", "Test context");
    ValidationLogger.logConfigurationChange("Test config", "old", "new");

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(10);
    String formatted = ValidationLogger.formatLogEntries(entries);

    assertTrue(formatted.contains("Pattern Builder Validation Log"));
    assertTrue(formatted.contains("Test action"));
    assertTrue(formatted.contains("Test config"));
  }

  @Test
  void testMaxLogEntries() {
    // Add more than the maximum number of entries
    for (int i = 0; i < 1100; i++) {
      ValidationLogger.logUserAction("Action " + i, "Context " + i);
    }

    List<ValidationLogger.LogEntry> entries = ValidationLogger.getRecentLogEntries(2000);
    // Should be limited to 1000 entries
    assertEquals(1000, entries.size());

    // Should contain the most recent entries
    assertTrue(entries.get(entries.size() - 1).getMessage().contains("Action 1099"));
  }
}
