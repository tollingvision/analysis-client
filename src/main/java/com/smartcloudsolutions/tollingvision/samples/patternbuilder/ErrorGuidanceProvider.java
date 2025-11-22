package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Map;

/**
 * Provides contextual help, error guidance, and fix recommendations for pattern builder validation
 * issues. This class centralizes all user guidance to ensure consistent messaging throughout the
 * interface.
 */
public class ErrorGuidanceProvider {

  /**
   * Gets detailed fix recommendations for a validation error.
   *
   * @param error the validation error
   * @return list of specific fix recommendations
   */
  public static List<String> getFixRecommendations(ValidationError error) {
    return switch (error.getType()) {
      case NO_GROUP_ID_SELECTED ->
          List.of(
              "Select a token from the filename that uniquely identifies each vehicle group",
              "Look for tokens that contain vehicle IDs, license plates, or unique identifiers",
              "Avoid selecting tokens that are the same across all files (like prefixes or"
                  + " extensions)");

      case INVALID_GROUP_PATTERN ->
          List.of(
              "Ensure the Group Pattern contains exactly one set of parentheses (capturing group)",
              "Check that parentheses are properly matched and not escaped",
              "If using advanced mode, verify the regex syntax is correct");

      case NO_ROLE_RULES_DEFINED ->
          List.of(
              "Define at least one rule to identify front, rear, or overview images",
              "Use 'contains' rules for common keywords like 'front', 'rear', 'overview'",
              "Start with simple rules and refine based on preview results");

      case NO_FILES_MATCHED ->
          List.of(
              "Try selecting a different token as the Group ID",
              "Check if the selected folder contains the expected image files",
              "Verify that filenames follow a consistent pattern",
              "Consider using a more general Group ID token");

      case INCOMPLETE_GROUPS ->
          List.of(
              "Review role rules to ensure they match your filename patterns",
              "Check if some image types use different naming conventions",
              "Consider adding more flexible rules (e.g., 'contains' instead of 'equals')",
              "Verify that all expected image files are present in the sample folder");

      case REGEX_SYNTAX_ERROR ->
          List.of(
              "Check for unmatched parentheses, brackets, or braces",
              "Ensure special characters are properly escaped with backslashes",
              "Verify that quantifiers (+, *, ?, {}) are used correctly",
              "Switch to Simple mode for automatic regex generation");

      case EMPTY_GROUP_PATTERN ->
          List.of(
              "Select a Group ID token to generate the pattern automatically",
              "If in Advanced mode, enter a valid regex pattern",
              "Ensure the pattern contains exactly one capturing group");

      case NO_ROLE_PATTERNS ->
          List.of(
              "Define rules for at least one image role (front, rear, or overview)",
              "Use the role rules section to specify how to identify each image type",
              "If in Advanced mode, enter regex patterns for the roles you need");

      case INVALID_RULE_VALUE ->
          List.of(
              "Enter a value for the rule (e.g., 'front', 'rear', 'overview')",
              "Use keywords that appear in your filenames",
              "Check the sample filenames for common patterns");

      case MULTIPLE_CAPTURING_GROUPS ->
          List.of(
              "Remove extra parentheses from the Group Pattern",
              "Use non-capturing groups (?:...) if you need grouping without capturing",
              "Ensure only the Group ID portion is wrapped in parentheses");

      case NO_CAPTURING_GROUPS ->
          List.of(
              "Add parentheses around the Group ID portion of the pattern",
              "Ensure the Group ID token is properly selected",
              "If in Advanced mode, manually add capturing group parentheses");

      case INVALID_RULE_CONFIGURATION ->
          List.of(
              "Check that all rule fields are properly filled",
              "Ensure rule values are appropriate for the selected rule type",
              "Verify that the target role is correctly specified");

      case INVALID_REGEX_PATTERN ->
          List.of(
              "Check the regex syntax for errors",
              "Ensure special characters are properly escaped",
              "Test the pattern with a regex validator",
              "Consider switching to Simple mode for automatic generation");
    };
  }

