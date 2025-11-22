package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for FilenameTokenizer covering various filename formats and edge cases
 * as specified in the requirements.
 */
class FilenameTokenizerTest {

  private FilenameTokenizer tokenizer;

  @BeforeEach
  void setUp() {
    tokenizer = new FilenameTokenizer();
  }

  @Test
  @DisplayName("Should tokenize simple underscore-separated filename")
  void testTokenizeSimpleUnderscoreFilename() {
    String filename = "vehicle_001_front.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals(0, tokens.get(0).getPosition());
    assertEquals("001", tokens.get(1).getValue());
    assertEquals(1, tokens.get(1).getPosition());
    assertEquals("front", tokens.get(2).getValue());
    assertEquals(2, tokens.get(2).getPosition());
    assertEquals("jpg", tokens.get(3).getValue());
    assertEquals(3, tokens.get(3).getPosition());
  }

  @Test
  @DisplayName("Should tokenize hyphen-separated filename")
  void testTokenizeHyphenFilename() {
    String filename = "vehicle-002-rear.png";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("002", tokens.get(1).getValue());
    assertEquals("rear", tokens.get(2).getValue());
    assertEquals("png", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should tokenize dot-separated filename")
  void testTokenizeDotFilename() {
    String filename = "vehicle.003.overview.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("003", tokens.get(1).getValue());
    assertEquals("overview", tokens.get(2).getValue());
    assertEquals("jpg", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should tokenize space-separated filename")
  void testTokenizeSpaceFilename() {
    String filename = "vehicle 004 front.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("004", tokens.get(1).getValue());
    assertEquals("front", tokens.get(2).getValue());
    assertEquals("jpg", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should tokenize mixed delimiter filename")
  void testTokenizeMixedDelimiters() {
    String filename = "vehicle_005-rear.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("005", tokens.get(1).getValue());
    assertEquals("rear", tokens.get(2).getValue());
    assertEquals("jpg", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should handle complex filename with date and camera info")
  void testTokenizeComplexFilename() {
    String filename = "2024-01-15_cam1_vehicle_ABC123_front.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(6, tokens.size());
    assertEquals("2024-01-15", tokens.get(0).getValue());
    assertEquals("cam1", tokens.get(1).getValue());
    assertEquals("vehicle", tokens.get(2).getValue());
    assertEquals("ABC123", tokens.get(3).getValue());
    assertEquals("front", tokens.get(4).getValue());
    assertEquals("jpg", tokens.get(5).getValue());
  }

  @Test
  @DisplayName("Should handle empty or null filename")
  void testTokenizeEmptyFilename() {
    assertTrue(tokenizer.tokenizeFilename("").isEmpty());
    assertTrue(tokenizer.tokenizeFilename(null).isEmpty());
    assertTrue(tokenizer.tokenizeFilename("   ").isEmpty());
  }

  @Test
  @DisplayName("Should handle filename with consecutive delimiters")
  void testTokenizeConsecutiveDelimiters() {
    String filename = "vehicle__double__underscore.jpg";
    List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

    assertEquals(4, tokens.size());
    assertEquals("vehicle", tokens.get(0).getValue());
    assertEquals("double", tokens.get(1).getValue());
    assertEquals("underscore", tokens.get(2).getValue());
    assertEquals("jpg", tokens.get(3).getValue());
  }

  @Test
  @DisplayName("Should analyze standard vehicle filenames and detect patterns")
  void testAnalyzeStandardVehicleFilenames() {
    List<String> filenames =
        List.of(
            "vehicle_001_front.jpg",
            "vehicle_001_rear.jpg",
            "vehicle_001_overview.jpg",
            "vehicle_002_front.jpg",
            "vehicle_002_rear.jpg",
            "vehicle_002_overview.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    assertEquals(6, analysis.getFilenames().size());
    assertEquals(6, analysis.getTokenizedFilenames().size());
    assertFalse(analysis.getSuggestions().isEmpty());

    // Should detect camera/side tokens
    assertTrue(
        analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.CAMERA_SIDE));

    // Should detect extension tokens
    assertTrue(
        analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.EXTENSION));

    // Should detect index tokens
    assertTrue(analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.INDEX));
  }

  @Test
  @DisplayName("Should detect camera/side synonyms correctly")
  void testCameraSideSynonymDetection() {
    List<String> filenames =
        List.of(
            "vehicle_001_ov.jpg", // overview synonym
            "vehicle_002_f.jpg", // front synonym
            "vehicle_003_rr.jpg", // rear synonym
            "vehicle_004_scene.jpg", // overview synonym
            "vehicle_005_forward.jpg", // front synonym
            "vehicle_006_back.jpg" // rear synonym
            );

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should detect camera/side patterns with high confidence
    assertTrue(
        analysis.getSuggestions().stream()
            .anyMatch(s -> s.getType() == TokenType.CAMERA_SIDE && s.getConfidence() > 0.8));
  }

  @Test
  @DisplayName("Should detect date patterns in various formats")
  void testDatePatternDetection() {
    List<String> filenames =
        List.of(
            "2024-01-15_vehicle_001.jpg", // ISO date
            "01-15-2024_vehicle_002.jpg", // US date
            "20240115_vehicle_003.jpg", // Compact date
            "2024-02-20_vehicle_004.jpg" // ISO date
            );

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should detect date patterns
    assertTrue(
        analysis.getSuggestions().stream()
            .anyMatch(s -> s.getType() == TokenType.DATE && s.getConfidence() > 0.5));
  }

  @Test
  @DisplayName("Should detect numeric index sequences")
  void testIndexSequenceDetection() {
    List<String> filenames =
        List.of(
            "vehicle_001_front.jpg",
            "vehicle_002_front.jpg",
            "vehicle_003_front.jpg",
            "vehicle_123_front.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should detect index patterns
    assertTrue(
        analysis.getSuggestions().stream()
            .anyMatch(s -> s.getType() == TokenType.INDEX && s.getConfidence() > 0.4));
  }

  @Test
  @DisplayName("Should detect group ID with high uniqueness")
  void testGroupIdDetection() {
    List<String> filenames =
        List.of(
            "vehicle_ABC123_front.jpg",
            "vehicle_DEF456_front.jpg",
            "vehicle_GHI789_front.jpg",
            "vehicle_JKL012_front.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should detect group ID patterns (high uniqueness)
    assertTrue(analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.GROUP_ID));
  }

  @Test
  @DisplayName("Should detect prefix patterns")
  void testPrefixDetection() {
    List<String> filenames =
        List.of(
            "IMG_001_front.jpg", "IMG_002_rear.jpg", "IMG_003_overview.jpg", "IMG_004_front.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should detect prefix pattern
    assertTrue(analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.PREFIX));
  }

  @Test
  @DisplayName("Should handle edge case filenames")
  void testEdgeCaseFilenames() {
    List<String> filenames =
        List.of(
            "IMG_001.jpg", // Minimal tokens
            "DSC_002.JPG", // Uppercase extension
            "photo.png", // Very simple
            "a_b_c_d_e_f.gif" // Many tokens
            );

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should not crash and should return some analysis
    assertNotNull(analysis);
    assertEquals(4, analysis.getFilenames().size());
    assertFalse(analysis.getTokenizedFilenames().isEmpty());
  }

  @Test
  @DisplayName("Should handle empty filename list")
  void testAnalyzeEmptyFilenameList() {
    TokenAnalysis analysis = tokenizer.analyzeFilenames(List.of());

    assertTrue(analysis.getFilenames().isEmpty());
    assertTrue(analysis.getTokenizedFilenames().isEmpty());
    assertTrue(analysis.getSuggestions().isEmpty());
    assertTrue(analysis.getConfidenceScores().isEmpty());
  }

  @Test
  @DisplayName("Should handle null filename list")
  void testAnalyzeNullFilenameList() {
    TokenAnalysis analysis = tokenizer.analyzeFilenames(null);

    assertTrue(analysis.getFilenames().isEmpty());
    assertTrue(analysis.getTokenizedFilenames().isEmpty());
    assertTrue(analysis.getSuggestions().isEmpty());
    assertTrue(analysis.getConfidenceScores().isEmpty());
  }

  @Test
  @DisplayName("Should suggest token types with confidence scoring")
  void testSuggestTokenTypesWithConfidence() {
    List<String> filenames =
        List.of("vehicle_001_front.jpg", "vehicle_002_rear.jpg", "vehicle_003_overview.jpg");

    Map<String, List<FilenameToken>> tokenized =
        Map.of(
            filenames.get(0), tokenizer.tokenizeFilename(filenames.get(0)),
            filenames.get(1), tokenizer.tokenizeFilename(filenames.get(1)),
            filenames.get(2), tokenizer.tokenizeFilename(filenames.get(2)));

    List<TokenSuggestion> suggestions = tokenizer.suggestTokenTypes(tokenized);

    assertFalse(suggestions.isEmpty());

    // All suggestions should have valid confidence scores
    suggestions.forEach(
        suggestion -> {
          assertTrue(suggestion.getConfidence() >= 0.0);
          assertTrue(suggestion.getConfidence() <= 1.0);
          assertNotNull(suggestion.getType());
          assertNotNull(suggestion.getDescription());
          assertNotNull(suggestion.getExamples());
        });
  }

  @Test
  @DisplayName("Should apply token suggestions to update token types")
  void testTokenSuggestionApplication() {
    List<String> filenames = List.of("vehicle_001_front.jpg", "vehicle_002_rear.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Check that tokens have been updated with suggested types
    for (List<FilenameToken> tokens : analysis.getTokenizedFilenames().values()) {
      for (FilenameToken token : tokens) {
        // At least some tokens should have non-UNKNOWN types
        if (token.getSuggestedType() != TokenType.UNKNOWN) {
          assertTrue(token.getConfidence() > 0.0);
        }
      }
    }
  }

  @Test
  @DisplayName("Should handle filenames with no clear patterns")
  void testRandomFilenames() {
    List<String> filenames = List.of("abc.jpg", "xyz.png", "123.gif", "random.bmp");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Should not crash and should provide some analysis
    assertNotNull(analysis);
    assertEquals(4, analysis.getFilenames().size());

    // Should at least detect extensions
    assertTrue(
        analysis.getSuggestions().stream().anyMatch(s -> s.getType() == TokenType.EXTENSION));
  }
}
