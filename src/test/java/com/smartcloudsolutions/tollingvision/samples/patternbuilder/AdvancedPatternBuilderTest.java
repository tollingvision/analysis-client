package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for AdvancedPatternBuilder functionality. */
class AdvancedPatternBuilderTest {

  private AdvancedPatternBuilder advancedBuilder;

  @BeforeAll
  static void initToolkit() {
    // Initialize JavaFX toolkit
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // Platform already initialized
    }
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    // Ensure we're on the JavaFX Application Thread
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Create new builder for each test
          advancedBuilder =
              new AdvancedPatternBuilder(
                  "/test/folder", java.util.ResourceBundle.getBundle("messages"));
          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testInitialState() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Test initial configuration
          PatternConfiguration config = advancedBuilder.getConfiguration();
          assertNotNull(config);
          assertEquals("", config.getGroupPattern());
          assertEquals("", config.getFrontPattern());
          assertEquals("", config.getRearPattern());
          assertEquals("", config.getOverviewPattern());

          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testSetConfiguration() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Create test configuration
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-]+)_.*$");
          config.setFrontPattern(".*front.*");
          config.setRearPattern(".*rear.*");
          config.setOverviewPattern(".*overview.*");

          // Set configuration
          advancedBuilder.setConfiguration(config);

          // Verify configuration was set
          PatternConfiguration retrievedConfig = advancedBuilder.getConfiguration();
          assertEquals("^vehicle_([\\w\\-]+)_.*$", retrievedConfig.getGroupPattern());
          assertEquals(".*front.*", retrievedConfig.getFrontPattern());
          assertEquals(".*rear.*", retrievedConfig.getRearPattern());
          assertEquals(".*overview.*", retrievedConfig.getOverviewPattern());

          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testValidPatternValidation() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set valid patterns
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-]+)_.*$"); // Valid with one capturing group
          config.setFrontPattern(".*front.*");
          config.setRearPattern(".*rear.*");

          advancedBuilder.setConfiguration(config);

          // Wait a bit for validation to complete
          Platform.runLater(
              () -> {
                ValidationResult result = advancedBuilder.validateConfiguration();

                // Debug: print validation errors
                if (!result.isValid()) {
                  System.out.println("Validation failed with errors:");
                  for (ValidationError error : result.getErrors()) {
                    System.out.println("  - " + error.getType() + ": " + error.getMessage());
                  }
                }

                assertTrue(result.isValid(), "Valid patterns should pass validation");
                assertFalse(result.hasErrors(), "Valid patterns should have no errors");

                latch.countDown();
              });
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testInvalidPatternValidation() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set invalid patterns
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_[\\w\\-]+_.*$"); // Invalid - no capturing group
          config.setFrontPattern(".*front.*");

          advancedBuilder.setConfiguration(config);

          // Wait a bit for validation to complete
          Platform.runLater(
              () -> {
                ValidationResult result = advancedBuilder.validateConfiguration();
                assertFalse(result.isValid(), "Invalid patterns should fail validation");
                assertTrue(result.hasErrors(), "Invalid patterns should have errors");

                // Check for specific error type
                boolean hasCapturingGroupError =
                    result.getErrors().stream()
                        .anyMatch(
                            error -> error.getType() == ValidationErrorType.NO_CAPTURING_GROUPS);
                assertTrue(hasCapturingGroupError, "Should have capturing group error");

                latch.countDown();
              });
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testRegexSyntaxError() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set pattern with syntax error
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-+_.*$"); // Invalid regex - unmatched bracket
          config.setFrontPattern(".*front.*");

          advancedBuilder.setConfiguration(config);

          // Wait a bit for validation to complete
          Platform.runLater(
              () -> {
                ValidationResult result = advancedBuilder.validateConfiguration();
                assertFalse(result.isValid(), "Patterns with syntax errors should fail validation");
                assertTrue(result.hasErrors(), "Patterns with syntax errors should have errors");

                // Check for regex syntax error
                boolean hasSyntaxError =
                    result.getErrors().stream()
                        .anyMatch(
                            error -> error.getType() == ValidationErrorType.REGEX_SYNTAX_ERROR);
                assertTrue(hasSyntaxError, "Should have regex syntax error");

                latch.countDown();
              });
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testSampleFilenamesIntegration() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set sample filenames
          List<String> sampleFilenames =
              Arrays.asList(
                  "vehicle_001_front.jpg",
                  "vehicle_001_rear.jpg",
                  "vehicle_001_overview.jpg",
                  "vehicle_002_front.jpg",
                  "vehicle_002_rear.jpg");

          advancedBuilder.setSampleFilenames(sampleFilenames);

          // Set valid configuration
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-]+)_.*$");
          config.setFrontPattern(".*front.*");
          config.setRearPattern(".*rear.*");
          config.setOverviewPattern(".*overview.*");

          advancedBuilder.setConfiguration(config);

          // Verify preview pane has data
          assertNotNull(advancedBuilder.getPreviewPane());

          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testClearPatterns() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set some patterns first
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-]+)_.*$");
          config.setFrontPattern(".*front.*");
          advancedBuilder.setConfiguration(config);

          // Clear patterns
          advancedBuilder.clearPatterns();

          // Verify patterns are cleared
          PatternConfiguration clearedConfig = advancedBuilder.getConfiguration();
          assertEquals("", clearedConfig.getGroupPattern());
          assertEquals("", clearedConfig.getFrontPattern());
          assertEquals("", clearedConfig.getRearPattern());
          assertEquals("", clearedConfig.getOverviewPattern());

          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testConfigurationProperty() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Test configuration property binding
          assertNotNull(advancedBuilder.configurationProperty());

          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^test_([\\w]+)_.*$");

          advancedBuilder.configurationProperty().set(config);

          // Verify property was set
          assertEquals(config, advancedBuilder.configurationProperty().get());

          latch.countDown();
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testMultipleCapturingGroupsError() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set pattern with multiple capturing groups
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("^vehicle_([\\w\\-]+)_([\\w]+)_.*$"); // Two capturing groups
          config.setFrontPattern(".*front.*");

          advancedBuilder.setConfiguration(config);

          // Wait a bit for validation to complete
          Platform.runLater(
              () -> {
                ValidationResult result = advancedBuilder.validateConfiguration();
                assertFalse(result.isValid(), "Multiple capturing groups should fail validation");

                // Check for specific error type
                boolean hasMultipleGroupsError =
                    result.getErrors().stream()
                        .anyMatch(
                            error ->
                                error.getType() == ValidationErrorType.MULTIPLE_CAPTURING_GROUPS);
                assertTrue(hasMultipleGroupsError, "Should have multiple capturing groups error");

                latch.countDown();
              });
        });
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  void testEmptyPatternsValidation() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Set empty configuration
          PatternConfiguration config = new PatternConfiguration();
          // All patterns are empty by default

          advancedBuilder.setConfiguration(config);

          // Wait a bit for validation to complete
          Platform.runLater(
              () -> {
                ValidationResult result = advancedBuilder.validateConfiguration();
                assertFalse(result.isValid(), "Empty patterns should fail validation");
                assertTrue(result.hasErrors(), "Empty patterns should have errors");

                latch.countDown();
              });
        });
    latch.await(5, TimeUnit.SECONDS);
  }
}
