package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Engine for classifying filenames into image roles based on configurable rules. Implements
 * precedence-based classification where overview rules exclude files from front/rear consideration.
 */
public class RuleEngine {

  /**
   * Classifies a filename into an image role using the provided rules. Rules are applied in
   * precedence order (overview first, then front, then rear) to ensure proper exclusion logic.
   *
   * @param filename the filename to classify
   * @param rules the list of rules to apply
   * @return the classified image role, or null if no rules match
   * @throws IllegalArgumentException if filename is null or empty, or rules is null
   */
  public ImageRole classifyFilename(String filename, List<RoleRule> rules) {
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    if (rules == null) {
      throw new IllegalArgumentException("Rules cannot be null");
    }

    // Group rules by target role and sort by precedence
    Map<ImageRole, List<RoleRule>> rulesByRole =
        rules.stream().collect(Collectors.groupingBy(RoleRule::getTargetRole));

    // Process roles in precedence order (overview first, then front, then rear)
    List<ImageRole> roleOrder = Arrays.asList(ImageRole.OVERVIEW, ImageRole.FRONT, ImageRole.REAR);

    for (ImageRole role : roleOrder) {
      List<RoleRule> roleRules = rulesByRole.get(role);
      if (roleRules != null && !roleRules.isEmpty()) {
        // Sort rules by priority (lower values first)
        roleRules.sort(Comparator.comparingInt(RoleRule::getPriority));

        // Check if any rule matches
        for (RoleRule rule : roleRules) {
          if (matchesRule(filename, rule)) {
            return role;
          }
        }
      }
    }

    return null; // No matching rule found
  }

  /**
   * Checks if a filename matches a specific rule.
   *
   * @param filename the filename to check
   * @param rule the rule to apply
   * @return true if the filename matches the rule
   * @throws IllegalArgumentException if rule configuration is invalid
   */
  private boolean matchesRule(String filename, RoleRule rule) {
    if (rule.getRuleValue() == null || rule.getRuleValue().trim().isEmpty()) {
      return false; // Empty rule values don't match anything
    }

    String target = rule.isCaseSensitive() ? filename : filename.toLowerCase();
    String value = rule.isCaseSensitive() ? rule.getRuleValue() : rule.getRuleValue().toLowerCase();

    switch (rule.getRuleType()) {
      case EQUALS:
        return target.equals(value);

      case CONTAINS:
        return target.contains(value);

      case STARTS_WITH:
        return target.startsWith(value);

      case ENDS_WITH:
        return target.endsWith(value);

      case REGEX_OVERRIDE:
        try {
          Pattern pattern =
              rule.isCaseSensitive()
                  ? Pattern.compile(rule.getRuleValue())
                  : Pattern.compile(rule.getRuleValue(), Pattern.CASE_INSENSITIVE);
          return pattern.matcher(filename).matches();
        } catch (PatternSyntaxException e) {
          throw new IllegalArgumentException(
              "Invalid regex pattern in rule: " + rule.getRuleValue(), e);
        }

      default:
        throw new IllegalArgumentException("Unsupported rule type: " + rule.getRuleType());
    }
  }

  /**
   * Generates a regex pattern for all rules targeting a specific image role. The generated pattern
   * will match any filename that would be classified as the target role.
   *
   * @param rules the list of rules to convert
   * @param targetRole the image role to generate a pattern for
   * @return a regex pattern string, or null if no rules target the specified role
   * @throws IllegalArgumentException if rules is null or targetRole is null
   */
  public String generateRegexPattern(List<RoleRule> rules, ImageRole targetRole) {
    if (rules == null) {
      throw new IllegalArgumentException("Rules cannot be null");
    }
    if (targetRole == null) {
      throw new IllegalArgumentException("Target role cannot be null");
    }

    // Filter rules for the target role and sort by priority
    List<RoleRule> targetRules =
        rules.stream()
            .filter(rule -> rule.getTargetRole() == targetRole)
            .sorted(Comparator.comparingInt(RoleRule::getPriority))
            .collect(Collectors.toList());

    if (targetRules.isEmpty()) {
      return null;
    }

    // Generate regex alternatives for each rule
    List<String> patterns = new ArrayList<>();

    for (RoleRule rule : targetRules) {
      String pattern = generateSingleRulePattern(rule);
      if (pattern != null && !pattern.trim().isEmpty()) {
        patterns.add(pattern);
      }
    }

    if (patterns.isEmpty()) {
      return null;
    }

    // Combine patterns with OR logic
    if (patterns.size() == 1) {
      return patterns.get(0);
    } else {
      return "(" + String.join("|", patterns) + ")";
    }
  }

