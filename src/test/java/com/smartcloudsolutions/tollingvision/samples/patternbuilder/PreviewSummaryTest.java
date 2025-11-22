package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Test class for PreviewSummary functionality. */
class PreviewSummaryTest {

  private ResourceBundle getMessages() {
    return ResourceBundle.getBundle("messages");
  }

  @Test
  void testEmptyPreviewList() {
    PreviewSummary summary = new PreviewSummary(Arrays.asList(), getMessages());

    assertEquals(0, summary.getTotalFiles());
    assertEquals(0, summary.getMatchedFiles());
    assertEquals(0, summary.getUnmatchedFiles());
    assertEquals(0.0, summary.getMatchPercentage());
    assertEquals(0, summary.getGroupCount());
    assertEquals(0, summary.getCompleteGroupCount());
    assertFalse(summary.hasErrors());
    assertFalse(summary.hasWarnings());
    assertTrue(summary.isHealthy());
  }

  @Test
  void testNullPreviewList() {
    PreviewSummary summary = new PreviewSummary(null, getMessages());

    assertEquals(0, summary.getTotalFiles());
    assertEquals(0, summary.getMatchedFiles());
    assertEquals(0, summary.getUnmatchedFiles());
    assertEquals(0.0, summary.getMatchPercentage());
  }

  @Test
  void testSuccessfulPreviews() {
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createSuccessfulPreview("vehicle_002_front.jpg", "002", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_002_rear.jpg", "002", ImageRole.REAR));

    PreviewSummary summary = new PreviewSummary(previews, getMessages());

    assertEquals(4, summary.getTotalFiles());
    assertEquals(4, summary.getMatchedFiles());
    assertEquals(0, summary.getUnmatchedFiles());
    assertEquals(100.0, summary.getMatchPercentage());
    assertEquals(2, summary.getRoleCount(ImageRole.FRONT));
    assertEquals(2, summary.getRoleCount(ImageRole.REAR));
    assertEquals(0, summary.getRoleCount(ImageRole.OVERVIEW));
    assertEquals(2, summary.getGroupCount());
    assertEquals(2, summary.getCompleteGroupCount());
    assertFalse(summary.hasErrors());
    assertFalse(summary.hasWarnings());
    assertTrue(summary.isHealthy());
  }

  @Test
  void testMixedPreviews() {
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createUnmatchedPreview("unknown_file.jpg"),
            createErrorPreview("bad_file.jpg", "Invalid pattern"));

    PreviewSummary summary = new PreviewSummary(previews, getMessages());

    assertEquals(4, summary.getTotalFiles());
    assertEquals(2, summary.getMatchedFiles());
    assertEquals(2, summary.getUnmatchedFiles());
    assertEquals(50.0, summary.getMatchPercentage());
    assertEquals(1, summary.getRoleCount(ImageRole.FRONT));
    assertEquals(1, summary.getRoleCount(ImageRole.REAR));
    assertEquals(1, summary.getGroupCount());
    assertEquals(1, summary.getCompleteGroupCount());
    assertTrue(summary.hasErrors());
    assertTrue(summary.hasWarnings());
    assertFalse(summary.isHealthy());

    List<String> unmatchedFiles = summary.getUnmatchedFilenames();
    assertEquals(2, unmatchedFiles.size());
    assertTrue(unmatchedFiles.contains("unknown_file.jpg"));
    assertTrue(unmatchedFiles.contains("bad_file.jpg"));

    List<String> errorMessages = summary.getErrorMessages();
    assertEquals(1, errorMessages.size());
    assertTrue(errorMessages.get(0).contains("bad_file.jpg"));
    assertTrue(errorMessages.get(0).contains("Invalid pattern"));
  }

  @Test
  void testIncompleteGroups() {
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createSuccessfulPreview("vehicle_002_front.jpg", "002", ImageRole.FRONT),
            // Missing rear for group 002
            createSuccessfulPreview("vehicle_003_overview.jpg", "003", ImageRole.OVERVIEW)
            // Missing front and rear for group 003
            );

