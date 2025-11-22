package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Engine for grouping files by extracting group IDs, separate from role detection. This ensures
 * that grouping only uses the group ID token and doesn't include camera side/role information.
 */
public class GroupingEngine {

  /** Result of grouping operation with extracted group IDs and role assignments. */
  public static class GroupingResult {
    private final Map<String, List<String>> groups; // groupId -> list of filenames
    private final Map<String, String> fileToGroupId; // filename -> groupId
    private final Map<String, ImageRole> fileToRole; // filename -> role
    private final List<String> unmatchedFiles; // files that didn't match
    private final Map<String, String> unmatchedReasons; // filename -> reason

    public GroupingResult(
        Map<String, List<String>> groups,
        Map<String, String> fileToGroupId,
        Map<String, ImageRole> fileToRole,
        List<String> unmatchedFiles,
        Map<String, String> unmatchedReasons) {
      this.groups = Map.copyOf(groups);
      this.fileToGroupId = Map.copyOf(fileToGroupId);
      this.fileToRole = Map.copyOf(fileToRole);
      this.unmatchedFiles = List.copyOf(unmatchedFiles);
      this.unmatchedReasons = Map.copyOf(unmatchedReasons);
    }

    public Map<String, List<String>> getGroups() {
      return groups;
    }

    public Map<String, String> getFileToGroupId() {
      return fileToGroupId;
    }

    public Map<String, ImageRole> getFileToRole() {
      return fileToRole;
    }

    public List<String> getUnmatchedFiles() {
      return unmatchedFiles;
    }

    public Map<String, String> getUnmatchedReasons() {
      return unmatchedReasons;
    }

    public int getTotalFiles() {
      return fileToGroupId.size() + unmatchedFiles.size();
    }

    public int getMatchedFiles() {
      return fileToGroupId.size();
    }

    public int getGroupCount() {
      return groups.size();
    }

    public int getGroupsWithMissingRoles() {
      return unmatchedFiles.size();
    }
  }