  /**
   * Generates a regex pattern for a single rule.
   *
   * @param rule the rule to convert to regex
   * @return a regex pattern string
   * @throws IllegalArgumentException if rule configuration is invalid
   */
  private String generateSingleRulePattern(RoleRule rule) {
    if (rule.getRuleValue() == null || rule.getRuleValue().trim().isEmpty()) {
      return null;
    }

    String value = Pattern.quote(rule.getRuleValue()); // Escape special regex characters
    String flags = rule.isCaseSensitive() ? "" : "(?i)"; // Case-insensitive flag

    switch (rule.getRuleType()) {
      case EQUALS:
        return flags + "^" + value + "$";

      case CONTAINS:
        return flags + ".*" + value + ".*";

      case STARTS_WITH:
        return flags + "^" + value + ".*";

      case ENDS_WITH:
        return flags + ".*" + value + "$";

      case REGEX_OVERRIDE:
        // Validate the regex pattern
        try {
          Pattern.compile(rule.getRuleValue());
          return rule.isCaseSensitive() ? rule.getRuleValue() : "(?i)" + rule.getRuleValue();
        } catch (PatternSyntaxException e) {
          throw new IllegalArgumentException(
              "Invalid regex pattern in rule: " + rule.getRuleValue(), e);
        }

      default:
        throw new IllegalArgumentException("Unsupported rule type: " + rule.getRuleType());
    }
  }

  /**
   * Validates a list of rules for common configuration errors.
   *
   * @param rules the rules to validate
   * @return a validation result with any errors or warnings found
   */
  public ValidationResult validateRules(List<RoleRule> rules) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();

    if (rules == null) {
      errors.add(
          new ValidationError(
              ValidationErrorType.INVALID_RULE_CONFIGURATION, "Rules list cannot be null"));
      return new ValidationResult(false, errors, warnings);
    }

    // Check for empty rule values
    for (int i = 0; i < rules.size(); i++) {
      RoleRule rule = rules.get(i);
      if (rule.getRuleValue() == null || rule.getRuleValue().trim().isEmpty()) {
        warnings.add(
            new ValidationWarning(
                ValidationWarningType.EMPTY_RULE_VALUE,
                "Rule " + (i + 1) + " has empty value and will not match any files"));
      }

      // Validate regex patterns
      if (rule.getRuleType() == RuleType.REGEX_OVERRIDE) {
        try {
          Pattern.compile(rule.getRuleValue());
        } catch (PatternSyntaxException e) {
          errors.add(
              new ValidationError(
                  ValidationErrorType.INVALID_REGEX_PATTERN,
                  "Rule " + (i + 1) + " has invalid regex pattern: " + e.getMessage()));
        }
      }
    }

    // Check if all roles have at least one rule
    Set<ImageRole> rolesWithRules =
        rules.stream().map(RoleRule::getTargetRole).collect(Collectors.toSet());

    for (ImageRole role : ImageRole.values()) {
      if (!rolesWithRules.contains(role)) {
        warnings.add(
            new ValidationWarning(
                ValidationWarningType.MISSING_ROLE_RULES,
                "No rules defined for "
                    + role
                    + " role - files will not be classified as "
                    + role));
      }
    }

    boolean isValid = errors.isEmpty();
    return new ValidationResult(isValid, errors, warnings);
  }

  /**
   * Classifies multiple filenames and returns a summary of the results.
   *
   * @param filenames the filenames to classify
   * @param rules the rules to apply
   * @return a map of image roles to lists of matching filenames
   * @throws IllegalArgumentException if parameters are invalid
   */
  public Map<ImageRole, List<String>> classifyFilenames(
      List<String> filenames, List<RoleRule> rules) {
    if (filenames == null) {
      throw new IllegalArgumentException("Filenames cannot be null");
    }
    if (rules == null) {
      throw new IllegalArgumentException("Rules cannot be null");
    }

    Map<ImageRole, List<String>> results = new EnumMap<>(ImageRole.class);

    // Initialize empty lists for all roles
    for (ImageRole role : ImageRole.values()) {
      results.put(role, new ArrayList<>());
    }

    // Classify each filename
    for (String filename : filenames) {
      if (filename != null && !filename.trim().isEmpty()) {
        ImageRole role = classifyFilename(filename, rules);
        if (role != null) {
          results.get(role).add(filename);
        }
      }
    }

    return results;
  }
}
