package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated statistics and validation status for pattern preview results. Provides summary
 * information about how filenames are being categorized and identifies potential configuration
 * issues.
 */
public class PreviewSummary {
  private final int totalFiles;
  private final int matchedFiles;
  private final int unmatchedFiles;
  private final Map<ImageRole, Integer> roleCounts;
  private final List<String> unmatchedFilenames;
  private final Map<String, Set<ImageRole>> groupRoles;
  private final List<String> incompleteGroups;
  private final List<String> errorMessages;
  private final boolean hasErrors;
  private final boolean hasWarnings;
  private final java.util.ResourceBundle messages;

  /**
   * Creates a preview summary from a list of filename previews.
   *
   * @param previews the list of filename previews to summarize
   * @param messages i18n messages
   */
  public PreviewSummary(List<FilenamePreview> previews, java.util.ResourceBundle messages) {
    this.messages = messages;
    if (previews == null) {
      previews = new ArrayList<>();
    }

    this.totalFiles = previews.size();
    this.roleCounts = new EnumMap<>(ImageRole.class);
    this.unmatchedFilenames = new ArrayList<>();
    this.groupRoles = new HashMap<>();
    this.incompleteGroups = new ArrayList<>();
    this.errorMessages = new ArrayList<>();

    // Initialize role counts
    for (ImageRole role : ImageRole.values()) {
      roleCounts.put(role, 0);
    }

    // Process each preview
    int matched = 0;
    boolean hasErrors = false;
    boolean hasWarnings = false;

    for (FilenamePreview preview : previews) {
      if (preview.isMatched()) {
        matched++;

        // Count roles
        ImageRole role = preview.getRole();
        if (role != null) {
          roleCounts.put(role, roleCounts.get(role) + 1);

          // Track group roles for completeness checking
          String groupId = preview.getGroupId();
          if (groupId != null && !groupId.trim().isEmpty()) {
            groupRoles.computeIfAbsent(groupId, k -> EnumSet.noneOf(ImageRole.class)).add(role);
          }
        }
      } else {
        unmatchedFilenames.add(preview.getFilename());
      }

      // Collect error messages
      if (preview.hasError()) {
        hasErrors = true;
        errorMessages.add(preview.getFilename() + ": " + preview.getErrorMessage());
      }
    }

    this.matchedFiles = matched;
    this.unmatchedFiles = totalFiles - matched;

    // Check for incomplete groups (groups missing required roles)
    // A group is considered complete if it has both FRONT and REAR roles
    // Groups with only OVERVIEW or missing either FRONT or REAR are incomplete
    for (Map.Entry<String, Set<ImageRole>> entry : groupRoles.entrySet()) {
      String groupId = entry.getKey();
      Set<ImageRole> roles = entry.getValue();

      // A group is complete only if it has both FRONT and REAR
      boolean hasFront = roles.contains(ImageRole.FRONT);
      boolean hasRear = roles.contains(ImageRole.REAR);

      if (!hasFront || !hasRear) {
        incompleteGroups.add(groupId);
        hasWarnings = true;
      }
    }

    this.hasErrors = hasErrors;
    this.hasWarnings = hasWarnings || !unmatchedFilenames.isEmpty();
  }

  /**
   * @return the total number of files in the preview
   */
  public int getTotalFiles() {
    return totalFiles;
  }

  /**
   * @return the number of files that were successfully matched and categorized
   */
  public int getMatchedFiles() {
    return matchedFiles;
  }

  /**
   * @return the number of files that could not be matched or categorized
   */
  public int getUnmatchedFiles() {
    return unmatchedFiles;
  }

  /**
   * @return the percentage of files that were successfully matched (0-100)
   */
  public double getMatchPercentage() {
    return totalFiles > 0 ? (matchedFiles * 100.0) / totalFiles : 0.0;
  }

  /**
   * @return a map of image roles to the number of files classified as each role
   */
  public Map<ImageRole, Integer> getRoleCounts() {
    return new EnumMap<>(roleCounts);
  }

  /**
   * Gets the count of files classified as the specified role.
   *
   * @param role the image role
   * @return the number of files classified as this role
   */
  public int getRoleCount(ImageRole role) {
    return roleCounts.getOrDefault(role, 0);
  }

  /**
   * @return the list of filenames that could not be matched or categorized
   */
  public List<String> getUnmatchedFilenames() {
    return new ArrayList<>(unmatchedFilenames);
  }

  /**
   * @return a map of group IDs to the set of roles found in each group
   */
  public Map<String, Set<ImageRole>> getGroupRoles() {
    Map<String, Set<ImageRole>> copy = new HashMap<>();
    for (Map.Entry<String, Set<ImageRole>> entry : groupRoles.entrySet()) {
      copy.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
    }
    return copy;
  }

  /**
   * @return the list of group IDs that are missing required image roles
   */
  public List<String> getIncompleteGroups() {
    return new ArrayList<>(incompleteGroups);
  }

  /**
   * @return the list of error messages encountered during processing
   */
  public List<String> getErrorMessages() {
    return new ArrayList<>(errorMessages);
  }

  /**
   * @return true if any errors were encountered during processing
   */
  public boolean hasErrors() {
    return hasErrors;
  }

  /**
   * @return true if any warnings were generated (incomplete groups, unmatched files)
   */
  public boolean hasWarnings() {
    return hasWarnings;
  }

  /**
   * @return true if the configuration appears to be working well (high match rate, few warnings)
   */
  public boolean isHealthy() {
    if (hasErrors) {
      return false;
    }

    // For empty configurations, consider healthy if no errors
    if (totalFiles == 0) {
      return true;
    }

    // Check match percentage
    if (getMatchPercentage() < 80.0) {
      return false;
    }

    // Check incomplete groups ratio (allow up to 20% incomplete groups, but not
    // exactly 20%)
    if (groupRoles.size() > 0) {
      double incompleteRatio = (double) incompleteGroups.size() / groupRoles.size();
      return incompleteRatio < 0.2;
    }

    return true;
  }

  /**
   * Generates a human-readable summary text.
   *
   * @return a formatted summary string
   */
  public String getSummaryText() {
    StringBuilder sb = new StringBuilder();

    sb.append(
        String.format(
            messages.getString("preview.summary.files"),
            totalFiles,
            matchedFiles,
            getMatchPercentage(),
            unmatchedFiles));

    if (matchedFiles > 0) {
      sb.append("\n").append(messages.getString("preview.role.distribution"));
      for (ImageRole role : ImageRole.values()) {
        int count = getRoleCount(role);
        if (count > 0) {
          sb.append(String.format(" %s: %d", role, count));
        }
      }
    }

    if (!incompleteGroups.isEmpty()) {
      sb.append("\n")
          .append(
              String.format(messages.getString("preview.role.missing"), incompleteGroups.size()));
    }

    if (hasErrors) {
      sb.append("\n")
          .append(String.format(messages.getString("preview.role.errors"), errorMessages.size()));
    }

    return sb.toString();
  }

  /**
   * Gets the total number of unique groups found.
   *
   * @return the number of unique group IDs
   */
  public int getGroupCount() {
    return groupRoles.size();
  }

  /**
   * Gets the number of complete groups (groups with both front and rear images).
   *
   * @return the number of complete groups
   */
  public int getCompleteGroupCount() {
    return getGroupCount() - incompleteGroups.size();
  }

  @Override
  public String toString() {
    return getSummaryText();
  }
}
