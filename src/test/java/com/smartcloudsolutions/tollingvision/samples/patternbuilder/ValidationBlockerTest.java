package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ValidationBlocker functionality. */
class ValidationBlockerTest {

  private ValidationBlocker validationBlocker;

  @BeforeEach
  void setUp() {
    validationBlocker = new ValidationBlocker();
  }

  @Test
  void testInitialState() {
    assertTrue(validationBlocker.isBlocked());
    assertEquals("Configuration incomplete", validationBlocker.getBlockingReason());
    assertTrue(validationBlocker.getBlockingErrors().isEmpty());
    assertTrue(validationBlocker.getActiveWarnings().isEmpty());
  }

  @Test
  void testValidConfiguration() {
    ValidationResult validResult = ValidationResult.success();
    validationBlocker.updateValidationState(validResult);

    assertFalse(validationBlocker.isBlocked());
    assertEquals("Configuration is valid", validationBlocker.getBlockingReason());
    assertTrue(validationBlocker.getBlockingErrors().isEmpty());
  }

  @Test
  void testConfigurationWithErrors() {
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationResult errorResult = ValidationResult.failure(List.of(error));

    validationBlocker.updateValidationState(errorResult);

    assertTrue(validationBlocker.isBlocked());
    assertEquals(1, validationBlocker.getBlockingErrors().size());
    assertEquals(error, validationBlocker.getBlockingErrors().get(0));
  }

  @Test
  void testConfigurationWithWarnings() {
    ValidationWarning warning = ValidationWarning.of(ValidationWarningType.UNMATCHED_FILES);
    ValidationResult warningResult = ValidationResult.withWarnings(List.of(warning));

    validationBlocker.updateValidationState(warningResult);

    assertFalse(validationBlocker.isBlocked());
    assertEquals(1, validationBlocker.getActiveWarnings().size());
    assertEquals(warning, validationBlocker.getActiveWarnings().get(0));
  }

  @Test
  void testIsBlockedBy() {
    ValidationError error1 = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationError error2 = ValidationError.of(ValidationErrorType.INVALID_GROUP_PATTERN);
    ValidationResult errorResult = ValidationResult.failure(List.of(error1, error2));

    validationBlocker.updateValidationState(errorResult);

    assertTrue(validationBlocker.isBlockedBy(ValidationErrorType.NO_GROUP_ID_SELECTED));
    assertTrue(validationBlocker.isBlockedBy(ValidationErrorType.INVALID_GROUP_PATTERN));
    assertFalse(validationBlocker.isBlockedBy(ValidationErrorType.NO_ROLE_RULES_DEFINED));
  }

  @Test
  void testGetMostCriticalError() {
    // Test with no errors
    assertNull(validationBlocker.getMostCriticalError());

    // Test with multiple errors (should prioritize by error priority)
    ValidationError lowPriorityError = ValidationError.of(ValidationErrorType.INCOMPLETE_GROUPS);
    ValidationError highPriorityError =
        ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationResult errorResult =
        ValidationResult.failure(List.of(lowPriorityError, highPriorityError));

    validationBlocker.updateValidationState(errorResult);

    ValidationError critical = validationBlocker.getMostCriticalError();
    assertEquals(ValidationErrorType.NO_GROUP_ID_SELECTED, critical.getType());
  }

  @Test
  void testGetFixRecommendations() {
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationResult errorResult = ValidationResult.failure(List.of(error));

    validationBlocker.updateValidationState(errorResult);

    List<String> recommendations = validationBlocker.getFixRecommendations();
    assertFalse(recommendations.isEmpty());
    assertTrue(recommendations.stream().anyMatch(rec -> rec.contains("Select a token")));
  }

  @Test
  void testGetValidationSummary() {
    // Test valid state
    ValidationResult validResult = ValidationResult.success();
    validationBlocker.updateValidationState(validResult);

    String summary = validationBlocker.getValidationSummary();
    assertTrue(summary.contains("Configuration is valid"));

    // Test valid with warnings
    ValidationWarning warning = ValidationWarning.of(ValidationWarningType.UNMATCHED_FILES);
    ValidationResult warningResult = ValidationResult.withWarnings(List.of(warning));
    validationBlocker.updateValidationState(warningResult);

    summary = validationBlocker.getValidationSummary();
    assertTrue(summary.contains("valid with 1 warning"));

    // Test blocked state
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationResult errorResult = ValidationResult.failure(List.of(error));
    validationBlocker.updateValidationState(errorResult);

    summary = validationBlocker.getValidationSummary();
    assertTrue(summary.contains("1 error"));
    assertTrue(summary.contains("must be fixed"));
  }

  @Test
  void testGetErrorDetails() {
    ValidationError error1 = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationError error2 =
        ValidationError.of(ValidationErrorType.INVALID_GROUP_PATTERN, "Custom message");
    ValidationResult errorResult = ValidationResult.failure(List.of(error1, error2));

    validationBlocker.updateValidationState(errorResult);

    String details = validationBlocker.getErrorDetails();
    assertTrue(details.contains("Errors that must be fixed"));
    assertTrue(details.contains("1. Please select a token"));
    assertTrue(details.contains("2. Custom message"));
    assertTrue(details.contains("Fixes:"));
  }

  @Test
  void testGetWarningDetails() {
    ValidationWarning warning1 = ValidationWarning.of(ValidationWarningType.UNMATCHED_FILES);
    ValidationWarning warning2 =
        ValidationWarning.of(ValidationWarningType.INCOMPLETE_GROUP_COVERAGE, "Custom warning");
    ValidationResult warningResult = ValidationResult.withWarnings(List.of(warning1, warning2));

    validationBlocker.updateValidationState(warningResult);

    String details = validationBlocker.getWarningDetails();
    assertTrue(details.contains("Warnings (non-blocking)"));
    assertTrue(details.contains("1. Some files don't match"));
    assertTrue(details.contains("2. Custom warning"));
  }

  @Test
  void testClearValidationState() {
    // Set up some validation state
    ValidationError error = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationResult errorResult = ValidationResult.failure(List.of(error));
    validationBlocker.updateValidationState(errorResult);

    assertTrue(validationBlocker.isBlocked());
    assertFalse(validationBlocker.getBlockingErrors().isEmpty());

    // Clear state
    validationBlocker.clearValidationState();

    assertTrue(validationBlocker.isBlocked());
    assertEquals("Configuration incomplete", validationBlocker.getBlockingReason());
    assertTrue(validationBlocker.getBlockingErrors().isEmpty());
    assertTrue(validationBlocker.getActiveWarnings().isEmpty());
  }

  @Test
  void testNullValidationResult() {
    validationBlocker.updateValidationState(null);

    assertTrue(validationBlocker.isBlocked());
    assertEquals("No validation result available", validationBlocker.getBlockingReason());
  }

  @Test
  void testMultipleErrorsBlockingReason() {
    ValidationError error1 = ValidationError.of(ValidationErrorType.NO_GROUP_ID_SELECTED);
    ValidationError error2 = ValidationError.of(ValidationErrorType.INVALID_GROUP_PATTERN);
    ValidationError error3 = ValidationError.of(ValidationErrorType.NO_ROLE_RULES_DEFINED);
    ValidationResult errorResult = ValidationResult.failure(List.of(error1, error2, error3));

    validationBlocker.updateValidationState(errorResult);

    String reason = validationBlocker.getBlockingReason();
    assertTrue(reason.contains("Please select a token")); // Most critical error
    assertTrue(reason.contains("and 2 other errors"));
  }
}
