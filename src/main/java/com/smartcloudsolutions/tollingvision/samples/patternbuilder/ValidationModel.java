package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * Observable shared validation state between Steps 2-4 in the pattern builder. Provides debounced
 * validation refresh, reactive updates, and consistent validation state management across all
 * pattern builder components.
 */
public class ValidationModel {

  private static final Duration DEBOUNCE_DELAY = Duration.millis(300);

  // Core validation state
  private final ObjectProperty<ValidationResult> validationResult = new SimpleObjectProperty<>();
  private final BooleanProperty validationInProgress = new SimpleBooleanProperty(false);
  private final BooleanProperty hasErrors = new SimpleBooleanProperty(false);
  private final BooleanProperty hasWarnings = new SimpleBooleanProperty(false);
  private final BooleanProperty isValid = new SimpleBooleanProperty(false);
  private final StringProperty validationSummary = new SimpleStringProperty();

  // Error and warning collections
  private final ObservableList<ValidationError> errors = FXCollections.observableArrayList();
  private final ObservableList<ValidationWarning> warnings = FXCollections.observableArrayList();

  // Configuration state
  private final ObjectProperty<PatternConfiguration> currentConfiguration =
      new SimpleObjectProperty<>();
  private final ObservableList<String> sampleFilenames = FXCollections.observableArrayList();

  // Services
  private final PatternGenerator patternGenerator = new PatternGenerator();

  // i18n resources
  private final java.util.ResourceBundle messages;

  // Debouncing
  private Timeline debounceTimeline;
  private Task<ValidationResult> currentValidationTask;

  /**
   * Creates a new ValidationModel with initial empty state.
   *
   * @param messages the resource bundle for i18n strings
   */
  public ValidationModel(java.util.ResourceBundle messages) {
    this.messages = messages;

    setupDebouncing();
    setupBindings();

    // Initialize with empty validation result
    validationSummary.set(messages.getString("status.select.folder"));
    updateValidationState(ValidationResult.success());
  }

  /** Sets up debounced validation refresh mechanism. */
  private void setupDebouncing() {
    debounceTimeline = new Timeline(new KeyFrame(DEBOUNCE_DELAY, e -> performValidation()));
    debounceTimeline.setCycleCount(1);
  }

  /** Sets up property bindings for derived validation state. */
  private void setupBindings() {
    // Update derived properties when validation result changes
    validationResult.addListener(
        (obs, oldResult, newResult) -> {
          if (newResult != null) {
            Platform.runLater(
                () -> {
                  hasErrors.set(newResult.hasErrors());
                  hasWarnings.set(newResult.hasWarnings());
                  isValid.set(newResult.isValid());

                  errors.setAll(newResult.getErrors());
                  warnings.setAll(newResult.getWarnings());

                  updateValidationSummary(newResult);
                });
          }
        });

    // Trigger validation when configuration changes
    currentConfiguration.addListener(
        (obs, oldConfig, newConfig) -> {
          requestValidationRefresh();
        });

    // Trigger validation when sample filenames change
    sampleFilenames.addListener(
        (javafx.collections.ListChangeListener<String>)
            c -> {
              requestValidationRefresh();
            });
  }

  /** Updates the validation summary text based on the validation result. */
  /**
   * Updates the validation summary text based on the validation result.
   *
   * @param result the validation result to generate summary for
   */
  private void updateValidationSummary(ValidationResult result) {
    if (result.hasErrors()) {
      if (result.getErrors().size() == 1) {
        validationSummary.set("✗ " + result.getErrors().get(0).getMessage());
      } else {
        validationSummary.set(
            String.format(
                "✗ " + messages.getString("status.validation.errors"), result.getErrors().size()));
      }
    } else if (result.hasWarnings()) {
      if (result.getWarnings().size() == 1) {
        validationSummary.set("⚠ " + result.getWarnings().get(0).getMessage());
      } else {
        validationSummary.set(
            String.format(
                "⚠ " + messages.getString("status.configuration.valid.warnings"),
                result.getWarnings().size()));
      }
    } else {
      validationSummary.set("✓ " + messages.getString("status.pattern.generated"));
    }
  }

  /**
   * Requests a debounced validation refresh. Multiple calls within the debounce period will be
   * coalesced into a single validation.
   */
  public void requestValidationRefresh() {
    // Cancel any pending validation
    debounceTimeline.stop();

    // Start new debounced validation
    debounceTimeline.playFromStart();
  }

  /**
   * Immediately performs validation without debouncing. Use this for critical validation updates
   * that need immediate feedback.
   */
  public void performImmediateValidation() {
    debounceTimeline.stop();
    performValidation();
  }

