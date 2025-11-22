package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.Objects;

/**
 * Represents a validation error that prevents pattern usage. Errors must be resolved before the
 * pattern can be used.
 */
public class ValidationError {
  private final ValidationErrorType type;
  private final String message;
  private final String context;

  /**
   * Creates a new validation error with the specified type and message.
   *
   * @param type the error type
   * @param message the error message
   */
  public ValidationError(ValidationErrorType type, String message) {
    this(type, message, null);
  }

  /**
   * Creates a new validation error with the specified type, message, and context.
   *
   * @param type the error type
   * @param message the error message
   * @param context additional context information
   */
  public ValidationError(ValidationErrorType type, String message, String context) {
    this.type = Objects.requireNonNull(type, "Error type cannot be null");
    this.message = message != null ? message : type.getDefaultMessage();
    this.context = context;
  }

  /**
   * Creates a validation error using the default message for the error type.
   *
   * @param type the error type
   * @return a new ValidationError with the default message
   */
  public static ValidationError of(ValidationErrorType type) {
    return new ValidationError(type, type.getDefaultMessage());
  }

  /**
   * Creates a validation error with a custom message.
   *
   * @param type the error type
   * @param message the custom error message
   * @return a new ValidationError with the custom message
   */
  public static ValidationError of(ValidationErrorType type, String message) {
    return new ValidationError(type, message);
  }

  /**
   * Creates a validation error with a custom message and context.
   *
   * @param type the error type
   * @param message the custom error message
   * @param context additional context information
   * @return a new ValidationError with the custom message and context
   */
  public static ValidationError of(ValidationErrorType type, String message, String context) {
    return new ValidationError(type, message, context);
  }

  /**
   * @return the error type
   */
  public ValidationErrorType getType() {
    return type;
  }

  /**
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return the context information, or null if none provided
   */
  public String getContext() {
    return context;
  }

  /**
   * @return true if this error has context information
   */
  public boolean hasContext() {
    return context != null && !context.trim().isEmpty();
  }

  /**
   * Gets the full error description including context if available.
   *
   * @return the full error description
   */
  public String getFullDescription() {
    if (hasContext()) {
      return message + " (" + context + ")";
    }
    return message;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    ValidationError that = (ValidationError) obj;
    return type == that.type
        && Objects.equals(message, that.message)
        && Objects.equals(context, that.context);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message, context);
  }

  @Override
  public String toString() {
    return String.format(
        "ValidationError{type=%s, message='%s', context='%s'}", type, message, context);
  }
}
