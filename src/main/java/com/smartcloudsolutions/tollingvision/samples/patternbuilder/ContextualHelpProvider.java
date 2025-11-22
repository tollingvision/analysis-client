package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Provides contextual help, tooltips, and explanatory text throughout the pattern builder
 * interface. Centralizes all help content for consistency and maintainability.
 */
public class ContextualHelpProvider {

  // Cache for created tooltips to avoid recreation
  private static final Map<String, Tooltip> TOOLTIP_CACHE = new HashMap<>();

  /**
   * Adds a tooltip to a UI component with contextual help.
   *
   * @param node the UI component
   * @param helpKey the help content key
   */
  public static void addTooltip(Node node, String helpKey) {
    if (node == null || helpKey == null) {
      return;
    }

    Tooltip tooltip = getOrCreateTooltip(helpKey);
    if (tooltip != null) {
      Tooltip.install(node, tooltip);
    }
  }

  /**
   * Adds a custom tooltip to a UI component.
   *
   * @param node the UI component
   * @param text the tooltip text
   */
  public static void addCustomTooltip(Node node, String text) {
    if (node == null || text == null || text.trim().isEmpty()) {
      return;
    }

    Tooltip tooltip = createTooltip(text);
    Tooltip.install(node, tooltip);
  }

  /**
   * Gets help text for a specific component or concept.
   *
   * @param helpKey the help content key
   * @return help text, or empty string if not found
   */
  public static String getHelpText(String helpKey) {
    return switch (helpKey) {
      case "group-id-selection" ->
          "The Group ID identifies each unique vehicle in your image set. "
              + "Select the part of the filename that is unique for each vehicle, "
              + "such as a license plate number, vehicle ID, or timestamp. "
              + "This token will be used to group related images together.";

      case "token-types" ->
          "Tokens are the parts of your filename separated by delimiters like "
              + "underscores, hyphens, or dots. Common token types include:\n"
              + "• PREFIX: Fixed text at the beginning\n"
              + "• GROUP_ID: Unique identifier for each vehicle\n"
              + "• CAMERA_SIDE: Indicates image type (front, rear, overview)\n"
              + "• DATE: Date/time information\n"
              + "• INDEX: Sequential numbers\n"
              + "• EXTENSION: File extension (.jpg, .png, etc.)";

      case "role-rules" ->
          "Role rules determine how to categorize images as front, rear, or overview. "
              + "Define rules based on keywords or patterns in your filenames:\n"
              + "• EQUALS: Exact match (case sensitive/insensitive)\n"
              + "• CONTAINS: Filename contains the specified text\n"
              + "• STARTS_WITH: Filename begins with the text\n"
              + "• ENDS_WITH: Filename ends with the text\n"
              + "Overview rules are applied first and exclude files from front/rear consideration.";

      case "pattern-preview" ->
          "The preview shows how your configuration will categorize actual filenames. "
              + "Each row shows:\n"
              + "• Filename: The original filename\n"
              + "• Group ID: The extracted group identifier\n"
              + "• Role: The assigned image role (front, rear, overview)\n"
              + "• Status: Whether the file was successfully processed\n"
              + "Green rows indicate successful matches, red rows show issues that need attention.";

      case "regex-patterns" ->
          "Regular expressions (regex) are patterns that match text. The pattern builder"
              + " automatically generates regex from your visual configuration:\n"
              + "• Group Pattern: Extracts the group ID (must have exactly one capturing group)\n"
              + "• Role Patterns: Identify front, rear, and overview images\n"
              + "In Advanced mode, you can edit these patterns directly.";

      case "capturing-groups" ->
          "Capturing groups in regex are marked with parentheses (). "
              + "The Group Pattern must contain exactly one capturing group around "
              + "the part that identifies each vehicle. For example:\n"
              + "• vehicle_([A-Z0-9]+)_front.jpg captures the vehicle ID\n"
              + "• (\\d{4}-\\d{2}-\\d{2})_cam1_rear.jpg captures the date";

      case "extension-matching" ->
          "Extension matching controls how file extensions are handled:\n"
              + "• Specific: Match only the extensions found in your sample files\n"
              + "• Flexible: Match any common image extension (.jpg, .png, .tiff, etc.)\n"
              + "Use flexible matching when your image sets may have mixed extensions.";

      case "case-sensitivity" ->
          "Case sensitivity determines whether 'Front' matches 'front':\n"
              + "• Case Sensitive: Exact case matching required\n"
              + "• Case Insensitive: Matches regardless of uppercase/lowercase\n"
              + "Most filename patterns work better with case insensitive matching.";

      case "validation-errors" ->
          "Validation errors prevent pattern usage and must be fixed:\n"
              + "• Red indicators show blocking issues\n"
              + "• Each error includes specific fix recommendations\n"
              + "• The preview will show which files are affected\n"
              + "Fix all errors before proceeding with image processing.";

      case "validation-warnings" ->
          "Validation warnings indicate potential issues but don't block usage:\n"
              + "• Yellow indicators show non-blocking concerns\n"
              + "• Warnings help optimize your configuration\n"
              + "• You can proceed with warnings, but consider addressing them\n"
              + "• Common warnings include unmatched files or incomplete groups.";

      case "preset-management" ->
          "Presets save your pattern configurations for reuse:\n"
              + "• Save successful configurations as named presets\n"
              + "• Load presets for similar image sets\n"
              + "• Export/import presets to share between installations\n"
              + "• Presets include both visual rules and generated regex patterns.";

      case "performance-tips" ->
          "For optimal performance with large image sets:\n"
              + "• Sample analysis is limited to 500 files\n"
              + "• Complex regex patterns may slow processing\n"
              + "• Use specific rules rather than overly broad patterns\n"
              + "• Test with a small sample before processing large batches.";

      default -> "";
    };
  }