  /** Performs the actual validation in a background thread. */
  private void performValidation() {
    PatternConfiguration config = currentConfiguration.get();
    if (config == null) {
      updateValidationState(
          ValidationResult.failure(
              ValidationError.of(
                  ValidationErrorType.INVALID_RULE_CONFIGURATION, "No configuration available")));
      return;
    }

    // Cancel any running validation task
    if (currentValidationTask != null && !currentValidationTask.isDone()) {
      currentValidationTask.cancel(true);
    }

    // Set validation in progress
    Platform.runLater(() -> validationInProgress.set(true));

    // Create and execute validation task
    currentValidationTask = createValidationTask(config);
    Thread.startVirtualThread(currentValidationTask);
  }

  /** Creates a background validation task. */
  private Task<ValidationResult> createValidationTask(PatternConfiguration config) {
    return new Task<ValidationResult>() {
      @Override
      protected ValidationResult call() throws Exception {
        try {
          // Perform comprehensive validation
          ValidationResult result = patternGenerator.validatePatterns(config);

          // Add additional validation checks specific to the current context
          result = enhanceValidationWithContext(result, config);

          return result;
        } catch (Exception e) {
          ValidationLogger.logException(e, "Validation task failed");
          return ValidationResult.failure(
              ValidationError.of(
                  ValidationErrorType.INVALID_RULE_CONFIGURATION,
                  "Validation failed: " + e.getMessage()));
        }
      }

      @Override
      protected void succeeded() {
        Platform.runLater(
            () -> {
              validationInProgress.set(false);
              updateValidationState(getValue());
            });
      }

      @Override
      protected void failed() {
        Platform.runLater(
            () -> {
              validationInProgress.set(false);
              updateValidationState(
                  ValidationResult.failure(
                      ValidationError.of(
                          ValidationErrorType.INVALID_RULE_CONFIGURATION,
                          "Validation failed: " + getException().getMessage())));
            });
      }

      @Override
      protected void cancelled() {
        Platform.runLater(
            () -> {
              validationInProgress.set(false);
            });
      }
    };
  }

  /** Enhances validation result with additional context-specific checks. */
  private ValidationResult enhanceValidationWithContext(
      ValidationResult baseResult, PatternConfiguration config) {
    List<ValidationError> additionalErrors = new ArrayList<>();
    List<ValidationWarning> additionalWarnings = new ArrayList<>();

    // Check if we have sample filenames for testing
    if (sampleFilenames.isEmpty()) {
      additionalWarnings.add(
          ValidationWarning.of(
              ValidationWarningType.NO_SAMPLE_FILES,
              "No sample files available for pattern testing"));
    } else {
      // Test patterns against sample files
      ValidationResult sampleTestResult = testPatternsAgainstSamples(config);
      additionalErrors.addAll(sampleTestResult.getErrors());
      additionalWarnings.addAll(sampleTestResult.getWarnings());
    }

    // Combine results
    ValidationResult enhanced =
        new ValidationResult(
            baseResult.isValid() && additionalErrors.isEmpty(),
            combineErrors(baseResult.getErrors(), additionalErrors),
            combineWarnings(baseResult.getWarnings(), additionalWarnings));

    return enhanced;
  }

  /** Tests patterns against sample filenames to identify potential issues. */
  private ValidationResult testPatternsAgainstSamples(PatternConfiguration config) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();

