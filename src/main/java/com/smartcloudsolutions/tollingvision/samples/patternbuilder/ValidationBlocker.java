package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages validation blocking to prevent processing until all critical issues are resolved.
 * Provides reactive properties for UI binding and clear messaging about what needs to be fixed.
 */
public class ValidationBlocker {

  private final BooleanProperty blocked = new SimpleBooleanProperty(true);
  private final StringProperty blockingReason =
      new SimpleStringProperty("Configuration incomplete");
  private final ObservableList<ValidationError> blockingErrors =
      FXCollections.observableArrayList();
  private final ObservableList<ValidationWarning> activeWarnings =
      FXCollections.observableArrayList();

  /**
   * Updates the validation state based on the provided validation result.
   *
   * @param result the validation result to process
   */
  public void updateValidationState(ValidationResult result) {
    if (result == null) {
      setBlocked(true, "No validation result available");
      blockingErrors.clear();
      activeWarnings.clear();
      return;
    }

    // Update error and warning lists
    blockingErrors.setAll(result.getErrors());
    activeWarnings.setAll(result.getWarnings());

    // Determine if processing should be blocked
    boolean shouldBlock = result.hasErrors();

    if (shouldBlock) {
      String reason = generateBlockingReason(result.getErrors());
      setBlocked(true, reason);
      ValidationLogger.logValidationError(
          ValidationError.of(ValidationErrorType.INVALID_RULE_CONFIGURATION, reason),
          "Blocking processing due to validation errors");
    } else {
      setBlocked(false, "Configuration is valid");
      if (result.hasWarnings()) {
        ValidationLogger.logUserAction(
            "Proceeding with warnings", result.getWarnings().size() + " warnings present");
      }
    }
  }

  /**
   * Checks if a specific error type is currently blocking.
   *
   * @param errorType the error type to check
   * @return true if this error type is blocking
   */
  public boolean isBlockedBy(ValidationErrorType errorType) {
    return blockingErrors.stream().anyMatch(error -> error.getType() == errorType);
  }

  /**
   * Gets the most critical blocking error.
   *
   * @return the most critical error, or null if none
   */
  public ValidationError getMostCriticalError() {
    if (blockingErrors.isEmpty()) {
      return null;
    }

    // Prioritize certain error types
    return blockingErrors.stream()
        .min(
            (e1, e2) ->
                Integer.compare(getErrorPriority(e1.getType()), getErrorPriority(e2.getType())))
        .orElse(blockingErrors.get(0));
  }