  /**
   * Gets tooltip text for specific UI elements.
   *
   * @param tooltipKey the tooltip key
   * @return tooltip text
   */
  public static String getTooltipText(String tooltipKey) {
    return switch (tooltipKey) {
      case "group-id-required" -> "Group ID selection is required to generate patterns";

      case "role-rules-optional" -> "Define at least one role rule to categorize images";

      case "preview-auto-update" -> "Preview updates automatically when configuration changes";

      case "validation-blocking" -> "Fix all validation errors before proceeding";

      case "extension-flexible" ->
          "Match any image extension: .jpg, .jpeg, .png, .tiff, .bmp, .gif, .webp";

      case "case-insensitive" -> "Match text regardless of uppercase/lowercase";

      case "regex-syntax" ->
          "Use standard regex syntax with proper escaping for special characters";

      case "capturing-group-required" ->
          "Group pattern must contain exactly one capturing group ()";

      case "non-capturing-group" -> "Use (?:...) for grouping without capturing";

      case "pattern-explanation" -> "Click to see how this pattern works";

      case "sample-files" -> "First 500 files from the selected folder will be analyzed";

      case "token-confidence" -> "Confidence level for automatic token type detection";

      case "rule-priority" -> "Lower numbers have higher priority (overview rules always first)";

      case "preset-auto-save" -> "Configuration is automatically saved when patterns are applied";

      case "advanced-mode" -> "Edit regex patterns directly with live preview";

      case "simple-mode" -> "Visual pattern builder with automatic regex generation";

      default -> "";
    };
  }

  /**
   * Gets step-by-step guidance for the pattern building workflow.
   *
   * @param step the workflow step
   * @return guidance text for the step
   */
  public static String getWorkflowGuidance(String step) {
    return switch (step) {
      case "file-analysis" ->
          "Step 1: Select a folder containing sample image files. "
              + "The system will analyze the first 500 files to detect naming patterns.";

      case "token-selection" ->
          "Step 1: Review the detected filename tokens. "
              + "These are the parts of your filenames separated by common delimiters.";

      case "group-id-selection" ->
          "Step 2: Select the token that uniquely identifies each vehicle. "
              + "This is typically a license plate, vehicle ID, or unique identifier.";

      case "role-rules" ->
          "Step 3: Define rules to categorize images by role. "
              + "Specify how to identify front, rear, and overview images.";

      case "preview-validation" ->
          "Step 5: Review the preview to ensure your rules work correctly. "
              + "Fix any validation errors before proceeding.";

      case "pattern-generation" ->
          "Step 6: Generate the final patterns and apply to your configuration. "
              + "The patterns will be used for actual image processing.";

      default -> "";
    };
  }

  /**
   * Gets error-specific help text with actionable guidance.
   *
   * @param errorType the validation error type
   * @return detailed help text for the error
   */
  public static String getErrorHelp(ValidationErrorType errorType) {
    return switch (errorType) {
      case NO_GROUP_ID_SELECTED ->
          "You need to select a Group ID token to identify each vehicle group.\n\n"
              + "What to do:\n"
              + "1. Look at your sample filenames in the preview\n"
              + "2. Identify which part is unique for each vehicle\n"
              + "3. Click on that token to select it as the Group ID\n\n"
              + "Example: In 'vehicle_ABC123_front.jpg', select 'ABC123' as the Group ID.";

      case INVALID_GROUP_PATTERN ->
          "The Group Pattern must contain exactly one capturing group (parentheses).\n\n"
              + "What to do:\n"
              + "1. Check that the pattern has exactly one set of parentheses ()\n"
              + "2. Ensure parentheses are around the Group ID portion\n"
              + "3. If in Advanced mode, verify regex syntax is correct\n\n"
              + "Example: ^vehicle_([A-Z0-9]+)_\\w+\\.jpg$ has one capturing group around the ID.";

      case NO_ROLE_RULES_DEFINED ->
          "You need to define rules to identify different image types.\n\n"
              + "What to do:\n"
              + "1. Add rules for front, rear, or overview images\n"
              + "2. Use keywords that appear in your filenames\n"
              + "3. Start with 'contains' rules for common words\n\n"
              + "Example: Add a rule where filename 'contains' 'front' for front images.";

      case NO_FILES_MATCHED ->
          "No files match your current Group Pattern.\n\n"
              + "What to do:\n"
              + "1. Try selecting a different Group ID token\n"
              + "2. Check if your sample folder contains the expected files\n"
              + "3. Verify filenames follow a consistent pattern\n\n"
              + "The preview will show which files are not matching.";

      default -> "Please check the validation error details and fix recommendations.";
    };
  }

  /** Gets or creates a tooltip for the specified key. */
  private static Tooltip getOrCreateTooltip(String key) {
    return TOOLTIP_CACHE.computeIfAbsent(
        key,
        k -> {
          String text = getTooltipText(k);
          return text.isEmpty() ? null : createTooltip(text);
        });
  }

  /** Creates a properly configured tooltip. */
  private static Tooltip createTooltip(String text) {
    Tooltip tooltip = new Tooltip(text);
    tooltip.setShowDelay(Duration.millis(500));
    tooltip.setHideDelay(Duration.millis(100));
    tooltip.setShowDuration(Duration.seconds(10));
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(400);
    return tooltip;
  }

  /** Clears the tooltip cache (useful for testing or memory management). */
  public static void clearTooltipCache() {
    TOOLTIP_CACHE.clear();
  }
}
