package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests for ExtensionMatcher functionality. */
class ExtensionMatcherTest {

  @Test
  void testHasImageExtension() {
    assertTrue(ExtensionMatcher.hasImageExtension("test.jpg"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.jpeg"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.png"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.tiff"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.tif"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.bmp"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.gif"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.webp"));

    // Case insensitive
    assertTrue(ExtensionMatcher.hasImageExtension("test.JPG"));
    assertTrue(ExtensionMatcher.hasImageExtension("test.PNG"));

    // Non-image extensions
    assertFalse(ExtensionMatcher.hasImageExtension("test.txt"));
    assertFalse(ExtensionMatcher.hasImageExtension("test.pdf"));
    assertFalse(ExtensionMatcher.hasImageExtension("test"));
    assertFalse(ExtensionMatcher.hasImageExtension(""));
    assertFalse(ExtensionMatcher.hasImageExtension(null));
  }

  @Test
  void testGetExtension() {
    assertEquals("jpg", ExtensionMatcher.getExtension("test.jpg"));
    assertEquals("png", ExtensionMatcher.getExtension("test.png"));
    assertEquals("jpg", ExtensionMatcher.getExtension("test.JPG")); // Lowercase
    assertEquals("", ExtensionMatcher.getExtension("test"));
    assertEquals("", ExtensionMatcher.getExtension(""));
    assertEquals("", ExtensionMatcher.getExtension(null));
    assertEquals("", ExtensionMatcher.getExtension("test."));
  }

  @Test
  void testGetUniqueExtensions() {
    List<String> filenames =
        List.of("test1.jpg", "test2.png", "test3.jpg", "test4.tiff", "test5.PNG");

    Set<String> extensions = ExtensionMatcher.getUniqueExtensions(filenames);
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains("jpg"));
    assertTrue(extensions.contains("png"));
    assertTrue(extensions.contains("tiff"));
  }

  @Test
  void testGenerateExtensionPattern() {
    Set<String> extensions = Set.of("jpg", "png");

    // Case sensitive
    String pattern = ExtensionMatcher.generateExtensionPattern(extensions, true);
    assertTrue(pattern.contains("jpg"));
    assertTrue(pattern.contains("png"));
    assertFalse(pattern.contains("(?i:"));

    // Case insensitive
    pattern = ExtensionMatcher.generateExtensionPattern(extensions, false);
    assertTrue(pattern.contains("(?i:"));

    // Empty extensions should return any image pattern
    pattern = ExtensionMatcher.generateExtensionPattern(Set.of(), false);
    assertEquals(ExtensionMatcher.ANY_IMAGE_EXTENSION_PATTERN, pattern);
  }

  @Test
  void testApplyExtensionMatching() {
    String originalPattern = "^test_(.+)_\\w+\\.jpg$";

    // Without extension matching
    String result = ExtensionMatcher.applyExtensionMatching(originalPattern, false);
    assertEquals(originalPattern, result);

    // With extension matching
    result = ExtensionMatcher.applyExtensionMatching(originalPattern, true);
    assertNotEquals(originalPattern, result);
    assertTrue(result.contains("jpg|jpeg|png|tiff"));

    // Null pattern
    assertNull(ExtensionMatcher.applyExtensionMatching(null, true));

    // Empty pattern
    assertEquals("", ExtensionMatcher.applyExtensionMatching("", true));
  }

  @Test
  void testHasFlexibleExtensionMatching() {
    assertTrue(ExtensionMatcher.hasFlexibleExtensionMatching(".*\\.(jpg|jpeg|png|tiff)$"));
    assertTrue(ExtensionMatcher.hasFlexibleExtensionMatching("(?i:.*\\.jpg|png.*)"));
    assertFalse(ExtensionMatcher.hasFlexibleExtensionMatching(".*\\.jpg$"));
    assertFalse(ExtensionMatcher.hasFlexibleExtensionMatching(null));
    assertFalse(ExtensionMatcher.hasFlexibleExtensionMatching(""));
  }

  @Test
  void testValidateExtension() {
    Set<String> specificExtensions = Set.of("jpg", "png");

    // With any extension matching
    assertTrue(ExtensionMatcher.validateExtension("test.jpg", true, null));
    assertTrue(ExtensionMatcher.validateExtension("test.png", true, null));
    assertTrue(ExtensionMatcher.validateExtension("test.tiff", true, null));
    assertFalse(ExtensionMatcher.validateExtension("test.txt", true, null));

    // With specific extensions
    assertTrue(ExtensionMatcher.validateExtension("test.jpg", false, specificExtensions));
    assertTrue(ExtensionMatcher.validateExtension("test.png", false, specificExtensions));
    assertFalse(ExtensionMatcher.validateExtension("test.tiff", false, specificExtensions));

    // Invalid inputs
    assertFalse(ExtensionMatcher.validateExtension(null, true, null));
    assertFalse(ExtensionMatcher.validateExtension("", true, null));
  }

  @Test
  void testGetExtensionMatchingDescription() {
    Set<String> extensions = Set.of("jpg", "png");

    // Any extension
    String description = ExtensionMatcher.getExtensionMatchingDescription(true, null);
    assertTrue(description.contains("any image extension"));

    // Single extension
    description = ExtensionMatcher.getExtensionMatchingDescription(false, Set.of("jpg"));
    assertTrue(description.contains(".jpg files only"));

    // Multiple extensions
    description = ExtensionMatcher.getExtensionMatchingDescription(false, extensions);
    assertTrue(description.contains("Matching extensions"));
    assertTrue(description.contains(".jpg"));
    assertTrue(description.contains(".png"));

    // No extensions
    description = ExtensionMatcher.getExtensionMatchingDescription(false, Set.of());
    assertTrue(description.contains("No extension matching"));
  }

  @Test
  void testAnalyzeExtensionUsage() {
    // Empty list
    ExtensionMatcher.ExtensionMatchingRecommendation recommendation =
        ExtensionMatcher.analyzeExtensionUsage(List.of());
    assertFalse(recommendation.isRecommendFlexibleMatching());
    assertTrue(recommendation.getDetectedExtensions().isEmpty());

    // Single extension
    List<String> singleExtension = List.of("test1.jpg", "test2.jpg", "test3.jpg");
    recommendation = ExtensionMatcher.analyzeExtensionUsage(singleExtension);
    assertFalse(recommendation.isRecommendFlexibleMatching());
    assertEquals(1, recommendation.getDetectedExtensions().size());
    assertTrue(recommendation.getDetectedExtensions().contains("jpg"));

    // Multiple image extensions
    List<String> multipleExtensions = List.of("test1.jpg", "test2.png", "test3.tiff");
    recommendation = ExtensionMatcher.analyzeExtensionUsage(multipleExtensions);
    assertTrue(recommendation.isRecommendFlexibleMatching());
    assertEquals(3, recommendation.getDetectedExtensions().size());

    // Mixed file types
    List<String> mixedTypes = List.of("test1.jpg", "test2.txt", "test3.pdf");
    recommendation = ExtensionMatcher.analyzeExtensionUsage(mixedTypes);
    assertFalse(recommendation.isRecommendFlexibleMatching());
    assertTrue(recommendation.getReasoning().contains("Mixed file types"));

    // No extensions
    List<String> noExtensions = List.of("test1", "test2", "test3");
    recommendation = ExtensionMatcher.analyzeExtensionUsage(noExtensions);
    assertFalse(recommendation.isRecommendFlexibleMatching());
    assertTrue(recommendation.getReasoning().contains("No file extensions"));
  }

  @Test
  void testSupportedImageExtensions() {
    Set<String> supported = ExtensionMatcher.SUPPORTED_IMAGE_EXTENSIONS;
    assertTrue(supported.contains("jpg"));
    assertTrue(supported.contains("jpeg"));
    assertTrue(supported.contains("png"));
    assertTrue(supported.contains("tiff"));
    assertTrue(supported.contains("tif"));
    assertTrue(supported.contains("bmp"));
    assertTrue(supported.contains("gif"));
    assertTrue(supported.contains("webp"));
  }

  @Test
  void testAnyImageExtensionPattern() {
    String pattern = ExtensionMatcher.ANY_IMAGE_EXTENSION_PATTERN;
    assertTrue(pattern.contains("jpg"));
    assertTrue(pattern.contains("jpeg"));
    assertTrue(pattern.contains("png"));
    assertTrue(pattern.contains("tiff"));
    assertTrue(pattern.contains("(?i:"));
  }
}
