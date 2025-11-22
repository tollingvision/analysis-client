package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for CustomTokenManager functionality. */
class CustomTokenManagerTest {

  private CustomTokenManager customTokenManager;

  @BeforeEach
  void setUp() {
    customTokenManager = new CustomTokenManager();
  }

  @Test
  void testAddCustomToken() {
    CustomTokenManager.CustomToken token =
        new CustomTokenManager.CustomToken(
            "Lane", "Traffic lane identifier", Set.of("lane1", "lane2"), TokenType.SUFFIX);

    customTokenManager.addCustomToken(token);

    assertEquals(1, customTokenManager.getCustomTokenCount());
    assertEquals(token, customTokenManager.getCustomToken("Lane"));
  }

  @Test
  void testFindMatchingCustomToken() {
    CustomTokenManager.CustomToken token =
        new CustomTokenManager.CustomToken(
            "Lane", "Traffic lane identifier", Set.of("lane1", "lane2"), TokenType.SUFFIX);

    customTokenManager.addCustomToken(token);

    assertEquals(token, customTokenManager.findMatchingCustomToken("lane1"));
    assertEquals(token, customTokenManager.findMatchingCustomToken("LANE1")); // Case insensitive
    assertNull(customTokenManager.findMatchingCustomToken("lane3"));
  }

  @Test
  void testLoadPreconfiguredCustomTokens() {
    customTokenManager.loadPreconfiguredCustomTokens();

    assertTrue(customTokenManager.getCustomTokenCount() > 0);
    assertNotNull(customTokenManager.getCustomToken("Lane"));
    assertNotNull(customTokenManager.getCustomToken("Direction"));
  }

  @Test
  void testEnhanceWithCustomTokens() {
    // Add a custom token
    CustomTokenManager.CustomToken laneToken =
        new CustomTokenManager.CustomToken(
            "Lane", "Traffic lane identifier", Set.of("lane1", "lane2"), TokenType.SUFFIX);
    customTokenManager.addCustomToken(laneToken);

    // Create test tokens
    List<FilenameToken> tokens =
        List.of(
            new FilenameToken("vehicle", 0, TokenType.PREFIX, 0.8),
            new FilenameToken("123", 1, TokenType.GROUP_ID, 0.9),
            new FilenameToken("lane1", 2, TokenType.UNKNOWN, 0.1),
            new FilenameToken("jpg", 3, TokenType.EXTENSION, 0.9));

    Map<String, List<FilenameToken>> tokenizedFilenames = Map.of("test.jpg", tokens);

    TokenAnalysis originalAnalysis =
        new TokenAnalysis(List.of("test.jpg"), tokenizedFilenames, List.of(), Map.of());

    // Enhance with custom tokens
    TokenAnalysis enhancedAnalysis = customTokenManager.enhanceWithCustomTokens(originalAnalysis);

    List<FilenameToken> enhancedTokens = enhancedAnalysis.getTokensForFilename("test.jpg");

    // Check that lane1 token was enhanced
    FilenameToken enhancedLaneToken =
        enhancedTokens.stream().filter(t -> t.getValue().equals("lane1")).findFirst().orElse(null);

    assertNotNull(enhancedLaneToken);
    assertEquals(TokenType.SUFFIX, enhancedLaneToken.getSuggestedType());
    assertEquals(0.9, enhancedLaneToken.getConfidence(), 0.01);
  }

  @Test
  void testCustomTokenDialogFlag() {
    assertFalse(customTokenManager.hasCustomTokenDialogBeenShown());

    customTokenManager.markCustomTokenDialogShown();
    assertTrue(customTokenManager.hasCustomTokenDialogBeenShown());

    customTokenManager.resetCustomTokenDialogFlag();
    assertFalse(customTokenManager.hasCustomTokenDialogBeenShown());
  }
}
