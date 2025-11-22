package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for TokenSuggestion class. */
class TokenSuggestionTest {

  @Test
  @DisplayName("Should create token suggestion with valid parameters")
  void testCreateTokenSuggestion() {
    List<String> examples = List.of("front", "rear", "overview");
    TokenSuggestion suggestion =
        new TokenSuggestion(TokenType.CAMERA_SIDE, "Camera position identifier", examples, 0.85);

    assertEquals(TokenType.CAMERA_SIDE, suggestion.getType());
    assertEquals("Camera position identifier", suggestion.getDescription());
    assertEquals(3, suggestion.getExamples().size());
    assertTrue(suggestion.getExamples().contains("front"));
    assertTrue(suggestion.getExamples().contains("rear"));
    assertTrue(suggestion.getExamples().contains("overview"));
    assertEquals(0.85, suggestion.getConfidence());
  }

  @Test
  @DisplayName("Should clamp confidence to valid range")
  void testConfidenceClamping() {
    // Test confidence above 1.0
    TokenSuggestion highConfidence =
        new TokenSuggestion(TokenType.PREFIX, "Test", List.of("test"), 1.5);
    assertEquals(1.0, highConfidence.getConfidence());

    // Test confidence below 0.0
    TokenSuggestion lowConfidence =
        new TokenSuggestion(TokenType.PREFIX, "Test", List.of("test"), -0.5);
    assertEquals(0.0, lowConfidence.getConfidence());

    // Test valid confidence
    TokenSuggestion validConfidence =
        new TokenSuggestion(TokenType.PREFIX, "Test", List.of("test"), 0.75);
    assertEquals(0.75, validConfidence.getConfidence());
  }

  @Test
  @DisplayName("Should return immutable examples list")
  void testImmutableExamples() {
    List<String> examples = List.of("example1", "example2");
    TokenSuggestion suggestion = new TokenSuggestion(TokenType.INDEX, "Test", examples, 0.5);

    List<String> returnedExamples = suggestion.getExamples();
    assertThrows(UnsupportedOperationException.class, () -> returnedExamples.add("example3"));
  }

  @Test
  @DisplayName("Should handle empty examples list")
  void testEmptyExamples() {
    TokenSuggestion suggestion =
        new TokenSuggestion(TokenType.UNKNOWN, "Unknown type", List.of(), 0.0);

    assertTrue(suggestion.getExamples().isEmpty());
    assertEquals(TokenType.UNKNOWN, suggestion.getType());
    assertEquals("Unknown type", suggestion.getDescription());
    assertEquals(0.0, suggestion.getConfidence());
  }

  @Test
  @DisplayName("Should provide meaningful toString representation")
  void testToString() {
    TokenSuggestion suggestion =
        new TokenSuggestion(TokenType.EXTENSION, "File extension", List.of("jpg", "png"), 0.95);

    String toString = suggestion.toString();
    assertTrue(toString.contains("EXTENSION"));
    assertTrue(toString.contains("File extension"));
    assertTrue(toString.contains("0.95"));
    assertTrue(toString.contains("jpg"));
    assertTrue(toString.contains("png"));
  }

  @Test
  @DisplayName("Should handle all token types")
  void testAllTokenTypes() {
    for (TokenType type : TokenType.values()) {
      TokenSuggestion suggestion =
          new TokenSuggestion(type, "Test description for " + type, List.of("example"), 0.5);

      assertEquals(type, suggestion.getType());
      assertNotNull(suggestion.getDescription());
      assertFalse(suggestion.getExamples().isEmpty());
      assertEquals(0.5, suggestion.getConfidence());
    }
  }

  @Test
  @DisplayName("Should handle null-safe operations")
  void testNullSafety() {
    // Test with null description (should not crash)
    TokenSuggestion suggestion = new TokenSuggestion(TokenType.PREFIX, null, List.of("test"), 0.5);

    assertEquals(TokenType.PREFIX, suggestion.getType());
    assertNull(suggestion.getDescription());
    assertEquals(1, suggestion.getExamples().size());
    assertEquals(0.5, suggestion.getConfidence());
  }
}
