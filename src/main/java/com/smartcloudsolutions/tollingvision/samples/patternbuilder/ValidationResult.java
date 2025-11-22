package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of pattern validation including errors and warnings. Errors indicate issues
 * that prevent pattern usage, while warnings indicate potential problems that don't block
 * functionality.
 */
public class ValidationResult {
  private final boolean valid;
  private final List<ValidationError> errors;
  private final List<ValidationWarning> warnings;

  /**
   * Creates a new validation result.
   *
   * @param valid true if the validation passed without errors
   * @param errors list of validation errors (blocking issues)
   * @param warnings list of validation warnings (non-blocking issues)
   */
  public ValidationResult(
      boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {
    this.valid = valid;
    this.errors = errors != null ? List.copyOf(errors) : List.of();
    this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
  }

  /**
   * Creates a successful validation result with no errors or warnings.
   *
   * @return a valid ValidationResult
   */
  public static ValidationResult success() {
    return new ValidationResult(true, List.of(), List.of());
  }

  /**
   * Creates a failed validation result with the specified errors.
   *
   * @param errors the validation errors
   * @return an invalid ValidationResult
   */
  public static ValidationResult failure(List<ValidationError> errors) {
    return new ValidationResult(false, errors, List.of());
  }

  /**
   * Creates a failed validation result with a single error.
   *
   * @param error the validation error
   * @return an invalid ValidationResult
   */
  public static ValidationResult failure(ValidationError error) {
    return new ValidationResult(false, List.of(error), List.of());
  }

  /**
   * Creates a validation result with warnings but no errors.
   *
   * @param warnings the validation warnings
   * @return a valid ValidationResult with warnings
   */
  public static ValidationResult withWarnings(List<ValidationWarning> warnings) {
    return new ValidationResult(true, List.of(), warnings);
  }

  /**
   * @return true if validation passed without errors
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * @return list of validation errors (blocking issues)
   */
  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * @return list of validation warnings (non-blocking issues)
   */
  public List<ValidationWarning> getWarnings() {
    return warnings;
  }

  /**
   * @return true if there are any errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * @return true if there are any warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets all error messages as a list of strings.
   *
   * @return list of error messages
   */
  public List<String> getErrorMessages() {
    return errors.stream().map(ValidationError::getMessage).toList();
  }

  /**
   * Gets all warning messages as a list of strings.
   *
   * @return list of warning messages
   */
  public List<String> getWarningMessages() {
    return warnings.stream().map(ValidationWarning::getMessage).toList();
  }

  /**
   * Combines this validation result with another, merging errors and warnings. The combined result
   * is valid only if both results are valid.
   *
   * @param other the other validation result to combine with
   * @return a new ValidationResult containing combined errors and warnings
   */
  public ValidationResult combine(ValidationResult other) {
    if (other == null) {
      return this;
    }

    List<ValidationError> combinedErrors = new ArrayList<>(this.errors);
    combinedErrors.addAll(other.errors);

    List<ValidationWarning> combinedWarnings = new ArrayList<>(this.warnings);
    combinedWarnings.addAll(other.warnings);

    boolean combinedValid = this.valid && other.valid;

    return new ValidationResult(combinedValid, combinedErrors, combinedWarnings);
  }

  @Override
  public String toString() {
    return String.format(
        "ValidationResult{valid=%s, errors=%d, warnings=%d}",
        valid, errors.size(), warnings.size());
  }
}
