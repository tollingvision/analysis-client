package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Enumeration of validation error types with user-friendly descriptions. These errors prevent
 * pattern usage and must be resolved.
 */
public enum ValidationErrorType {
  /** No Group ID token has been selected */
  NO_GROUP_ID_SELECTED("Please select a token to use as Group ID"),

  /** Group pattern doesn't contain exactly one capturing group */
  INVALID_GROUP_PATTERN("Group pattern must contain exactly one capturing group"),

  /** No role rules have been defined */
  NO_ROLE_RULES_DEFINED("Please define rules for identifying image roles"),

  /** No files match the current pattern */
  NO_FILES_MATCHED("No files match the current pattern - try adjusting Group ID"),

  /** Some groups are missing required image types */
  INCOMPLETE_GROUPS("Some groups are missing required image types"),

  /** Invalid regular expression syntax */
  REGEX_SYNTAX_ERROR("Invalid regular expression syntax"),

  /** Group pattern is empty or null */
  EMPTY_GROUP_PATTERN("Group pattern cannot be empty"),

  /** All role patterns are empty */
  NO_ROLE_PATTERNS("At least one role pattern must be defined"),

  /** Rule value is empty or invalid */
  INVALID_RULE_VALUE("Rule value cannot be empty"),

  /** Multiple capturing groups found in group pattern */
  MULTIPLE_CAPTURING_GROUPS(
      "Group pattern contains multiple capturing groups - only one is allowed"),

  /** No capturing groups found in group pattern */
  NO_CAPTURING_GROUPS("Group pattern must contain exactly one capturing group"),

  /** Invalid rule configuration */
  INVALID_RULE_CONFIGURATION("Invalid rule configuration"),

  /** Invalid regex pattern in rule */
  INVALID_REGEX_PATTERN("Invalid regex pattern in rule");

  private final String defaultMessage;

  /**
   * Creates a validation error type with a default message.
   *
   * @param defaultMessage the default error message
   */
  ValidationErrorType(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  /**
   * @return the default error message for this error type
   */
  public String getDefaultMessage() {
    return defaultMessage;
  }
}