    if (config.getGroupPattern() != null && !config.getGroupPattern().trim().isEmpty()) {
      try {
        // Test group pattern against samples
        GroupingEngine groupingEngine = new GroupingEngine();
        GroupingEngine.GroupingResult result =
            groupingEngine.groupAndAssignRoles(
                new ArrayList<>(sampleFilenames),
                config.getGroupPattern(),
                config.getRoleRules(),
                new UnknownSegmentHandler());

        // Check for common issues
        if (result.getMatchedFiles() == 0) {
          errors.add(
              ValidationError.of(
                  ValidationErrorType.NO_FILES_MATCHED,
                  "Group pattern doesn't match any sample files"));
        } else if (result.getMatchedFiles() < sampleFilenames.size() * 0.5) {
          warnings.add(
              ValidationWarning.of(
                  ValidationWarningType.LOW_MATCH_RATE,
                  String.format(
                      "Group pattern only matches %d of %d sample files",
                      result.getMatchedFiles(), sampleFilenames.size())));
        }

        // Check for groups with missing roles
        if (result.getGroupsWithMissingRoles() > 0) {
          warnings.add(
              ValidationWarning.of(
                  ValidationWarningType.INCOMPLETE_GROUPS,
                  String.format(
                      "%d groups are missing required image roles",
                      result.getGroupsWithMissingRoles())));
        }

      } catch (Exception e) {
        errors.add(
            ValidationError.of(
                ValidationErrorType.INVALID_GROUP_PATTERN,
                "Group pattern failed when tested against samples: " + e.getMessage()));
      }
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Combines two lists of validation errors. */
  private List<ValidationError> combineErrors(
      List<ValidationError> list1, List<ValidationError> list2) {
    List<ValidationError> combined = new ArrayList<>(list1);
    combined.addAll(list2);
    return combined;
  }

  /** Combines two lists of validation warnings. */
  private List<ValidationWarning> combineWarnings(
      List<ValidationWarning> list1, List<ValidationWarning> list2) {
    List<ValidationWarning> combined = new ArrayList<>(list1);
    combined.addAll(list2);
    return combined;
  }

  /** Updates the internal validation state with a new result. */
  private void updateValidationState(ValidationResult result) {
    validationResult.set(result);

    // Log validation results
    if (result.hasErrors()) {
      for (ValidationError error : result.getErrors()) {
        ValidationLogger.logValidationError(error, "ValidationModel");
      }
    }

    if (result.hasWarnings()) {
      for (ValidationWarning warning : result.getWarnings()) {
        ValidationLogger.logValidationWarning(warning, "ValidationModel");
      }
    }
  }

  /** Updates the current configuration and triggers validation. */
  public void updateConfiguration(PatternConfiguration config) {
    currentConfiguration.set(config);
  }

  /** Updates the sample filenames and triggers validation. */
  public void updateSampleFilenames(List<String> filenames) {
    sampleFilenames.setAll(filenames != null ? filenames : List.of());
  }

  /** Clears all validation state and resets to initial state. */
  public void clearValidationState() {
    debounceTimeline.stop();

    if (currentValidationTask != null && !currentValidationTask.isDone()) {
      currentValidationTask.cancel(true);
    }

    Platform.runLater(
        () -> {
          validationInProgress.set(false);
          updateValidationState(ValidationResult.success());
          currentConfiguration.set(null);
          sampleFilenames.clear();
        });
  }

  /** Checks if validation should show empty state (no errors or warnings). */
  public boolean shouldHideValidationDisplay() {
    ValidationResult result = validationResult.get();
    return result == null || (!result.hasErrors() && !result.hasWarnings());
  }

  /** Checks if the success banner should be shown. */
  public boolean shouldShowSuccessBanner() {
    ValidationResult result = validationResult.get();
    return result != null && result.isValid() && !result.hasWarnings();
  }

  /** Guarantees that pattern generation will succeed when success banner is shown. */
  public boolean canGeneratePatterns() {
    return shouldShowSuccessBanner() && currentConfiguration.get() != null;
  }

  // Property getters for binding

  public ObjectProperty<ValidationResult> validationResultProperty() {
    return validationResult;
  }

  public BooleanProperty validationInProgressProperty() {
    return validationInProgress;
  }

  public BooleanProperty hasErrorsProperty() {
    return hasErrors;
  }

  public BooleanProperty hasWarningsProperty() {
    return hasWarnings;
  }

  public BooleanProperty isValidProperty() {
    return isValid;
  }

  public StringProperty validationSummaryProperty() {
    return validationSummary;
  }

  public ObservableList<ValidationError> getErrors() {
    return errors;
  }

  public ObservableList<ValidationWarning> getWarnings() {
    return warnings;
  }

  public ObjectProperty<PatternConfiguration> currentConfigurationProperty() {
    return currentConfiguration;
  }

  public ObservableList<String> getSampleFilenames() {
    return sampleFilenames;
  }

  // Getters for current values

  public ValidationResult getValidationResult() {
    return validationResult.get();
  }

  public boolean isValidationInProgress() {
    return validationInProgress.get();
  }

  public boolean hasErrors() {
    return hasErrors.get();
  }

  public boolean hasWarnings() {
    return hasWarnings.get();
  }

  public boolean isValid() {
    return isValid.get();
  }

  public String getValidationSummary() {
    return validationSummary.get();
  }

  public PatternConfiguration getCurrentConfiguration() {
    return currentConfiguration.get();
  }

  /** Cleanup method to shut down background processing. */
  public void shutdown() {
    debounceTimeline.stop();

    if (currentValidationTask != null && !currentValidationTask.isDone()) {
      currentValidationTask.cancel(true);
    }

    // Virtual threads don't need explicit shutdown
  }
}
