package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for GroupingEngine functionality. */
class GroupingEngineTest {

  private GroupingEngine groupingEngine;
  private UnknownSegmentHandler unknownSegmentHandler;

  @BeforeEach
  void setUp() {
    groupingEngine = new GroupingEngine();
    unknownSegmentHandler = new UnknownSegmentHandler();
  }

  @Test
  void testBasicGrouping() {
    List<String> filenames =
        List.of("vehicle_ABC123_front.jpg", "vehicle_ABC123_rear.jpg", "vehicle_XYZ789_front.jpg");

    String groupPattern = "^vehicle_([A-Z0-9]+)_\\w+\\.jpg$";

    List<RoleRule> roleRules =
        List.of(
            new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
            new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

    GroupingEngine.GroupingResult result =
        groupingEngine.groupAndAssignRoles(
            filenames, groupPattern, roleRules, unknownSegmentHandler);

    assertEquals(2, result.getGroupCount());
    assertEquals(3, result.getMatchedFiles());
    assertEquals(0, result.getUnmatchedFiles().size());

    assertTrue(result.getGroups().containsKey("ABC123"));
    assertTrue(result.getGroups().containsKey("XYZ789"));

    assertEquals(ImageRole.FRONT, result.getFileToRole().get("vehicle_ABC123_front.jpg"));
    assertEquals(ImageRole.REAR, result.getFileToRole().get("vehicle_ABC123_rear.jpg"));
  }

  @Test
  void testComplexFilenameWithUnknownSegment() {
    List<String> filenames =
        List.of("52_08354353_15864_lpr_rear.jpg", "52_08354353_15864_lpr_front.jpg");

    // Group pattern should extract 15864 as group ID
    String groupPattern = "^\\d+_\\d+_([\\d]+)_\\w+_\\w+\\.jpg$";

    List<RoleRule> roleRules =
        List.of(
            new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
            new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

    GroupingEngine.GroupingResult result =
        groupingEngine.groupAndAssignRoles(
            filenames, groupPattern, roleRules, unknownSegmentHandler);

    assertEquals(1, result.getGroupCount());
    assertEquals(2, result.getMatchedFiles());
    assertTrue(result.getGroups().containsKey("15864"));

    assertEquals(ImageRole.FRONT, result.getFileToRole().get("52_08354353_15864_lpr_front.jpg"));
    assertEquals(ImageRole.REAR, result.getFileToRole().get("52_08354353_15864_lpr_rear.jpg"));
  }

  @Test
  void testOverviewPrecedence() {
    List<String> filenames =
        List.of(
            "vehicle_ABC123_overview.jpg", "vehicle_ABC123_front.jpg", "vehicle_ABC123_rear.jpg");

    String groupPattern = "^vehicle_([A-Z0-9]+)_\\w+\\.jpg$";

    List<RoleRule> roleRules =
        List.of(
            new RoleRule(
                ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 0), // Highest priority
            new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1),
            new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2));

    GroupingEngine.GroupingResult result =
        groupingEngine.groupAndAssignRoles(
            filenames, groupPattern, roleRules, unknownSegmentHandler);

    assertEquals(1, result.getGroupCount());
    assertEquals(3, result.getMatchedFiles());

    assertEquals(ImageRole.OVERVIEW, result.getFileToRole().get("vehicle_ABC123_overview.jpg"));
    assertEquals(ImageRole.FRONT, result.getFileToRole().get("vehicle_ABC123_front.jpg"));
    assertEquals(ImageRole.REAR, result.getFileToRole().get("vehicle_ABC123_rear.jpg"));
  }

  @Test
  void testInvalidGroupPattern() {
    List<String> filenames = List.of("test.jpg");
    String invalidPattern = "^[unclosed";

    GroupingEngine.GroupingResult result =
        groupingEngine.groupAndAssignRoles(
            filenames, invalidPattern, List.of(), unknownSegmentHandler);

    assertEquals(0, result.getGroupCount());
    assertEquals(0, result.getMatchedFiles());
    assertEquals(1, result.getUnmatchedFiles().size());
    assertTrue(result.getUnmatchedReasons().get("test.jpg").contains("Invalid group pattern"));
  }

  @Test
  void testNoMatchingFiles() {
    List<String> filenames = List.of("nomatch.jpg");
    String groupPattern = "^vehicle_([A-Z0-9]+)_\\w+\\.jpg$";

    GroupingEngine.GroupingResult result =
        groupingEngine.groupAndAssignRoles(
            filenames, groupPattern, List.of(), unknownSegmentHandler);

    assertEquals(0, result.getGroupCount());
    assertEquals(0, result.getMatchedFiles());
    assertEquals(1, result.getUnmatchedFiles().size());
    assertEquals(
        "Filename doesn't match group pattern", result.getUnmatchedReasons().get("nomatch.jpg"));
  }

  @Test
  void testValidateGroupPattern() {
    // Valid pattern with one capturing group
    ValidationResult result =
        groupingEngine.validateGroupPattern("^vehicle_([A-Z0-9]+)_\\w+\\.jpg$");
    assertTrue(result.isValid());

    // Invalid pattern - no capturing groups
    result = groupingEngine.validateGroupPattern("^vehicle_[A-Z0-9]+_\\w+\\.jpg$");
    assertFalse(result.isValid());
    assertEquals(ValidationErrorType.NO_CAPTURING_GROUPS, result.getErrors().get(0).getType());

    // Invalid pattern - multiple capturing groups
    result = groupingEngine.validateGroupPattern("^(vehicle)_([A-Z0-9]+)_\\w+\\.jpg$");
    assertFalse(result.isValid());
    assertEquals(
        ValidationErrorType.MULTIPLE_CAPTURING_GROUPS, result.getErrors().get(0).getType());

    // Invalid regex syntax
    result = groupingEngine.validateGroupPattern("^[unclosed");
    assertFalse(result.isValid());
    assertEquals(ValidationErrorType.REGEX_SYNTAX_ERROR, result.getErrors().get(0).getType());

    // Empty pattern
    result = groupingEngine.validateGroupPattern("");
    assertFalse(result.isValid());
    assertEquals(ValidationErrorType.EMPTY_GROUP_PATTERN, result.getErrors().get(0).getType());
  }
}
