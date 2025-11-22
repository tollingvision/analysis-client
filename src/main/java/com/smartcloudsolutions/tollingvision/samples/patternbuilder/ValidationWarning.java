package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.Objects;

/**
 * Represents a validation warning that indicates a potential issue but doesn't prevent pattern
 * usage.
 */
public class ValidationWarning {
  private final ValidationWarningType type;
  private final String message;
  private final String context;

  /**
   * Creates a new validation warning with the specified type and message.
   *
   * @param type the warning type
   * @param message the warning message
   */
  public ValidationWarning(ValidationWarningType type, String message) {
    this(type, message, null);
  }

  /**
   * Creates a new validation warning with the specified type, message, and context.
   *
   * @param type the warning type
   * @param message the warning message
   * @param context additional context information
   */
  public ValidationWarning(ValidationWarningType type, String message, String context) {
    this.type = Objects.requireNonNull(type, "Warning type cannot be null");
    this.message = message != null ? message : type.getDefaultMessage();
    this.context = context;
  }

  /**
   * Creates a validation warning using the default message for the warning type.
   *
   * @param type the warning type
   * @return a new ValidationWarning with the default message
   */
  public static ValidationWarning of(ValidationWarningType type) {
    return new ValidationWarning(type, type.getDefaultMessage());
  }

  /**
   * Creates a validation warning with a custom message.
   *
   * @param type the warning type
   * @param message the custom warning message
   * @return a new ValidationWarning with the custom message
   */
  public static ValidationWarning of(ValidationWarningType type, String message) {
    return new ValidationWarning(type, message);
  }

  /**
   * Creates a validation warning with a custom message and context.
   *
   * @param type the warning type
   * @param message the custom warning message
   * @param context additional context information
   * @return a new ValidationWarning with the custom message and context
   */
  public static ValidationWarning of(ValidationWarningType type, String message, String context) {
    return new ValidationWarning(type, message, context);
  }

  /**
   * @return the warning type
   */
  public ValidationWarningType getType() {
    return type;
  }

  /**
   * @return the warning message
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
   * @return true if this warning has context information
   */
  public boolean hasContext() {
    return context != null && !context.trim().isEmpty();
  }

  /**
   * Gets the full warning description including context if available.
   *
   * @return the full warning description
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
    ValidationWarning that = (ValidationWarning) obj;
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
        "ValidationWarning{type=%s, message='%s', context='%s'}", type, message, context);
  }
}