    PreviewSummary summary = new PreviewSummary(previews, getMessages());

    assertEquals(3, summary.getGroupCount());
    assertEquals(1, summary.getCompleteGroupCount()); // Only group 001 is complete

    List<String> incompleteGroups = summary.getIncompleteGroups();
    assertEquals(2, incompleteGroups.size());
    assertTrue(incompleteGroups.contains("002"));
    assertTrue(incompleteGroups.contains("003"));

    assertTrue(summary.hasWarnings());
    assertFalse(summary.isHealthy());
  }

  @Test
  void testGroupRoles() {
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createSuccessfulPreview("vehicle_001_overview.jpg", "001", ImageRole.OVERVIEW),
            createSuccessfulPreview("vehicle_002_front.jpg", "002", ImageRole.FRONT));

    PreviewSummary summary = new PreviewSummary(previews, getMessages());

    Map<String, Set<ImageRole>> groupRoles = summary.getGroupRoles();
    assertEquals(2, groupRoles.size());

    Set<ImageRole> group001Roles = groupRoles.get("001");
    assertEquals(3, group001Roles.size());
    assertTrue(group001Roles.contains(ImageRole.FRONT));
    assertTrue(group001Roles.contains(ImageRole.REAR));
    assertTrue(group001Roles.contains(ImageRole.OVERVIEW));

    Set<ImageRole> group002Roles = groupRoles.get("002");
    assertEquals(1, group002Roles.size());
    assertTrue(group002Roles.contains(ImageRole.FRONT));
  }

  @Test
  void testSummaryText() {
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createUnmatchedPreview("unknown.jpg"));

    PreviewSummary summary = new PreviewSummary(previews, getMessages());
    String summaryText = summary.getSummaryText();

    assertTrue(summaryText.contains("3 files"));
    assertTrue(summaryText.contains("2 matched"));
    assertTrue(summaryText.contains("66.7%"));
    assertTrue(summaryText.contains("1 unmatched"));
    assertTrue(summaryText.contains("FRONT: 1"));
    assertTrue(summaryText.contains("REAR: 1"));
  }

  @Test
  void testHealthyConfiguration() {
    // Create a healthy configuration with 90% match rate
    List<FilenamePreview> previews =
        Arrays.asList(
            createSuccessfulPreview("vehicle_001_front.jpg", "001", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_001_rear.jpg", "001", ImageRole.REAR),
            createSuccessfulPreview("vehicle_002_front.jpg", "002", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_002_rear.jpg", "002", ImageRole.REAR),
            createSuccessfulPreview("vehicle_003_front.jpg", "003", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_003_rear.jpg", "003", ImageRole.REAR),
            createSuccessfulPreview("vehicle_004_front.jpg", "004", ImageRole.FRONT),
            createSuccessfulPreview("vehicle_004_rear.jpg", "004", ImageRole.REAR),
            createSuccessfulPreview("vehicle_005_front.jpg", "005", ImageRole.FRONT),
            createUnmatchedPreview("unknown.jpg") // 10% unmatched
            );

    PreviewSummary summary = new PreviewSummary(previews, getMessages());

    assertEquals(90.0, summary.getMatchPercentage());
    assertFalse(summary.hasErrors());
    assertTrue(summary.hasWarnings()); // Due to incomplete group 005
    assertFalse(summary.isHealthy()); // Due to incomplete groups
  }

  private FilenamePreview createSuccessfulPreview(String filename, String groupId, ImageRole role) {
    return new FilenamePreview(filename, groupId, role, true, null);
  }

  private FilenamePreview createUnmatchedPreview(String filename) {
    return new FilenamePreview(filename, null, null, false, null);
  }

  private FilenamePreview createErrorPreview(String filename, String errorMessage) {
    return new FilenamePreview(filename, null, null, false, errorMessage);
  }
}
