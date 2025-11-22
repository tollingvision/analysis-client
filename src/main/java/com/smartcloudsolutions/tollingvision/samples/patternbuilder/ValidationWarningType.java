package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Enumeration of validation warning types that indicate potential issues but don't prevent pattern
 * usage.
 */
public enum ValidationWarningType {
  /** Some files don't match any role pattern */
  UNMATCHED_FILES("Some files don't match any role pattern"),

  /** Some groups only have one image type */
  INCOMPLETE_GROUP_COVERAGE("Some groups only have one image type"),

  /** Role rules have overlapping patterns */
  OVERLAPPING_RULES("Role rules have overlapping patterns"),

  /** Pattern may be too restrictive */
  RESTRICTIVE_PATTERN("Pattern may be too restrictive"),

  /** Pattern may be too permissive */
  PERMISSIVE_PATTERN("Pattern may be too permissive"),

  /** Case sensitivity might cause issues */
  CASE_SENSITIVITY_WARNING("Case sensitivity might cause matching issues"),

  /** Regular expression is complex and might be slow */
  COMPLEX_REGEX("Regular expression is complex and might impact performance"),

  /** No overview images found in groups */
  NO_OVERVIEW_IMAGES("No overview images found - consider adding overview rules"),

  /** Unbalanced group sizes */
  UNBALANCED_GROUPS("Groups have significantly different numbers of images"),

  /** Potential file extension issues */
  EXTENSION_MISMATCH("File extensions vary - consider using extension matching"),

  /** Rule has empty value and will not match any files */
  EMPTY_RULE_VALUE("Rule has empty value and will not match any files"),

  /** No rules defined for a specific image role */
  MISSING_ROLE_RULES("No rules defined for image role"),

  /** No sample files available for pattern testing */
  NO_SAMPLE_FILES("No sample files available for pattern testing"),

  /** Pattern matches fewer than expected files */
  LOW_MATCH_RATE("Pattern matches fewer files than expected"),

  /** Some groups are missing required image roles */
  INCOMPLETE_GROUPS("Some groups are missing required image roles");

  private final String defaultMessage;

  /**
   * Creates a validation warning type with a default message.
   *
   * @param defaultMessage the default warning message
   */
  ValidationWarningType(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  /**
   * @return the default warning message for this warning type
   */
  public String getDefaultMessage() {
    return defaultMessage;
  }
}
