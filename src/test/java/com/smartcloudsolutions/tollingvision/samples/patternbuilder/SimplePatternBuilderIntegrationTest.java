package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for SimplePatternBuilder with all sub-components. */
class SimplePatternBuilderIntegrationTest {

  private SimplePatternBuilder patternBuilder;
  private Path tempDir;

  @BeforeAll
  static void initJavaFX() {
    // Initialize JavaFX toolkit
    try {
      new JFXPanel();
    } catch (UnsupportedOperationException e) {
      // Headless environment - skip JavaFX initialization
      org.junit.jupiter.api.Assumptions.assumeFalse(
          Boolean.getBoolean("java.awt.headless"), "Skipping JavaFX tests in headless environment");
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    // Skip in headless CI
    org.junit.jupiter.api.Assumptions.assumeFalse(
        Boolean.getBoolean("java.awt.headless"), "Skipping JavaFX tests in headless environment");
    // Create temporary directory with sample files
    tempDir = Files.createTempDirectory("pattern-builder-test");
    createSampleFiles();

    Platform.runLater(
        () -> {
          patternBuilder = new SimplePatternBuilder(
              tempDir.toString(), java.util.ResourceBundle.getBundle("messages"));
        });

    // Wait for JavaFX initialization
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void createSampleFiles() throws IOException {
    // Create sample image files with different patterns
    List<String> sampleFiles = List.of(
        "vehicle_001_front.jpg",
        "vehicle_001_rear.jpg",
        "vehicle_001_overview.jpg",
        "vehicle_002_front.jpg",
        "vehicle_002_rear.jpg",
        "vehicle_002_overview.jpg",
        "car_ABC123_f.png",
        "car_ABC123_r.png",
        "car_ABC123_ov.png");

    for (String filename : sampleFiles) {
      Files.createFile(tempDir.resolve(filename));
    }
  }

  @Test
  void testCompleteWorkflow() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(
        () -> {
          try {
            // Step 1: Analyze sample files
            patternBuilder.analyzeSampleFiles(tempDir);

            // Wait a bit for analysis to complete
            Thread.sleep(500);

            // Verify files were loaded
            assertFalse(patternBuilder.getSampleFilenames().isEmpty());
            assertTrue(patternBuilder.getSampleFilenames().size() >= 9);

            // Verify tokens were detected
            assertFalse(patternBuilder.getDetectedTokens().isEmpty());

            // Step 2: Select group ID (simulate user selection)
            FilenameToken groupIdToken = patternBuilder.getDetectedTokens().stream()
                .filter(
                    token -> token.getSuggestedType() == TokenType.GROUP_ID
                        || token.getValue().matches("\\d+|[A-Z0-9]+"))
                .findFirst()
                .orElse(patternBuilder.getDetectedTokens().get(1)); // Fallback to second token

            patternBuilder.selectedGroupIdProperty().set(groupIdToken.getSuggestedType());
            assertNotNull(patternBuilder.selectedGroupIdProperty().get());

            // Step 3: Add role rules
            RoleRule frontRule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2);
            RoleRule rearRule = new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 3);
            RoleRule overviewRule = new RoleRule(ImageRole.OVERVIEW, RuleType.CONTAINS, "overview", false, 1);

            patternBuilder.getRoleRules().addAll(List.of(frontRule, rearRule, overviewRule));

            // Step 4: Generate configuration
            PatternConfiguration config = patternBuilder.generateConfiguration();

            // Verify configuration
            assertNotNull(config);
            assertFalse(config.getTokens().isEmpty());
            assertNotNull(config.getGroupIdToken());
            assertEquals(3, config.getRoleRules().size());

            // Verify patterns were generated
            assertFalse(config.getGroupPattern().isEmpty());
            assertFalse(config.getFrontPattern().isEmpty());
            assertFalse(config.getRearPattern().isEmpty());
            assertFalse(config.getOverviewPattern().isEmpty());

            // Verify group pattern contains capturing group
            assertTrue(config.getGroupPattern().contains("("));
            assertTrue(config.getGroupPattern().contains(")"));

            latch.countDown();

          } catch (Exception e) {
            fail("Integration test failed: " + e.getMessage());
            latch.countDown();
          }
        });

    assertTrue(latch.await(10, TimeUnit.SECONDS), "Test did not complete within timeout");
  }

  @Test
  void testFileAnalysisPane() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(
        () -> {
          try {
            // Test file analysis directly
            patternBuilder.analyzeSampleFiles(tempDir);

            // Wait for analysis
            Thread.sleep(500);

            // Verify sample filenames were loaded
            assertFalse(patternBuilder.getSampleFilenames().isEmpty());

            // Verify all files are image files
            for (String filename : patternBuilder.getSampleFilenames()) {
              assertTrue(filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|bmp|tiff|gif)$"));
            }

            latch.countDown();

          } catch (Exception e) {
            fail("File analysis test failed: " + e.getMessage());
            latch.countDown();
          }
        });

    assertTrue(
        latch.await(5, TimeUnit.SECONDS), "File analysis test did not complete within timeout");
  }

  @Test
  void testTokenDetection() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(
        () -> {
          try {
            // Analyze files to get tokens
            patternBuilder.analyzeSampleFiles(tempDir);
            Thread.sleep(500);

            // Verify tokens were detected
            assertFalse(patternBuilder.getDetectedTokens().isEmpty());

            // Check for expected token types
            boolean hasGroupId = patternBuilder.getDetectedTokens().stream()
                .anyMatch(token -> token.getSuggestedType() == TokenType.GROUP_ID);
            boolean hasCameraSide = patternBuilder.getDetectedTokens().stream()
                .anyMatch(token -> token.getSuggestedType() == TokenType.CAMERA_SIDE);
            boolean hasExtension = patternBuilder.getDetectedTokens().stream()
                .anyMatch(token -> token.getSuggestedType() == TokenType.EXTENSION);

            // At least one of these should be detected
            assertTrue(
                hasGroupId || hasCameraSide || hasExtension,
                "Expected to detect at least one common token type");

            latch.countDown();

          } catch (Exception e) {
            fail("Token detection test failed: " + e.getMessage());
            latch.countDown();
          }
        });

    assertTrue(
        latch.await(5, TimeUnit.SECONDS), "Token detection test did not complete within timeout");
  }

  @Test
  void testPatternGeneration() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(
        () -> {
          try {
            // Set up minimal configuration
            FilenameToken token1 = new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9);
            FilenameToken token2 = new FilenameToken("001", 1, TokenType.GROUP_ID, 0.8);
            FilenameToken token3 = new FilenameToken("front", 2, TokenType.CAMERA_SIDE, 0.7);

            patternBuilder.getDetectedTokens().addAll(List.of(token1, token2, token3));
            patternBuilder.selectedGroupIdProperty().set(TokenType.GROUP_ID);

            RoleRule frontRule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2);
            patternBuilder.getRoleRules().add(frontRule);

            // Generate configuration
            PatternConfiguration config = patternBuilder.generateConfiguration();

            // Verify patterns
            assertNotNull(config.getGroupPattern());
            assertFalse(config.getGroupPattern().isEmpty());
            assertTrue(config.getGroupPattern().contains("("));
            assertTrue(config.getGroupPattern().contains(")"));

            assertNotNull(config.getFrontPattern());
            assertFalse(config.getFrontPattern().isEmpty());

            latch.countDown();

          } catch (Exception e) {
            fail("Pattern generation test failed: " + e.getMessage());
            latch.countDown();
          }
        });

    assertTrue(
        latch.await(5, TimeUnit.SECONDS),
        "Pattern generation test did not complete within timeout");
  }
}
