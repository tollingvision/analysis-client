package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Set;

/**
 * Utility for handling file extension matching in pattern generation. Provides options for matching
 * specific extensions or any image extension.
 */
public class ExtensionMatcher {

  /** Common image file extensions supported by the system. */
  public static final Set<String> SUPPORTED_IMAGE_EXTENSIONS =
      Set.of("jpg", "jpeg", "png", "tiff", "tif", "bmp", "gif", "webp");

  /** Regex pattern that matches any supported image extension. */
  public static final String ANY_IMAGE_EXTENSION_PATTERN =
      "(?i:\\.(jpg|jpeg|png|tiff?|bmp|gif|webp))";

  private static java.util.ResourceBundle messages = java.util.ResourceBundle.getBundle("messages");

  /**
   * Checks if a filename has a supported image extension.
   *
   * @param filename the filename to check
   * @return true if the file has a supported image extension
   */
  public static boolean hasImageExtension(String filename) {
    if (filename == null || filename.isEmpty()) {
      return false;
    }

    int lastDot = filename.lastIndexOf('.');
    if (lastDot == -1 || lastDot == filename.length() - 1) {
      return false;
    }

    String extension = filename.substring(lastDot + 1).toLowerCase();
    return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
  }

  /**
   * Extracts the file extension from a filename.
   *
   * @param filename the filename
   * @return the extension (without dot), or empty string if none
   */
  public static String getExtension(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "";
    }

    int lastDot = filename.lastIndexOf('.');
    if (lastDot == -1 || lastDot == filename.length() - 1) {
      return "";
    }

