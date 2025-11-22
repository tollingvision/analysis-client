package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for PatternPreviewPane functionality. */
class PatternPreviewPaneTest {

  private PatternPreviewPane previewPane;

  @BeforeAll
  static void initJavaFX() {
    // Initialize JavaFX toolkit
    new JFXPanel();
  }

  @BeforeEach
  void setUp() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          previewPane = new PatternPreviewPane(java.util.ResourceBundle.getBundle("messages"));
          latch.countDown();
        });
    assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX initialization timed out");
  }

  @Test
  void testInitialState() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertNotNull(previewPane);
          assertNull(previewPane.getPreviewSummary());
          assertTrue(previewPane.getPreviewData().isEmpty());
          latch.countDown();
        });
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  @Test
  void testUpdatePreviewWithEmptyData() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          PatternConfiguration config = new PatternConfiguration();
          List<String> filenames = Arrays.asList();

          previewPane.updatePreview(config, filenames);

          // Should handle empty data gracefully
          assertTrue(previewPane.getPreviewData().isEmpty());
          latch.countDown();
        });
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  @Test
  void testUpdatePreviewWithValidConfiguration() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // Create a valid configuration
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("vehicle_(\\d+)_.*");

          RoleRule frontRule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 1);
          RoleRule rearRule = new RoleRule(ImageRole.REAR, RuleType.CONTAINS, "rear", false, 2);
          config.addRoleRule(frontRule);
          config.addRoleRule(rearRule);

          List<String> filenames =
              Arrays.asList(
                  "vehicle_001_front.jpg",
                  "vehicle_001_rear.jpg",
                  "vehicle_002_front.jpg",
                  "vehicle_002_rear.jpg");

          previewPane.updatePreview(config, filenames);

          // Give some time for background processing
          Platform.runLater(
              () -> {
                try {
                  Thread.sleep(100); // Brief pause for processing
                  latch.countDown();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  latch.countDown();
                }
              });
        });
    assertTrue(latch.await(10, TimeUnit.SECONDS));
  }

  @Test
  void testClearPreview() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          // First add some data
          PatternConfiguration config = new PatternConfiguration();
          config.setGroupPattern("test_(\\d+).*");
          List<String> filenames = Arrays.asList("test_001.jpg", "test_002.jpg");

          previewPane.updatePreview(config, filenames);

          // Then clear it
          previewPane.clearPreview();

          assertTrue(previewPane.getPreviewData().isEmpty());
          assertNull(previewPane.getPreviewSummary());
          latch.countDown();
        });
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }
}