  /**
   * Gets fix recommendations for the current blocking errors.
   *
   * @return list of fix recommendations
   */
  public List<String> getFixRecommendations() {
    return blockingErrors.stream()
        .flatMap(error -> ErrorGuidanceProvider.getFixRecommendations(error).stream())
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Gets a user-friendly summary of the current validation state.
   *
   * @return validation state summary
   */
  public String getValidationSummary() {
    if (!blocked.get()) {
      if (activeWarnings.isEmpty()) {
        return "✓ Configuration is valid and ready to use";
      } else {
        return String.format(
            "⚠ Configuration is valid with %d warning%s",
            activeWarnings.size(), activeWarnings.size() == 1 ? "" : "s");
      }
    } else {
      return String.format(
          "✗ %d error%s must be fixed before proceeding",
          blockingErrors.size(), blockingErrors.size() == 1 ? "" : "s");
    }
  }

  /**
   * Gets detailed error information for display.
   *
   * @return formatted error details
   */
  public String getErrorDetails() {
    if (blockingErrors.isEmpty()) {
      return "";
    }

    StringBuilder details = new StringBuilder();
    details.append("Errors that must be fixed:\n\n");

    for (int i = 0; i < blockingErrors.size(); i++) {
      ValidationError error = blockingErrors.get(i);
      details.append(String.format("%d. %s", i + 1, error.getMessage()));

      if (error.hasContext()) {
        details.append("\n   Context: ").append(error.getContext());
      }

      List<String> fixes = ErrorGuidanceProvider.getFixRecommendations(error);
      if (!fixes.isEmpty()) {
        details.append("\n   Fixes:");
        for (String fix : fixes) {
          details.append("\n   • ").append(fix);
        }
      }

      if (i < blockingErrors.size() - 1) {
        details.append("\n\n");
      }
    }

    return details.toString();
  }

  /**
   * Gets warning information for display.
   *
   * @return formatted warning details
   */
  public String getWarningDetails() {
    if (activeWarnings.isEmpty()) {
      return "";
    }

    StringBuilder details = new StringBuilder();
    details.append("Warnings (non-blocking):\n\n");

    for (int i = 0; i < activeWarnings.size(); i++) {
      ValidationWarning warning = activeWarnings.get(i);
      details.append(String.format("%d. %s", i + 1, warning.getMessage()));

      if (warning.hasContext()) {
        details.append("\n   Context: ").append(warning.getContext());
      }

      if (i < activeWarnings.size() - 1) {
        details.append("\n\n");
      }
    }

    return details.toString();
  }

  /** Clears all validation state. */
  public void clearValidationState() {
    blockingErrors.clear();
    activeWarnings.clear();
    setBlocked(true, "Configuration incomplete");
  }

  /**
   * Property indicating whether processing is blocked.
   *
   * @return blocked property
   */
  public BooleanProperty blockedProperty() {
    return blocked;
  }

  /**
   * @return true if processing is currently blocked
   */
  public boolean isBlocked() {
    return blocked.get();
  }

  /**
   * Property containing the reason for blocking.
   *
   * @return blocking reason property
   */
  public StringProperty blockingReasonProperty() {
    return blockingReason;
  }

  /**
   * @return the current blocking reason
   */
  public String getBlockingReason() {
    return blockingReason.get();
  }

  /**
   * Observable list of current blocking errors.
   *
   * @return blocking errors list
   */
  public ObservableList<ValidationError> getBlockingErrors() {
    return blockingErrors;
  }

  /**
   * Observable list of current warnings.
   *
   * @return active warnings list
   */
  public ObservableList<ValidationWarning> getActiveWarnings() {
    return activeWarnings;
  }

  /** Sets the blocked state with a reason. */
  private void setBlocked(boolean isBlocked, String reason) {
    blocked.set(isBlocked);
    blockingReason.set(reason);
  }

  /** Generates a user-friendly blocking reason from errors. */
  private String generateBlockingReason(List<ValidationError> errors) {
    if (errors.isEmpty()) {
      return "Configuration incomplete";
    }

    if (errors.size() == 1) {
      return errors.get(0).getMessage();
    }

    // Find the most critical error
    ValidationError critical =
        errors.stream()
            .min(
                (e1, e2) ->
                    Integer.compare(getErrorPriority(e1.getType()), getErrorPriority(e2.getType())))
            .orElse(errors.get(0));

    return String.format(
        "%s (and %d other error%s)",
        critical.getMessage(), errors.size() - 1, errors.size() == 2 ? "" : "s");
  }

  /** Gets the priority of an error type (lower number = higher priority). */
  private int getErrorPriority(ValidationErrorType errorType) {
    return switch (errorType) {
      case NO_GROUP_ID_SELECTED -> 1;
      case EMPTY_GROUP_PATTERN -> 2;
      case INVALID_GROUP_PATTERN -> 3;
      case NO_CAPTURING_GROUPS -> 4;
      case MULTIPLE_CAPTURING_GROUPS -> 5;
      case NO_ROLE_RULES_DEFINED -> 6;
      case NO_ROLE_PATTERNS -> 7;
      case INVALID_RULE_VALUE -> 8;
      case REGEX_SYNTAX_ERROR -> 9;
      case NO_FILES_MATCHED -> 10;
      case INCOMPLETE_GROUPS -> 11;
      case INVALID_RULE_CONFIGURATION -> 12;
      case INVALID_REGEX_PATTERN -> 13;
    };
  }
}