    return filename.substring(lastDot + 1).toLowerCase();
  }

  /**
   * Gets all unique extensions from a list of filenames.
   *
   * @param filenames the list of filenames
   * @return set of unique extensions (without dots)
   */
  public static Set<String> getUniqueExtensions(List<String> filenames) {
    return filenames.stream()
        .map(ExtensionMatcher::getExtension)
        .filter(ext -> !ext.isEmpty())
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Generates a regex pattern for matching specific extensions.
   *
   * @param extensions the extensions to match (without dots)
   * @param caseSensitive whether matching should be case sensitive
   * @return regex pattern for the extensions
   */
  public static String generateExtensionPattern(Set<String> extensions, boolean caseSensitive) {
    if (extensions == null || extensions.isEmpty()) {
      return ANY_IMAGE_EXTENSION_PATTERN;
    }

    String caseFlag = caseSensitive ? "" : "(?i:";
    String caseClose = caseSensitive ? "" : ")";

    String extensionList = String.join("|", extensions);
    return caseFlag + "\\.(" + extensionList + ")" + caseClose;
  }

  /**
   * Modifies a pattern to use flexible extension matching. Replaces specific extension patterns
   * with the any-image-extension pattern.
   *
   * @param pattern the original pattern
   * @param useAnyExtension whether to use flexible extension matching
   * @return modified pattern with flexible extension matching
   */
  public static String applyExtensionMatching(String pattern, boolean useAnyExtension) {
    if (pattern == null || pattern.isEmpty() || !useAnyExtension) {
      return pattern;
    }

    // Replace common extension patterns with flexible matching
    String modified = pattern;

    // Escape the replacement pattern to avoid group reference issues
    String escapedReplacement =
        java.util.regex.Matcher.quoteReplacement(ANY_IMAGE_EXTENSION_PATTERN);

    // Replace specific extensions like .jpg, .png, etc.
    modified = modified.replaceAll("\\\\\\.[a-zA-Z]{3,4}\\$", escapedReplacement + "\\$");
    modified = modified.replaceAll("\\\\\\.[a-zA-Z]{3,4}(?=\\W|$)", escapedReplacement);

    // Replace case-insensitive extension patterns
    modified = modified.replaceAll("\\(\\?i:[^)]*\\\\\\.[^)]+\\)", escapedReplacement);

    // Replace extension character classes like [jJ][pP][gG]
    modified = modified.replaceAll("\\\\\\.[\\[a-zA-Z\\]]+\\$", escapedReplacement + "\\$");

    return modified;
  }

  /**
   * Checks if a pattern already includes flexible extension matching.
   *
   * @param pattern the pattern to check
   * @return true if the pattern uses flexible extension matching
   */
  public static boolean hasFlexibleExtensionMatching(String pattern) {
    if (pattern == null) {
      return false;
    }

    return pattern.contains("jpg|jpeg|png|tiff")
        || pattern.contains("\\.(jpg|jpeg|png|tiff")
        || pattern.matches(".*\\(\\?i:.*\\\\\\.[^)]*\\).*");
  }

  /**
   * Validates that a filename matches the expected image extension pattern.
   *
   * @param filename the filename to validate
   * @param useAnyExtension whether any image extension should be accepted
   * @param specificExtensions specific extensions to match (if not using any extension)
   * @return true if the filename matches the extension requirements
   */
  public static boolean validateExtension(
      String filename, boolean useAnyExtension, Set<String> specificExtensions) {
    if (filename == null || filename.isEmpty()) {
      return false;
    }

    if (useAnyExtension) {
      return hasImageExtension(filename);
    }

    if (specificExtensions == null || specificExtensions.isEmpty()) {
      return hasImageExtension(filename);
    }

    String extension = getExtension(filename);
    return specificExtensions.contains(extension);
  }

  /**
   * Gets a user-friendly description of extension matching settings.
   *
   * @param useAnyExtension whether any extension matching is enabled
   * @param specificExtensions specific extensions being matched
   * @return description of extension matching
   */
  public static String getExtensionMatchingDescription(
      boolean useAnyExtension, Set<String> specificExtensions) {
    if (useAnyExtension) {
      return messages.getString("extension.matching.description.any");
    }

    if (specificExtensions == null || specificExtensions.isEmpty()) {
      return messages.getString("extension.matching.description.none");
    }

    if (specificExtensions.size() == 1) {
      return String.format(
          messages.getString("extension.matching.description.single"),
          specificExtensions.iterator().next());
    }

    return String.format(
        messages.getString("extension.matching.description.specific"),
        specificExtensions.stream()
            .map(ext -> "." + ext)
            .collect(java.util.stream.Collectors.joining(", ")));
  }

  /**
   * Suggests whether to use flexible extension matching based on filename analysis.
   *
   * @param filenames the filenames to analyze
   * @return recommendation for extension matching
   */
  public static ExtensionMatchingRecommendation analyzeExtensionUsage(List<String> filenames) {
    if (filenames == null || filenames.isEmpty()) {
      return new ExtensionMatchingRecommendation(false, Set.of(), "No files to analyze");
    }

    Set<String> extensions = getUniqueExtensions(filenames);

    if (extensions.isEmpty()) {
      return new ExtensionMatchingRecommendation(false, Set.of(), "No file extensions found");
    }

    if (extensions.size() == 1) {
      String ext = extensions.iterator().next();
      return new ExtensionMatchingRecommendation(
          false, extensions, "All files use ." + ext + " extension");
    }

    // Check if all extensions are image extensions
    boolean allImageExtensions = extensions.stream().allMatch(SUPPORTED_IMAGE_EXTENSIONS::contains);

    if (allImageExtensions) {
      return new ExtensionMatchingRecommendation(
          true,
          extensions,
          String.format(
              "Multiple image extensions found (%s) - recommend flexible matching",
              extensions.stream()
                  .map(ext -> "." + ext)
                  .collect(java.util.stream.Collectors.joining(", "))));
    } else {
      return new ExtensionMatchingRecommendation(
          false, extensions, "Mixed file types found - specific extension matching recommended");
    }
  }

  /** Represents a recommendation for extension matching configuration. */
  public static class ExtensionMatchingRecommendation {
    private final boolean recommendFlexibleMatching;
    private final Set<String> detectedExtensions;
    private final String reasoning;

    public ExtensionMatchingRecommendation(
        boolean recommendFlexibleMatching, Set<String> detectedExtensions, String reasoning) {
      this.recommendFlexibleMatching = recommendFlexibleMatching;
      this.detectedExtensions = Set.copyOf(detectedExtensions);
      this.reasoning = reasoning;
    }

    public boolean isRecommendFlexibleMatching() {
      return recommendFlexibleMatching;
    }

    public Set<String> getDetectedExtensions() {
      return detectedExtensions;
    }

    public String getReasoning() {
      return reasoning;
    }
  }
}
