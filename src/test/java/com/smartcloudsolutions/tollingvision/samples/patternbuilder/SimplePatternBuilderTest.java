package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SimplePatternBuilder visual interface. */
class SimplePatternBuilderTest {

  private SimplePatternBuilder patternBuilder;

  @BeforeAll
  static void initJavaFX() {
    // Initialize JavaFX toolkit
    new JFXPanel();
  }

  @BeforeEach
  void setUp() {
    Platform.runLater(
        () -> {
          patternBuilder =
              new SimplePatternBuilder(
                  "/test/folder", java.util.ResourceBundle.getBundle("messages"));
        });

    // Wait for JavaFX initialization
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void testInitialization() {
    Platform.runLater(
        () -> {
          assertNotNull(patternBuilder);
          assertNotNull(patternBuilder.getSampleFilenames());
          assertNotNull(patternBuilder.getDetectedTokens());
          assertNotNull(patternBuilder.selectedGroupIdProperty());
          assertNotNull(patternBuilder.getRoleRules());

          // Initial state should be empty
          assertTrue(patternBuilder.getSampleFilenames().isEmpty());
          assertTrue(patternBuilder.getDetectedTokens().isEmpty());
          assertNull(patternBuilder.selectedGroupIdProperty().get());
          assertTrue(patternBuilder.getRoleRules().isEmpty());
        });
  }

  @Test
  void testGenerateConfigurationWithoutData() {
    Platform.runLater(
        () -> {
          PatternConfiguration config = patternBuilder.generateConfiguration();

          assertNotNull(config);
          assertTrue(config.getTokens().isEmpty());
          assertNull(config.getGroupIdToken());
          assertTrue(config.getRoleRules().isEmpty());
          assertTrue(config.getGroupPattern().isEmpty());
          assertTrue(config.getFrontPattern().isEmpty());
          assertTrue(config.getRearPattern().isEmpty());
          assertTrue(config.getOverviewPattern().isEmpty());
        });
  }

  @Test
  void testGenerateConfigurationWithData() {
    Platform.runLater(
        () -> {
          // Set up test data
          FilenameToken token1 = new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.9);
          FilenameToken token2 = new FilenameToken("001", 1, TokenType.GROUP_ID, 0.8);
          FilenameToken token3 = new FilenameToken("front", 2, TokenType.CAMERA_SIDE, 0.7);

          patternBuilder.getDetectedTokens().addAll(List.of(token1, token2, token3));
          patternBuilder.selectedGroupIdProperty().set(TokenType.GROUP_ID);

          RoleRule frontRule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2);
          patternBuilder.getRoleRules().add(frontRule);

          // Generate configuration
          PatternConfiguration config = patternBuilder.generateConfiguration();

          assertNotNull(config);
          assertEquals(3, config.getTokens().size());
          assertEquals(TokenType.GROUP_ID, config.getGroupIdToken());
          assertEquals(1, config.getRoleRules().size());
          assertEquals(frontRule, config.getRoleRules().get(0));

          // Should have generated patterns
          assertFalse(config.getGroupPattern().isEmpty());
          assertFalse(config.getFrontPattern().isEmpty());
        });
  }

  @Test
  void testIsImageFile() {
    Platform.runLater(
        () -> {
          // Use reflection to access private method for testing
          try {
            java.lang.reflect.Method isImageFileMethod =
                SimplePatternBuilder.class.getDeclaredMethod("isImageFile", String.class);
            isImageFileMethod.setAccessible(true);

            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.jpg"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.jpeg"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.png"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.bmp"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.tiff"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "test.gif"));
            assertTrue((Boolean) isImageFileMethod.invoke(patternBuilder, "TEST.JPG"));

            assertFalse((Boolean) isImageFileMethod.invoke(patternBuilder, "test.txt"));
            assertFalse((Boolean) isImageFileMethod.invoke(patternBuilder, "test.doc"));
            assertFalse((Boolean) isImageFileMethod.invoke(patternBuilder, "test"));

          } catch (Exception e) {
            fail("Failed to test isImageFile method: " + e.getMessage());
          }
        });
  }

  @Test
  void testDataBindings() {
    Platform.runLater(
        () -> {
          // Test that detected tokens are properly bound
          FilenameToken token = new FilenameToken("test", 0, TokenType.GROUP_ID, 0.8);
          patternBuilder.getDetectedTokens().add(token);

          // Verify the token was added
          assertEquals(1, patternBuilder.getDetectedTokens().size());
          assertEquals(token, patternBuilder.getDetectedTokens().get(0));

          // Test role rules binding
          RoleRule rule = new RoleRule(ImageRole.FRONT, RuleType.CONTAINS, "front", false, 2);
          patternBuilder.getRoleRules().add(rule);

          assertEquals(1, patternBuilder.getRoleRules().size());
          assertEquals(rule, patternBuilder.getRoleRules().get(0));
        });
  }
}
