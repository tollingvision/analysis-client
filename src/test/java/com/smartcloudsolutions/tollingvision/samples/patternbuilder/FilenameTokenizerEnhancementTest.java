package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for enhanced FilenameTokenizer functionality including front token detection. */
class FilenameTokenizerEnhancementTest {

  private FilenameTokenizer tokenizer;

  @BeforeEach
  void setUp() {
    tokenizer = new FilenameTokenizer();
  }

  @Test
  void testFrontTokenDetection() {
    assertTrue(tokenizer.isFrontToken("front"));
    assertTrue(tokenizer.isFrontToken("f"));
    assertTrue(tokenizer.isFrontToken("fr"));
    assertTrue(tokenizer.isFrontToken("forward"));
    assertTrue(tokenizer.isFrontToken("FRONT")); // Case insensitive

    assertFalse(tokenizer.isFrontToken("rear"));
    assertFalse(tokenizer.isFrontToken("overview"));
    assertFalse(tokenizer.isFrontToken("side"));
  }

  @Test
  void testGetImageRoleForToken() {
    assertEquals(ImageRole.FRONT, tokenizer.getImageRoleForToken("front"));
    assertEquals(ImageRole.FRONT, tokenizer.getImageRoleForToken("f"));
    assertEquals(ImageRole.FRONT, tokenizer.getImageRoleForToken("forward"));

    assertEquals(ImageRole.REAR, tokenizer.getImageRoleForToken("rear"));
    assertEquals(ImageRole.REAR, tokenizer.getImageRoleForToken("r"));
    assertEquals(ImageRole.REAR, tokenizer.getImageRoleForToken("back"));

    assertEquals(ImageRole.OVERVIEW, tokenizer.getImageRoleForToken("overview"));
    assertEquals(ImageRole.OVERVIEW, tokenizer.getImageRoleForToken("ov"));
    assertEquals(ImageRole.OVERVIEW, tokenizer.getImageRoleForToken("scene"));

    assertNull(tokenizer.getImageRoleForToken("unknown"));
    assertNull(tokenizer.getImageRoleForToken("123"));
  }

  @Test
  void testAnalyzeFilenamesWithFrontTokens() {
    List<String> filenames =
        List.of(
            "vehicle_001_front.jpg",
            "vehicle_002_f.jpg",
            "vehicle_003_forward.jpg",
            "vehicle_004_rear.jpg");

    TokenAnalysis analysis = tokenizer.analyzeFilenames(filenames);

    // Check that front tokens are properly detected
    List<FilenameToken> tokens = analysis.getTokensForFilename("vehicle_001_front.jpg");

    FilenameToken frontToken =
        tokens.stream().filter(t -> t.getValue().equals("front")).findFirst().orElse(null);

    assertNotNull(frontToken);
    assertEquals(TokenType.CAMERA_SIDE, frontToken.getSuggestedType());
    assertTrue(frontToken.getConfidence() > 0.5);
  }

  @Test
  void testTokenizationWithVariousFrontSynonyms() {
    List<String> testFilenames =
        List.of(
            "cam_123_front_20240101.jpg",
            "img_456_f_timestamp.png",
            "photo_789_fr_data.jpg",
            "pic_000_forward_info.jpeg");

    for (String filename : testFilenames) {
      List<FilenameToken> tokens = tokenizer.tokenizeFilename(filename);

      // Find the camera/side token
      FilenameToken cameraToken =
          tokens.stream()
              .filter(t -> tokenizer.isFrontToken(t.getValue()))
              .findFirst()
              .orElse(null);

      assertNotNull(cameraToken, "Front token not found in: " + filename);
      assertEquals(ImageRole.FRONT, tokenizer.getImageRoleForToken(cameraToken.getValue()));
    }
  }
}
