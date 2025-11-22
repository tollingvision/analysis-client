package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Task 16: Enhanced token selection with front and custom token propagation.
 * Tests all the requirements from Requirement 12.
 */
class Task16IntegrationTest {

  private FilenameTokenizer tokenizer;
  private CustomTokenManager customTokenManager;

  @BeforeEach
  void setUp() {
    tokenizer = new FilenameTokenizer();
    customTokenManager = new CustomTokenManager();
  }

  @Test
  void testRequirement12_1_FrontTokenDetectionInTokenSelection() {
    // Requirement 12.1: WHEN filename analysis detects front tokens (with synonyms: front, f, fr,
    // forward)
    // THEN the system SHALL display them in the Token Selection screen as selectable options

    List<String> filenames =
        List.of(
            "vehicle_001_front.jpg",
            "vehicle_002_f.jpg",
            "vehicle_003_fr.jpg",
            "vehicle_004_forward.jpg",
            "vehicle_005_rear.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Verify front tokens are detected and available
    List<FilenameToken> tokens = analysis.getTokensForFilename("vehicle_001_front.jpg");

    boolean frontTokenFound =
        tokens.stream()
            .anyMatch(
                token ->
                    tokenizer.isFrontToken(token.getValue())
                        && token.getSuggestedType() == TokenType.CAMERA_SIDE);

    assertTrue(frontTokenFound, "Front token should be detected and classified as CAMERA_SIDE");

    // Test all front synonyms
    String[] frontSynonyms = {"front", "f", "fr", "forward"};
    for (String synonym : frontSynonyms) {
      assertTrue(
          tokenizer.isFrontToken(synonym), "Front synonym '" + synonym + "' should be detected");
      assertEquals(
          ImageRole.FRONT,
          tokenizer.getImageRoleForToken(synonym),
          "Front synonym '" + synonym + "' should map to FRONT role");
    }
  }

  @Test
  void testRequirement12_2_PreconfiguredCustomTokensInTokenSelection() {
    // Requirement 12.2: WHEN preconfigured Custom Tokens exist
    // THEN the system SHALL pre-populate them in the Token Selection screen for user selection

    // Load preconfigured custom tokens
    customTokenManager.loadPreconfiguredCustomTokens();

    assertTrue(
        customTokenManager.getCustomTokenCount() > 0,
        "Preconfigured custom tokens should be loaded");

    // Verify specific preconfigured tokens exist
    assertNotNull(
        customTokenManager.getCustomToken("Lane"), "Lane custom token should be preconfigured");
    assertNotNull(
        customTokenManager.getCustomToken("Direction"),
        "Direction custom token should be preconfigured");
    assertNotNull(
        customTokenManager.getCustomToken("Station"),
        "Station custom token should be preconfigured");

    // Test that custom tokens are applied during analysis
    List<String> filenames = List.of("station1_vehicle_123_lane1.jpg");
    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Enhance with custom tokens
    TokenAnalysis enhancedAnalysis = customTokenManager.enhanceWithCustomTokens(analysis);

    List<FilenameToken> tokens =
        enhancedAnalysis.getTokensForFilename("station1_vehicle_123_lane1.jpg");

    // Verify custom tokens are applied
    boolean stationTokenFound =
        tokens.stream()
            .anyMatch(
                token ->
                    token.getValue().equals("station1")
                        && token.getSuggestedType() == TokenType.PREFIX);

    boolean laneTokenFound =
        tokens.stream()
            .anyMatch(
                token ->
                    token.getValue().equals("lane1")
                        && token.getSuggestedType() == TokenType.SUFFIX);

    assertTrue(stationTokenFound, "Station custom token should be applied");
    assertTrue(laneTokenFound, "Lane custom token should be applied");
  }

  @Test
  void testRequirement12_3_CustomTokenDialogOnlyOnceInSimpleMode() {
    // Requirement 12.3: WHEN switching into Simple mode for the first time
    // THEN the system SHALL show the Custom Token handling dialog once

    assertFalse(
        customTokenManager.hasCustomTokenDialogBeenShown(),
        "Custom token dialog should not be shown initially");

    // Simulate showing the dialog
    customTokenManager.markCustomTokenDialogShown();

    assertTrue(
        customTokenManager.hasCustomTokenDialogBeenShown(),
        "Custom token dialog should be marked as shown");

    // Reset and test again
    customTokenManager.resetCustomTokenDialogFlag();
    assertFalse(
        customTokenManager.hasCustomTokenDialogBeenShown(),
        "Custom token dialog flag should be resettable");
  }

  @Test
  void testRequirement12_4_NoUnexpectedCustomTokenDialogs() {
    // Requirement 12.4: WHEN navigating normally within Simple mode OR using Advanced mode
    // THEN the system SHALL NOT show unexpected Custom Token dialogs

    // This is primarily a behavioral requirement that would be tested through UI interaction
    // We can test the flag mechanism that prevents unexpected dialogs

    customTokenManager.markCustomTokenDialogShown();
    assertTrue(
        customTokenManager.hasCustomTokenDialogBeenShown(),
        "Dialog should be marked as shown to prevent unexpected dialogs");

    // The dialog should not be shown again unless explicitly reset
    // This prevents unexpected dialogs during normal navigation
  }

  @Test
  void testRequirement12_5_FrontTokensPropagateToRolesStep() {
    // Requirement 12.5: WHEN front tokens are selected
    // THEN the system SHALL carry them through to the Roles step for rule configuration

    List<String> filenames =
        List.of("vehicle_001_front.jpg", "vehicle_002_rear.jpg", "vehicle_003_overview.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);
    List<FilenameToken> tokens = analysis.getTokensForFilename("vehicle_001_front.jpg");

    // Verify that front tokens are available for rule configuration
    FilenameToken frontToken =
        tokens.stream()
            .filter(token -> tokenizer.isFrontToken(token.getValue()))
            .findFirst()
            .orElse(null);

    assertNotNull(frontToken, "Front token should be available");
    assertEquals(
        TokenType.CAMERA_SIDE,
        frontToken.getSuggestedType(),
        "Front token should be classified as CAMERA_SIDE");
    assertEquals(
        ImageRole.FRONT,
        tokenizer.getImageRoleForToken(frontToken.getValue()),
        "Front token should map to FRONT role for rule configuration");
  }

  @Test
  void testEndToEndTokenPropagation() {
    // Test the complete flow from tokenization to rule suggestion

    // 1. Load custom tokens
    customTokenManager.loadPreconfiguredCustomTokens();

    // 2. Analyze filenames with front tokens and custom tokens
    List<String> filenames =
        List.of(
            "station1_vehicle_001_front_lane1.jpg",
            "station1_vehicle_002_rear_lane2.jpg",
            "station2_vehicle_003_f_lane1.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);
    TokenAnalysis enhancedAnalysis = customTokenManager.enhanceWithCustomTokens(analysis);

    // 3. Enhanced analysis is ready for token selection

    // 4. Get tokens for role rule suggestion
    List<FilenameToken> tokens =
        enhancedAnalysis.getTokensForFilename("station1_vehicle_001_front_lane1.jpg");

    // 5. Test that all token types are properly detected
    Map<TokenType, Long> tokenTypeCounts =
        tokens.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    FilenameToken::getSuggestedType, java.util.stream.Collectors.counting()));

    assertTrue(tokenTypeCounts.containsKey(TokenType.PREFIX), "PREFIX tokens should be detected");
    assertTrue(
        tokenTypeCounts.containsKey(TokenType.CAMERA_SIDE),
        "CAMERA_SIDE tokens should be detected");
    assertTrue(tokenTypeCounts.containsKey(TokenType.SUFFIX), "SUFFIX tokens should be detected");
    assertTrue(
        tokenTypeCounts.containsKey(TokenType.EXTENSION), "EXTENSION tokens should be detected");

    // 6. Verify front token propagation
    boolean frontTokenPresent =
        tokens.stream()
            .anyMatch(
                token ->
                    tokenizer.isFrontToken(token.getValue())
                        && token.getSuggestedType() == TokenType.CAMERA_SIDE);

    assertTrue(frontTokenPresent, "Front tokens should propagate through the entire flow");

    // 7. Verify custom token application
    boolean customTokenApplied =
        tokens.stream()
            .anyMatch(
                token -> customTokenManager.findMatchingCustomToken(token.getValue()) != null);

    assertTrue(customTokenApplied, "Custom tokens should be applied in the flow");
  }
}
