package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for TokenAnalysis class. */
class TokenAnalysisTest {

  private TokenAnalysis analysis;
  private List<String> testFilenames;
  private Map<String, List<FilenameToken>> testTokenizedFilenames;
  private List<TokenSuggestion> testSuggestions;
  private Map<TokenType, Double> testConfidenceScores;

  @BeforeEach
  void setUp() {
    testFilenames = List.of("vehicle_001_front.jpg", "vehicle_002_rear.jpg");

    testTokenizedFilenames =
        Map.of(
            "vehicle_001_front.jpg",
                List.of(
                    new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.8),
                    new FilenameToken("001", 1, TokenType.INDEX, 0.9),
                    new FilenameToken("front", 2, TokenType.CAMERA_SIDE, 0.95),
                    new FilenameToken("jpg", 3, TokenType.EXTENSION, 1.0)),
            "vehicle_002_rear.jpg",
                List.of(
                    new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.8),
                    new FilenameToken("002", 1, TokenType.INDEX, 0.9),
                    new FilenameToken("rear", 2, TokenType.CAMERA_SIDE, 0.95),
                    new FilenameToken("jpg", 3, TokenType.EXTENSION, 1.0)));

    testSuggestions =
        List.of(
            new TokenSuggestion(TokenType.PREFIX, "Fixed prefix", List.of("vehicle"), 0.8),
            new TokenSuggestion(TokenType.INDEX, "Numeric index", List.of("001", "002"), 0.9),
            new TokenSuggestion(
                TokenType.CAMERA_SIDE, "Camera side", List.of("front", "rear"), 0.95),
            new TokenSuggestion(TokenType.EXTENSION, "File extension", List.of("jpg"), 1.0));

    testConfidenceScores =
        Map.of(
            TokenType.PREFIX, 0.8,
            TokenType.INDEX, 0.9,
            TokenType.CAMERA_SIDE, 0.95,
            TokenType.EXTENSION, 1.0);

    analysis =
        new TokenAnalysis(
            testFilenames, testTokenizedFilenames, testSuggestions, testConfidenceScores);
  }

  @Test
  @DisplayName("Should return immutable copies of collections")
  void testImmutableCollections() {
    // Test that returned collections are immutable copies
    List<String> filenames = analysis.getFilenames();
    assertThrows(UnsupportedOperationException.class, () -> filenames.add("test"));

    List<TokenSuggestion> suggestions = analysis.getSuggestions();
    assertThrows(UnsupportedOperationException.class, () -> suggestions.add(null));

    Map<TokenType, Double> confidenceScores = analysis.getConfidenceScores();
    assertThrows(
        UnsupportedOperationException.class, () -> confidenceScores.put(TokenType.UNKNOWN, 0.0));

    Map<String, List<FilenameToken>> tokenizedFilenames = analysis.getTokenizedFilenames();
    assertThrows(
        UnsupportedOperationException.class, () -> tokenizedFilenames.put("test", List.of()));
  }

  @Test
  @DisplayName("Should return correct filenames")
  void testGetFilenames() {
    List<String> filenames = analysis.getFilenames();
    assertEquals(2, filenames.size());
    assertTrue(filenames.contains("vehicle_001_front.jpg"));
    assertTrue(filenames.contains("vehicle_002_rear.jpg"));
  }

  @Test
  @DisplayName("Should return correct tokenized filenames")
  void testGetTokenizedFilenames() {
    Map<String, List<FilenameToken>> tokenized = analysis.getTokenizedFilenames();
    assertEquals(2, tokenized.size());

    List<FilenameToken> tokens1 = tokenized.get("vehicle_001_front.jpg");
    assertNotNull(tokens1);
    assertEquals(4, tokens1.size());
    assertEquals("vehicle", tokens1.get(0).getValue());
    assertEquals("001", tokens1.get(1).getValue());
    assertEquals("front", tokens1.get(2).getValue());
    assertEquals("jpg", tokens1.get(3).getValue());
  }

  @Test
  @DisplayName("Should return correct suggestions")
  void testGetSuggestions() {
    List<TokenSuggestion> suggestions = analysis.getSuggestions();
    assertEquals(4, suggestions.size());

    assertTrue(suggestions.stream().anyMatch(s -> s.getType() == TokenType.PREFIX));
    assertTrue(suggestions.stream().anyMatch(s -> s.getType() == TokenType.INDEX));
    assertTrue(suggestions.stream().anyMatch(s -> s.getType() == TokenType.CAMERA_SIDE));
    assertTrue(suggestions.stream().anyMatch(s -> s.getType() == TokenType.EXTENSION));
  }

  @Test
  @DisplayName("Should return correct confidence scores")
  void testGetConfidenceScores() {
    Map<TokenType, Double> scores = analysis.getConfidenceScores();
    assertEquals(4, scores.size());
    assertEquals(0.8, scores.get(TokenType.PREFIX));
    assertEquals(0.9, scores.get(TokenType.INDEX));
    assertEquals(0.95, scores.get(TokenType.CAMERA_SIDE));
    assertEquals(1.0, scores.get(TokenType.EXTENSION));
  }

  @Test
  @DisplayName("Should return tokens for specific filename")
  void testGetTokensForFilename() {
    List<FilenameToken> tokens = analysis.getTokensForFilename("vehicle_001_front.jpg");
    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("001", tokens.get(1).getValue());
    assertEquals("front", tokens.get(2).getValue());
    assertEquals("jpg", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should return empty list for unknown filename")
  void testGetTokensForUnknownFilename() {
    List<FilenameToken> tokens = analysis.getTokensForFilename("unknown.jpg");
    assertTrue(tokens.isEmpty());
  }

  @Test
  @DisplayName("Should return best suggestion with highest confidence")
  void testGetBestSuggestion() {
    TokenSuggestion best = analysis.getBestSuggestion();
    assertNotNull(best);
    assertEquals(TokenType.EXTENSION, best.getType());
    assertEquals(1.0, best.getConfidence());
  }

  @Test
  @DisplayName("Should return suggestions for specific token type")
  void testGetSuggestionsForType() {
    List<TokenSuggestion> cameraSuggestions = analysis.getSuggestionsForType(TokenType.CAMERA_SIDE);
    assertEquals(1, cameraSuggestions.size());
    assertEquals(TokenType.CAMERA_SIDE, cameraSuggestions.get(0).getType());

    List<TokenSuggestion> unknownSuggestions = analysis.getSuggestionsForType(TokenType.UNKNOWN);
    assertTrue(unknownSuggestions.isEmpty());
  }

  @Test
  @DisplayName("Should handle empty analysis")
  void testEmptyAnalysis() {
    TokenAnalysis emptyAnalysis = new TokenAnalysis(List.of(), Map.of(), List.of(), Map.of());

    assertTrue(emptyAnalysis.getFilenames().isEmpty());
    assertTrue(emptyAnalysis.getTokenizedFilenames().isEmpty());
    assertTrue(emptyAnalysis.getSuggestions().isEmpty());
    assertTrue(emptyAnalysis.getConfidenceScores().isEmpty());
    assertNull(emptyAnalysis.getBestSuggestion());
    assertTrue(emptyAnalysis.getTokensForFilename("any").isEmpty());
    assertTrue(emptyAnalysis.getSuggestionsForType(TokenType.PREFIX).isEmpty());
  }
}