  /**
   * Gets contextual help text for UI components.
   *
   * @param component the UI component identifier
   * @return help text for the component
   */
  public static String getContextualHelp(String component) {
    return switch (component) {
      case "group-id-selector" ->
          "Select the part of the filename that identifies each vehicle group. "
              + "This should be unique for each vehicle (like a license plate or ID number).";

      case "role-rules" ->
          "Define rules to identify front, rear, and overview images. "
              + "Use keywords that appear in your filenames to categorize each image type.";

      case "token-selection" ->
          "These are the parts of your filenames, separated by common delimiters. "
              + "Select the token that uniquely identifies each vehicle group.";

      case "pattern-preview" ->
          "This shows how your rules will categorize actual filenames. "
              + "Green rows indicate successful matches, red rows show issues.";

      case "group-pattern" ->
          "The regex pattern used to extract the group ID from filenames. Must contain exactly one"
              + " capturing group (parentheses) around the group identifier.";

      case "role-patterns" ->
          "Regex patterns that identify front, rear, and overview images. "
              + "These are automatically generated from your role rules.";

      case "extension-matching" ->
          "Enable this to match any image extension (.jpg, .png, .tiff, etc.) "
              + "instead of requiring specific extensions in your patterns.";

      case "case-sensitivity" ->
          "When disabled, rules will match regardless of uppercase/lowercase. "
              + "Enable for exact case matching if your filenames use consistent casing.";

      case "preset-management" ->
          "Save your pattern configurations as presets for reuse. "
              + "Useful when processing similar image sets with the same naming patterns.";

      default -> "No help available for this component.";
    };
  }

  /**
   * Gets tooltip text for specific UI elements.
   *
   * @param element the UI element identifier
   * @return tooltip text
   */
  public static String getTooltip(String element) {
    return switch (element) {
      case "group-id-required" -> "Group ID selection is required to generate patterns";

      case "role-rules-optional" -> "At least one role rule is needed to categorize images";

      case "preview-update" -> "Preview updates automatically when configuration changes";

      case "validation-blocking" -> "Fix all errors before proceeding with pattern generation";

      case "extension-wildcard" -> "Match any image extension: .jpg, .jpeg, .png, .tiff, .bmp";

      case "case-insensitive" -> "Match text regardless of uppercase/lowercase";

      case "regex-syntax" -> "Use standard regex syntax with proper escaping";

      case "capturing-group" -> "Parentheses () create capturing groups for extracting values";

      case "non-capturing-group" -> "Use (?:...) for grouping without capturing";

      default -> "";
    };
  }

  /**
   * Gets examples for different pattern types.
   *
   * @param patternType the type of pattern
   * @return list of example patterns
   */
  public static List<String> getPatternExamples(String patternType) {
    return switch (patternType) {
      case "group-pattern" ->
          List.of(
              "^vehicle_([\\w\\-]+)_\\w+\\.jpg$ - Captures vehicle ID from"
                  + " vehicle_ABC123_front.jpg",
              "^(\\d{4}-\\d{2}-\\d{2})_cam\\d+_\\w+\\.jpg$ - Captures date from"
                  + " 2024-01-15_cam1_front.jpg",
              "^IMG_(\\d+)_\\w+\\.jpg$ - Captures number from IMG_001_front.jpg");

      case "role-pattern" ->
          List.of(
              ".*front.* - Matches any filename containing 'front'",
              ".*(?i:rear|back).* - Matches 'rear' or 'back' (case insensitive)",
              "^.*_ov\\..* - Matches filenames ending with '_ov.' before extension");

      case "filename-structure" ->
          List.of(
              "vehicle_ABC123_front.jpg → [vehicle] [ABC123] [front] [jpg]",
              "2024-01-15_cam1_vehicle_XYZ789_rear.png → [2024-01-15] [cam1] [vehicle] [XYZ789]"
                  + " [rear] [png]",
              "IMG_001_overview.tiff → [IMG] [001] [overview] [tiff]");

      default -> List.of();
    };
  }

  /**
   * Gets common filename patterns and their recommended configurations.
   *
   * @return map of pattern descriptions to configuration suggestions
   */
  public static Map<String, String> getCommonPatterns() {
    return Map.of(
        "vehicle_ID_role.ext",
        "Select the ID token as Group ID, use 'contains' rules for role keywords",
        "date_camera_vehicle_ID_role.ext",
        "Select the ID token as Group ID, ignore date/camera tokens",
        "IMG_number_role.ext",
        "Select the number token as Group ID, use role keywords",
        "prefix_ID_suffix_role.ext",
        "Select the ID token as Group ID, use suffix or role for categorization");
  }

  /**
   * Gets validation status messages for different states.
   *
   * @param state the validation state
   * @return status message
   */
  public static String getValidationStatusMessage(String state) {
    return switch (state) {
      case "valid" -> "✓ Configuration is valid and ready to use";
      case "errors" -> "✗ Please fix the errors below before proceeding";
      case "warnings" -> "⚠ Configuration is valid but has warnings";
      case "incomplete" -> "○ Complete the configuration to validate";
      default -> "";
    };
  }
}