  /**
   * Groups files using the group pattern and then assigns roles within each group.
   *
   * @param filenames list of filenames to process
   * @param groupPattern regex pattern for extracting group ID (must have exactly one capturing
   *     group)
   * @param roleRules list of role rules for assigning image roles
   * @param unknownSegmentHandler handler for unknown segments
   * @return grouping result with group assignments and role assignments
   */
  public GroupingResult groupAndAssignRoles(
      List<String> filenames,
      String groupPattern,
      List<RoleRule> roleRules,
      UnknownSegmentHandler unknownSegmentHandler) {

    Map<String, List<String>> groups = new HashMap<>();
    Map<String, String> fileToGroupId = new HashMap<>();
    Map<String, ImageRole> fileToRole = new HashMap<>();
    List<String> unmatchedFiles = new ArrayList<>();
    Map<String, String> unmatchedReasons = new HashMap<>();

    // Step 1: Group files by extracting group IDs
    Pattern groupRegex;
    try {
      groupRegex = Pattern.compile(groupPattern, Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException e) {
      // If pattern is invalid, all files are unmatched
      for (String filename : filenames) {
        unmatchedFiles.add(filename);
        unmatchedReasons.put(filename, "Invalid group pattern: " + e.getMessage());
      }
      return new GroupingResult(
          groups, fileToGroupId, fileToRole, unmatchedFiles, unmatchedReasons);
    }

    // Extract group IDs from filenames
    for (String filename : filenames) {
      Matcher matcher = groupRegex.matcher(filename);
      if (matcher.find() && matcher.groupCount() >= 1) {
        String groupId = matcher.group(1);
        if (groupId != null && !groupId.trim().isEmpty()) {
          groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(filename);
          fileToGroupId.put(filename, groupId);
        } else {
          unmatchedFiles.add(filename);
          unmatchedReasons.put(filename, "Group pattern matched but captured empty group ID");
        }
      } else {
        unmatchedFiles.add(filename);
        unmatchedReasons.put(filename, "Filename doesn't match group pattern");
      }
    }

    // Step 2: Assign roles within each group
    assignRolesWithinGroups(groups, roleRules, fileToRole, unmatchedFiles, unmatchedReasons);

    return new GroupingResult(groups, fileToGroupId, fileToRole, unmatchedFiles, unmatchedReasons);
  }

  /**
   * Assigns roles to files within each group using role rules. Overview rules are applied first and
   * exclude files from front/rear consideration.
   */
  private void assignRolesWithinGroups(
      Map<String, List<String>> groups,
      List<RoleRule> roleRules,
      Map<String, ImageRole> fileToRole,
      List<String> unmatchedFiles,
      Map<String, String> unmatchedReasons) {

    // Group rules by target role and sort by priority
    Map<ImageRole, List<RoleRule>> rulesByRole = new HashMap<>();
    for (RoleRule rule : roleRules) {
      rulesByRole.computeIfAbsent(rule.getTargetRole(), k -> new ArrayList<>()).add(rule);
    }

    // Sort rules by priority within each role
    for (List<RoleRule> rules : rulesByRole.values()) {
      rules.sort(Comparator.comparingInt(RoleRule::getPriority));
    }

    // Process each group
    for (Map.Entry<String, List<String>> groupEntry : groups.entrySet()) {
      List<String> groupFiles = new ArrayList<>(groupEntry.getValue());
      Set<String> assignedFiles = new HashSet<>();

      // Step 1: Apply overview rules first (highest precedence)
      List<RoleRule> overviewRules = rulesByRole.getOrDefault(ImageRole.OVERVIEW, List.of());
      for (String filename : groupFiles) {
        if (matchesAnyRule(filename, overviewRules)) {
          fileToRole.put(filename, ImageRole.OVERVIEW);
          assignedFiles.add(filename);
        }
      }

      // Step 2: Apply front rules to remaining files
      List<RoleRule> frontRules = rulesByRole.getOrDefault(ImageRole.FRONT, List.of());
      for (String filename : groupFiles) {
        if (!assignedFiles.contains(filename) && matchesAnyRule(filename, frontRules)) {
          fileToRole.put(filename, ImageRole.FRONT);
          assignedFiles.add(filename);
        }
      }

      // Step 3: Apply rear rules to remaining files
      List<RoleRule> rearRules = rulesByRole.getOrDefault(ImageRole.REAR, List.of());
      for (String filename : groupFiles) {
        if (!assignedFiles.contains(filename) && matchesAnyRule(filename, rearRules)) {
          fileToRole.put(filename, ImageRole.REAR);
          assignedFiles.add(filename);
        }
      }

      // Step 4: Handle unassigned files in the group
      for (String filename : groupFiles) {
        if (!assignedFiles.contains(filename)) {
          // Remove from successful matches and add to unmatched
          unmatchedFiles.add(filename);
          unmatchedReasons.put(filename, "No role rules matched this file");
        }
      }

      // Remove unmatched files from the group
      groupEntry.getValue().removeAll(unmatchedFiles);
    }

    // Remove empty groups
    groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  /** Checks if a filename matches any of the provided rules. */
  private boolean matchesAnyRule(String filename, List<RoleRule> rules) {
    for (RoleRule rule : rules) {
      if (matchesRule(filename, rule)) {
        return true;
      }
    }
    return false;
  }

  /** Checks if a filename matches a specific rule. */
  private boolean matchesRule(String filename, RoleRule rule) {
    String ruleValue = rule.getRuleValue();
    if (ruleValue == null || ruleValue.trim().isEmpty()) {
      return false;
    }

    String target = rule.isCaseSensitive() ? filename : filename.toLowerCase();
    String value = rule.isCaseSensitive() ? ruleValue : ruleValue.toLowerCase();

    return switch (rule.getRuleType()) {
      case EQUALS -> target.equals(value);
      case CONTAINS -> target.contains(value);
      case STARTS_WITH -> target.startsWith(value);
      case ENDS_WITH -> target.endsWith(value);
      case REGEX_OVERRIDE -> {
        try {
          Pattern pattern =
              Pattern.compile(ruleValue, rule.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
          yield pattern.matcher(filename).find();
        } catch (PatternSyntaxException e) {
          yield false;
        }
      }
    };
  }

  /**
   * Validates that a group pattern has exactly one capturing group.
   *
   * @param groupPattern the pattern to validate
   * @return validation result
   */
  public ValidationResult validateGroupPattern(String groupPattern) {
    if (groupPattern == null || groupPattern.trim().isEmpty()) {
      return ValidationResult.failure(ValidationError.of(ValidationErrorType.EMPTY_GROUP_PATTERN));
    }

    try {
      Pattern.compile(groupPattern);

      // Count capturing groups
      int capturingGroups = countCapturingGroups(groupPattern);

      if (capturingGroups == 0) {
        return ValidationResult.failure(
            ValidationError.of(ValidationErrorType.NO_CAPTURING_GROUPS));
      } else if (capturingGroups > 1) {
        return ValidationResult.failure(
            ValidationError.of(
                ValidationErrorType.MULTIPLE_CAPTURING_GROUPS,
                "Pattern contains " + capturingGroups + " capturing groups - only one is allowed"));
      }

      return ValidationResult.success();

    } catch (PatternSyntaxException e) {
      return ValidationResult.failure(
          ValidationError.of(
              ValidationErrorType.REGEX_SYNTAX_ERROR, "Invalid regex syntax: " + e.getMessage()));
    }
  }

  /** Counts the number of capturing groups in a regex pattern. */
  private int countCapturingGroups(String pattern) {
    if (pattern == null) {
      return 0;
    }

    int count = 0;
    boolean inCharClass = false;
    boolean escaped = false;

    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '[') {
        inCharClass = true;
      } else if (c == ']') {
        inCharClass = false;
      } else if (!inCharClass && c == '(' && i + 1 < pattern.length()) {
        // Check if it's a non-capturing group
        if (pattern.charAt(i + 1) != '?') {
          count++;
        }
      }
    }

    return count;
  }
}
