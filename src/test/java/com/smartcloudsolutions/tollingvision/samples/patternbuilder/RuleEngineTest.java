package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for RuleEngine class. Tests role classification accuracy, precedence
 * handling, regex generation, and error handling for various rule configurations.
 */
class RuleEngineTest {

  private RuleEngine ruleEngine;

  @BeforeEach
  void setUp() {
    ruleEngine = new RuleEngine();
  }

  @Nested
  @DisplayName("Filename Classification Tests")
  class FilenameClassificationTests {

    @Test
    @DisplayName("Should classify filename with EQUALS rule")
    void shouldClassifyWithEqualsRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.EQUALS, "front", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.EQUALS, "rear", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("front", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("rear", rules));
      assertNull(ruleEngine.classifyFilename("overview", rules));
    }

    @Test
    @DisplayName("Should classify filename with CONTAINS rule")
    void shouldClassifyWithContainsRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_001_front.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_001_rear.jpg", rules));
      assertNull(ruleEngine.classifyFilename("vehicle_001_side.jpg", rules));
    }

    @Test
    @DisplayName("Should classify filename with STARTS_WITH rule")
    void shouldClassifyWithStartsWithRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.STARTS_WITH, "front_", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.STARTS_WITH, "rear_", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("front_vehicle_001.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("rear_vehicle_001.jpg", rules));
      assertNull(ruleEngine.classifyFilename("vehicle_front_001.jpg", rules));
    }

    @Test
    @DisplayName("Should classify filename with ENDS_WITH rule")
    void shouldClassifyWithEndsWithRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.ENDS_WITH, "_front.jpg", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.ENDS_WITH, "_rear.jpg", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_001_front.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_001_rear.jpg", rules));
      assertNull(ruleEngine.classifyFilename("vehicle_001_front.png", rules));
    }

    @Test
    @DisplayName("Should classify filename with REGEX_OVERRIDE rule")
    void shouldClassifyWithRegexOverrideRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.REGEX_OVERRIDE, ".*[fF]ront.*", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.REGEX_OVERRIDE, ".*[rR]ear.*", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_Front_001.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_Rear_001.jpg", rules));
      assertNull(ruleEngine.classifyFilename("vehicle_side_001.jpg", rules));
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void shouldHandleCaseSensitivity() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "FRONT", true, 1), // Case sensitive
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2) // Case insensitive
              );

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_FRONT_001.jpg", rules));
      assertNull(
          ruleEngine.classifyFilename(
              "vehicle_front_001.jpg", rules)); // Should not match case-sensitive rule

      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_REAR_001.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_rear_001.jpg", rules));
    }
  }

  @Nested
  @DisplayName("Precedence and Priority Tests")
  class PrecedenceTests {

    @Test
    @DisplayName("Should apply overview precedence correctly")
    void shouldApplyOverviewPrecedence() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 1),
              new RoleRule(
                  ImageRole.FRONT,
                  RuleType.CONTAINS,
                  "view",
                  false,
                  2), // Would match "overview" too
              new RoleRule(
                  ImageRole.REAR, RuleType.CONTAINS, "over", false, 3) // Would match "overview" too
              );

      // Overview should take precedence even though other rules would also match
      assertEquals(
          ImageRole.OVERVIEW, ruleEngine.classifyFilename("vehicle_overview_001.jpg", rules));
      assertEquals(
          ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_frontview_001.jpg", rules));
    }

    @Test
    @DisplayName("Should process roles in correct precedence order")
    void shouldProcessRolesInPrecedenceOrder() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(
                  ImageRole.REAR,
                  RuleType.CONTAINS,
                  "cam",
                  false,
                  1), // Would match first alphabetically
              new RoleRule(
                  ImageRole.FRONT,
                  RuleType.CONTAINS,
                  "cam",
                  false,
                  2), // Would match first alphabetically
              new RoleRule(
                  ImageRole.OVERVIEW,
                  RuleType.CONTAINS,
                  "cam",
                  false,
                  3) // Should match due to precedence
              );

      // Overview should win due to role precedence, not rule priority
      assertEquals(ImageRole.OVERVIEW, ruleEngine.classifyFilename("cam_001.jpg", rules));
    }

    @Test
    @DisplayName("Should respect rule priority within same role")
    void shouldRespectRulePriorityWithinRole() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(
                  ImageRole.FRONT, RuleType.CONTAINS, "specific", false, 1), // Higher priority
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2) // Lower priority
              );

      // Should match the higher priority rule first
      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("specific_front_001.jpg", rules));
      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("general_front_001.jpg", rules));
    }
  }

  @Nested
  @DisplayName("Regex Pattern Generation Tests")
  class RegexPatternGenerationTests {

    @Test
    @DisplayName("Should generate regex for EQUALS rule")
    void shouldGenerateRegexForEqualsRule() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.EQUALS, "front", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("(?i)^\\Qfront\\E$", pattern);
    }

    @Test
    @DisplayName("Should generate regex for CONTAINS rule")
    void shouldGenerateRegexForContainsRule() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("(?i).*\\Qfront\\E.*", pattern);
    }

    @Test
    @DisplayName("Should generate regex for STARTS_WITH rule")
    void shouldGenerateRegexForStartsWithRule() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.STARTS_WITH, "front", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("(?i)^\\Qfront\\E.*", pattern);
    }

    @Test
    @DisplayName("Should generate regex for ENDS_WITH rule")
    void shouldGenerateRegexForEndsWithRule() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.ENDS_WITH, "front", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("(?i).*\\Qfront\\E$", pattern);
    }

    @Test
    @DisplayName("Should generate case-sensitive regex when specified")
    void shouldGenerateCaseSensitiveRegex() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "FRONT", true, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals(".*\\QFRONT\\E.*", pattern);
    }

    @Test
    @DisplayName("Should combine multiple rules with OR logic")
    void shouldCombineMultipleRulesWithOr() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "forward", false, 2));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("((?i).*\\Qfront\\E.*|(?i).*\\Qforward\\E.*)", pattern);
    }

    @Test
    @DisplayName("Should handle REGEX_OVERRIDE rule")
    void shouldHandleRegexOverrideRule() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.REGEX_OVERRIDE, ".*[fF]ront.*", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.FRONT);
      assertEquals("(?i).*[fF]ront.*", pattern);
    }

    @Test
    @DisplayName("Should return null for role with no rules")
    void shouldReturnNullForRoleWithNoRules() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      String pattern = ruleEngine.generateRegexPattern(rules, ImageRole.REAR);
      assertNull(pattern);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw exception for null filename")
    void shouldThrowExceptionForNullFilename() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      assertThrows(IllegalArgumentException.class, () -> ruleEngine.classifyFilename(null, rules));
    }

    @Test
    @DisplayName("Should throw exception for empty filename")
    void shouldThrowExceptionForEmptyFilename() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      assertThrows(IllegalArgumentException.class, () -> ruleEngine.classifyFilename("", rules));
      assertThrows(IllegalArgumentException.class, () -> ruleEngine.classifyFilename("   ", rules));
    }

    @Test
    @DisplayName("Should throw exception for null rules")
    void shouldThrowExceptionForNullRules() {
      assertThrows(
          IllegalArgumentException.class, () -> ruleEngine.classifyFilename("test.jpg", null));
    }

    @Test
    @DisplayName("Should handle invalid regex pattern")
    void shouldHandleInvalidRegexPattern() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.REGEX_OVERRIDE, "[invalid", false, 1));

      assertThrows(
          IllegalArgumentException.class, () -> ruleEngine.classifyFilename("test.jpg", rules));
    }

    @Test
    @DisplayName("Should handle empty rule values gracefully")
    void shouldHandleEmptyRuleValues() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

      // Should not match empty rule, but should match valid rule
      assertNull(ruleEngine.classifyFilename("front.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("rear.jpg", rules));
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate rules successfully")
    void shouldValidateRulesSuccessfully() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 1),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 3));

      ValidationResult result = ruleEngine.validateRules(rules);
      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should detect empty rule values")
    void shouldDetectEmptyRuleValues() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

      ValidationResult result = ruleEngine.validateRules(rules);
      assertTrue(result.isValid()); // Warnings don't make it invalid

      // Should have warnings for empty rule value AND missing overview rules
      assertTrue(result.getWarnings().size() >= 1);

      // Check that at least one warning is for empty rule value
      boolean hasEmptyRuleWarning =
          result.getWarnings().stream()
              .anyMatch(w -> w.getType() == ValidationWarningType.EMPTY_RULE_VALUE);
      assertTrue(hasEmptyRuleWarning);
    }

    @Test
    @DisplayName("Should detect invalid regex patterns")
    void shouldDetectInvalidRegexPatterns() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.REGEX_OVERRIDE, "[invalid", false, 1));

      ValidationResult result = ruleEngine.validateRules(rules);
      assertFalse(result.isValid());
      assertEquals(1, result.getErrors().size());
      assertEquals(ValidationErrorType.INVALID_REGEX_PATTERN, result.getErrors().get(0).getType());
    }

    @Test
    @DisplayName("Should detect missing role rules")
    void shouldDetectMissingRoleRules() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1)
              // Missing OVERVIEW and REAR rules
              );

      ValidationResult result = ruleEngine.validateRules(rules);
      assertTrue(result.isValid()); // Warnings don't make it invalid
      assertEquals(2, result.getWarnings().size()); // Should warn about missing OVERVIEW and REAR
    }

    @Test
    @DisplayName("Should handle null rules list")
    void shouldHandleNullRulesList() {
      ValidationResult result = ruleEngine.validateRules(null);
      assertFalse(result.isValid());
      assertEquals(1, result.getErrors().size());
      assertEquals(
          ValidationErrorType.INVALID_RULE_CONFIGURATION, result.getErrors().get(0).getType());
    }
  }

  @Nested
  @DisplayName("Batch Classification Tests")
  class BatchClassificationTests {

    @Test
    @DisplayName("Should classify multiple filenames correctly")
    void shouldClassifyMultipleFilenames() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 1),
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 3));

      List<String> filenames =
          Arrays.asList(
              "vehicle_001_front.jpg",
              "vehicle_001_rear.jpg",
              "vehicle_001_overview.jpg",
              "vehicle_002_front.jpg",
              "vehicle_002_side.jpg" // Should not match any rule
              );

      Map<ImageRole, List<String>> results = ruleEngine.classifyFilenames(filenames, rules);

      assertEquals(2, results.get(ImageRole.FRONT).size());
      assertEquals(1, results.get(ImageRole.REAR).size());
      assertEquals(1, results.get(ImageRole.OVERVIEW).size());

      assertTrue(results.get(ImageRole.FRONT).contains("vehicle_001_front.jpg"));
      assertTrue(results.get(ImageRole.FRONT).contains("vehicle_002_front.jpg"));
      assertTrue(results.get(ImageRole.REAR).contains("vehicle_001_rear.jpg"));
      assertTrue(results.get(ImageRole.OVERVIEW).contains("vehicle_001_overview.jpg"));
    }

    @Test
    @DisplayName("Should handle empty filename list")
    void shouldHandleEmptyFilenameList() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      Map<ImageRole, List<String>> results = ruleEngine.classifyFilenames(new ArrayList<>(), rules);

      for (ImageRole role : ImageRole.values()) {
        assertTrue(results.get(role).isEmpty());
      }
    }

    @Test
    @DisplayName("Should skip null and empty filenames")
    void shouldSkipNullAndEmptyFilenames() {
      List<RoleRule> rules =
          Arrays.asList(new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1));

      List<String> filenames =
          Arrays.asList("vehicle_front.jpg", null, "", "   ", "vehicle_front2.jpg");

      Map<ImageRole, List<String>> results = ruleEngine.classifyFilenames(filenames, rules);

      assertEquals(2, results.get(ImageRole.FRONT).size());
      assertTrue(results.get(ImageRole.FRONT).contains("vehicle_front.jpg"));
      assertTrue(results.get(ImageRole.FRONT).contains("vehicle_front2.jpg"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Complex Scenarios")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle special regex characters in rule values")
    void shouldHandleSpecialRegexCharacters() {
      List<RoleRule> rules =
          Arrays.asList(
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front[1]", false, 1),
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear(2)", false, 2));

      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_front[1]_001.jpg", rules));
      assertEquals(ImageRole.REAR, ruleEngine.classifyFilename("vehicle_rear(2)_001.jpg", rules));

      // Should not match without the special characters
      assertNull(ruleEngine.classifyFilename("vehicle_front1_001.jpg", rules));
      assertNull(ruleEngine.classifyFilename("vehicle_rear2_001.jpg", rules));
    }

    @Test
    @DisplayName("Should handle complex precedence scenarios")
    void shouldHandleComplexPrecedenceScenarios() {
      List<RoleRule> rules =
          Arrays.asList(
              // Overview rules that might conflict with others
              new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "scene", false, 1),
              new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "full", false, 2),

              // Front rules that might match overview terms
              new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
              new RoleRule(
                  ImageRole.FRONT,
                  RuleType.CONTAINS,
                  "scene_front",
                  false,
                  2), // Would match "scene" too

              // Rear rules
              new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 1));

      // Overview should take precedence
      assertEquals(ImageRole.OVERVIEW, ruleEngine.classifyFilename("vehicle_scene_001.jpg", rules));
      assertEquals(ImageRole.OVERVIEW, ruleEngine.classifyFilename("vehicle_full_view.jpg", rules));

      // Front should only match when overview doesn't
      assertEquals(ImageRole.FRONT, ruleEngine.classifyFilename("vehicle_front_001.jpg", rules));

      // This should match overview first, not front, even though it contains "scene_front"
      assertEquals(
          ImageRole.OVERVIEW, ruleEngine.classifyFilename("vehicle_scene_front_001.jpg", rules));
    }
  }
}
