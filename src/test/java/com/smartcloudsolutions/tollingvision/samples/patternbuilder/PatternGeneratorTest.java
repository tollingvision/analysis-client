package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for PatternGenerator service covering regex generation accuracy and
 * validation logic.
 */
class PatternGeneratorTest {

  private PatternGenerator patternGenerator;

  @BeforeEach
  void setUp() {
    patternGenerator = new PatternGenerator();
  }

  @Nested
  @DisplayName("Group Pattern Generation Tests")
  class GroupPatternGenerationTests {

    @Test
    @DisplayName("Should generate group pattern with capturing group around selected token")
    void shouldGenerateGroupPatternWithCapturingGroup() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(
              new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9),
              new FilenameToken("001", 1, TokenType.GROUP_ID, 0.8),
              new FilenameToken("front", 2, TokenType.CAMERA_SIDE, 0.9),
              new FilenameToken("jpg", 3, TokenType.EXTENSION, 1.0));

      // Act
      String pattern =
          patternGenerator.generateGroupPattern(
              tokens, TokenType.GROUP_ID, new java.util.HashSet<>());

      // Assert
      assertNotNull(pattern);
      assertTrue(
          pattern.contains("(\\d") || pattern.contains("(\\w"),
          "Should contain capturing group around group ID (with length quantifier)");
      assertTrue(pattern.contains("vehicle"), "Should contain prefix token");
      // Camera side is generic \w+ in group patterns (role detection is separate)
      assertTrue(
          pattern.contains("\\w+") || pattern.contains("(?i:front|f|fr|forward)"),
          "Should contain camera side pattern (generic or specific)");
      assertTrue(pattern.contains("(?i:jpg)"), "Should contain extension pattern");
    }

    @Test
    @DisplayName("Should handle empty token list")
    void shouldHandleEmptyTokenList() {
      // Act
      String pattern =
          patternGenerator.generateGroupPattern(List.of(), null, new java.util.HashSet<>());

      // Assert
      assertEquals("", pattern);
    }

    @Test
    @DisplayName("Should throw exception when group ID token is null")
    void shouldThrowExceptionWhenGroupIdTokenIsNull() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9));

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> patternGenerator.generateGroupPattern(tokens, null, new java.util.HashSet<>()));
    }

    @Test
    @DisplayName("Should throw exception when group ID token not in list")
    void shouldThrowExceptionWhenGroupIdTokenNotInList() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9));

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () ->
              patternGenerator.generateGroupPattern(
                  tokens, TokenType.GROUP_ID, new java.util.HashSet<>()));
    }

    @Test
    @DisplayName("Should generate pattern for date tokens")
    void shouldGeneratePatternForDateTokens() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(
              new FilenameToken("2024-01-15", 0, TokenType.DATE, 0.9),
              new FilenameToken("ABC123", 1, TokenType.GROUP_ID, 0.8));

      // Act
      String pattern =
          patternGenerator.generateGroupPattern(
              tokens, TokenType.GROUP_ID, new java.util.HashSet<>());

      // Assert
      assertTrue(
          pattern.contains("[\\w\\-") || pattern.contains("\\d{") || pattern.contains("\\w{"),
          "Should contain date pattern based on actual token characteristics");
      assertTrue(
          pattern.contains("(\\w") || pattern.contains("(\\d") || pattern.contains("([\\w"),
          "Should contain capturing group");
    }

    @Test
    @DisplayName("Should generate pattern for index tokens")
    void shouldGeneratePatternForIndexTokens() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(
              new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9),
              new FilenameToken("001", 1, TokenType.INDEX, 0.8),
              new FilenameToken("ABC123", 2, TokenType.GROUP_ID, 0.8));

      // Act
      String pattern =
          patternGenerator.generateGroupPattern(
              tokens, TokenType.GROUP_ID, new java.util.HashSet<>());

      // Assert
      assertTrue(pattern.contains("\\d"), "Should contain digit pattern for index");
      assertTrue(
          pattern.contains("(\\w") || pattern.contains("(\\d") || pattern.contains("(\\w"),
          "Should contain capturing group");
    }
  }

  @Nested
  @DisplayName("Role Pattern Generation Tests")
  class RolePatternGenerationTests {

    @Test
    @DisplayName("Should generate role pattern for single rule")
    void shouldGenerateRolePatternForSingleRule() {
      // Arrange
      RoleRule rule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);

      // Assert
      assertEquals("(?i:.*front.*)", pattern);
    }

    @Test
    @DisplayName("Should generate role pattern for multiple rules with OR logic")
    void shouldGenerateRolePatternForMultipleRules() {
      // Arrange
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "f", false, 2));

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);

      // Assert
      assertTrue(pattern.startsWith("(?:"));
      assertTrue(pattern.contains("|"));
      assertTrue(pattern.contains("front"));
      assertTrue(pattern.contains("f"));
    }

    @Test
    @DisplayName("Should handle case sensitive rules")
    void shouldHandleCaseSensitiveRules() {
      // Arrange
      RoleRule rule = new RoleRule(ImageRole.FRONT, RuleType.EQUALS, "FRONT", true, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);

      // Assert
      assertEquals("^FRONT$", pattern);
      assertFalse(pattern.contains("(?i:"));
    }

    @Test
    @DisplayName("Should generate pattern for STARTS_WITH rule")
    void shouldGeneratePatternForStartsWithRule() {
      // Arrange
      RoleRule rule = new RoleRule(ImageRole.REAR, RuleType.STARTS_WITH, "rear", false, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.REAR);

      // Assert
      assertEquals("(?i:^rear.*)", pattern);
    }

    @Test
    @DisplayName("Should generate pattern for ENDS_WITH rule")
    void shouldGeneratePatternForEndsWithRule() {
      // Arrange
      RoleRule rule = new RoleRule(ImageRole.OVERVIEW, RuleType.ENDS_WITH, "scene", false, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.OVERVIEW);

      // Assert
      assertEquals("(?i:.*scene$)", pattern);
    }

    @Test
    @DisplayName("Should handle REGEX_OVERRIDE rule")
    void shouldHandleRegexOverrideRule() {
      // Arrange
      String customRegex = "^(front|rear)_\\d+$";
      RoleRule rule = new RoleRule(ImageRole.FRONT, RuleType.REGEX_OVERRIDE, customRegex, false, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);

      // Assert
      assertEquals(customRegex, pattern);
    }

    @Test
    @DisplayName("Should return empty string for no matching rules")
    void shouldReturnEmptyStringForNoMatchingRules() {
      // Arrange
      RoleRule rule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1);
      List<RoleRule> rules = List.of(rule);

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.REAR);

      // Assert
      assertEquals("", pattern);
    }

    @Test
    @DisplayName("Should sort rules by priority")
    void shouldSortRulesByPriority() {
      // Arrange
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "second", false, 2),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "first", false, 1));

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);

      // Assert
      // First rule should appear first in the OR pattern
      assertTrue(pattern.indexOf("first") < pattern.indexOf("second"));
    }
  }

  @Nested
  @DisplayName("Pattern Validation Tests")
  class PatternValidationTests {

    @Test
    @DisplayName("Should validate successful configuration")
    void shouldValidateSuccessfulConfiguration() {
      // Arrange
      PatternConfiguration config = createValidConfiguration();

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertTrue(result.isValid());
      assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Should detect missing group ID token")
    void shouldDetectMissingGroupIdToken() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("");
      config.setGroupIdToken(null);

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(result.hasErrors());
      assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("Group ID")));
    }

    @Test
    @DisplayName("Should detect invalid capturing groups")
    void shouldDetectInvalidCapturingGroups() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("vehicle_\\d+_front"); // No capturing group
      config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(
          result.getErrorMessages().stream().anyMatch(msg -> msg.contains("capturing group")));
    }

    @Test
    @DisplayName("Should detect multiple capturing groups")
    void shouldDetectMultipleCapturingGroups() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("(vehicle)_(\\d+)_front"); // Two capturing groups
      config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(result.hasErrors());
      assertTrue(
          result.getErrorMessages().stream()
              .anyMatch(msg -> msg.toLowerCase().contains("capturing group")));
    }

    @Test
    @DisplayName("Should detect missing role patterns")
    void shouldDetectMissingRolePatterns() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("vehicle_(\\d+)_front");
      // No role rules or patterns

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("role")));
    }

    @Test
    @DisplayName("Should detect invalid regex syntax")
    void shouldDetectInvalidRegexSyntax() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("vehicle_(\\d+)_front");
      config.setFrontPattern("[invalid"); // Invalid regex
      config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("regex syntax")));
    }

    @Test
    @DisplayName("Should detect empty rule values")
    void shouldDetectEmptyRuleValues() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("vehicle_(\\d+)_front");
      config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "", false, 1));

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertFalse(result.isValid());
      assertTrue(
          result.getErrorMessages().stream()
              .anyMatch(msg -> msg.contains("Rule value cannot be empty")));
    }

    @Test
    @DisplayName("Should generate warnings for missing overview patterns")
    void shouldGenerateWarningsForMissingOverviewPatterns() {
      // Arrange
      PatternConfiguration config = new PatternConfiguration();
      config.setGroupPattern("vehicle_(\\d+)_front");
      config.setFrontPattern(".*front.*");
      config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));
      // No overview pattern

      // Act
      ValidationResult result = patternGenerator.validatePatterns(config);

      // Assert
      assertTrue(result.isValid()); // Should still be valid
      assertTrue(result.hasWarnings());
      assertTrue(result.getWarningMessages().stream().anyMatch(msg -> msg.contains("overview")));
    }

    @Test
    @DisplayName("Should handle null configuration")
    void shouldHandleNullConfiguration() {
      // Act
      ValidationResult result = patternGenerator.validatePatterns(null);

      // Assert
      assertFalse(result.isValid());
      assertTrue(result.hasErrors());
    }
  }

  @Nested
  @DisplayName("Regex Accuracy Tests")
  class RegexAccuracyTests {

    @Test
    @DisplayName("Generated group pattern should match expected filenames")
    void generatedGroupPatternShouldMatchExpectedFilenames() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(
              new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9),
              new FilenameToken("001", 1, TokenType.GROUP_ID, 0.8),
              new FilenameToken("front", 2, TokenType.CAMERA_SIDE, 0.9),
              new FilenameToken("jpg", 3, TokenType.EXTENSION, 1.0));

      // Act
      String pattern =
          patternGenerator.generateGroupPattern(
              tokens, TokenType.GROUP_ID, new java.util.HashSet<>());
      Pattern compiledPattern = Pattern.compile(pattern);

      // Assert - test with files that match the expected pattern structure
      assertTrue(compiledPattern.matcher("vehicle_001_front.jpg").matches());
      assertTrue(compiledPattern.matcher("vehicle_123_front.jpg").matches());
      assertTrue(compiledPattern.matcher("vehicle_789_front.jpg").matches());

      // Test capturing group extraction (GROUP_ID "001" is numeric, so pattern is
      // \d+)
      var matcher = compiledPattern.matcher("vehicle_001_front.jpg");
      assertTrue(matcher.matches());
      assertEquals("001", matcher.group(1));
    }

    @Test
    @DisplayName("Generated role pattern should match expected filenames")
    void generatedRolePatternShouldMatchExpectedFilenames() {
      // Arrange
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "f", false, 2));

      // Act
      String pattern = patternGenerator.generateRolePattern(rules, ImageRole.FRONT);
      Pattern compiledPattern = Pattern.compile(pattern);

      // Assert
      assertTrue(compiledPattern.matcher("vehicle_001_front.jpg").matches());
      assertTrue(compiledPattern.matcher("vehicle_001_f.jpg").matches());
      assertTrue(compiledPattern.matcher("vehicle_001_FRONT.jpg").matches()); // Case insensitive
      assertFalse(compiledPattern.matcher("vehicle_001_rear.jpg").matches());
    }

    @Test
    @DisplayName("Should escape special regex characters")
    void shouldEscapeSpecialRegexCharacters() {
      // Arrange
      List<FilenameToken> tokens =
          Arrays.asList(
              new FilenameToken("test[1]", 0, TokenType.PREFIX, 0.9),
              new FilenameToken("001", 1, TokenType.GROUP_ID, 0.8));

      // Act
      String pattern =
          patternGenerator.generateGroupPattern(
              tokens, TokenType.GROUP_ID, new java.util.HashSet<>());

      // Assert
      assertTrue(pattern.contains("test\\[1\\]"), "Should escape square brackets");

      // Test that the pattern compiles without errors
      assertDoesNotThrow(() -> Pattern.compile(pattern));
    }
  }

  /** Creates a valid pattern configuration for testing. */
  private PatternConfiguration createValidConfiguration() {
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("vehicle_(\\d+)_.*");
    config.setFrontPattern(".*front.*");
    config.setRearPattern(".*rear.*");
    config.setOverviewPattern(".*overview.*");

    config.addRoleRule(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));
    config.addRoleRule(new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 1));
    config.addRoleRule(new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 1));

    return config;
  }
}
