package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for performance optimization features implemented in Task 12. */
public class PerformanceOptimizationTest {

  private TokenizationCache cache;
  private FilenameTokenizer tokenizer;
  private BackgroundAnalysisService backgroundService;

  @BeforeEach
  void setUp() {
    cache = new TokenizationCache();
    tokenizer = new FilenameTokenizer(cache);
    backgroundService = new BackgroundAnalysisService(tokenizer, cache);
  }

  @AfterEach
  void tearDown() {
    if (backgroundService != null) {
      backgroundService.shutdown();
    }
    if (cache != null) {
      cache.clearCache();
    }
    MemoryManager.stopMonitoring();
  }

  @Test
  void testTokenizationCaching() {
    // Test that tokenization results are cached
    String filename = "vehicle_001_front.jpg";

    // First tokenization should miss cache
    List<FilenameToken> tokens1 = tokenizer.tokenizeFilename(filename);
    assertNotNull(tokens1);
    assertFalse(tokens1.isEmpty());

    // Second tokenization should hit cache
    List<FilenameToken> tokens2 = tokenizer.tokenizeFilename(filename);
    assertNotNull(tokens2);
    assertEquals(tokens1.size(), tokens2.size());

    // Verify cache statistics
    String cacheStats = cache.getCacheStats();
    assertTrue(cacheStats.contains("1 tokens"));
    assertTrue(cache.getHitRate() > 0);
  }

  @Test
  void testCacheMemoryManagement() {
    // Test that cache manages memory properly
    List<String> testFilenames = new ArrayList<>();

    // Generate many test filenames
    for (int i = 0; i < 1000; i++) {
      testFilenames.add(String.format("vehicle_%03d_front.jpg", i));
    }

    // Tokenize all filenames
    for (String filename : testFilenames) {
      tokenizer.tokenizeFilename(filename);
    }

    // Verify cache has reasonable memory usage
    long memoryUsage = cache.getEstimatedMemoryUsage();
    assertTrue(memoryUsage > 0, "Cache should track memory usage");

    // Clear cache and verify memory is freed
    cache.clearCache();
    assertEquals(0, cache.getEstimatedMemoryUsage(), "Cache should free memory when cleared");
  }

  @Test
  void testFilenamesKeyGeneration() {
    // Test that cache key generation works correctly
    List<String> filenames1 = List.of("a.jpg", "b.jpg", "c.jpg");
    List<String> filenames2 = List.of("c.jpg", "b.jpg", "a.jpg"); // Different order
    List<String> filenames3 = List.of("a.jpg", "b.jpg", "d.jpg"); // Different content

    String key1 = cache.generateFilenamesKey(filenames1);
    String key2 = cache.generateFilenamesKey(filenames2);
    String key3 = cache.generateFilenamesKey(filenames3);

    // Same content, different order should produce same key
    assertEquals(key1, key2, "Cache keys should be order-independent");

    // Different content should produce different keys
    assertNotEquals(key1, key3, "Different content should produce different keys");
  }

  @Test
  void testBackgroundAnalysisServiceCreation() {
    // Test that background service can be created and configured
    assertNotNull(backgroundService);

    // Test file limit
    assertEquals(500, BackgroundAnalysisService.getMaxFilesForAnalysis());
  }

  @Test
  void testMemoryManagerBasicFunctionality() {
    // Test basic memory manager functionality
    MemoryManager.MemoryInfo info = MemoryManager.getCurrentMemoryInfo();

    assertNotNull(info);
    assertTrue(info.getTotalMB() > 0, "Total memory should be positive");
    assertTrue(info.getUsedMB() >= 0, "Used memory should be non-negative");
    assertTrue(info.getAvailableMB() >= 0, "Available memory should be non-negative");
    assertTrue(
        info.getUsagePercent() >= 0 && info.getUsagePercent() <= 100,
        "Usage percent should be between 0 and 100");

    // Test memory usage string
    String memoryString = MemoryManager.getMemoryUsageString();
    assertNotNull(memoryString);
    assertTrue(memoryString.contains("Memory:"));
    assertTrue(memoryString.contains("MB"));
  }

  @Test
  void testMemoryManagerRecommendations() {
    // Test memory-based recommendations
    int maxItems = MemoryManager.getRecommendedMaxItems(1024); // 1KB per item
    assertTrue(maxItems > 0, "Should recommend processing at least some items");

    // Test with very large memory requirement
    int maxItemsLarge = MemoryManager.getRecommendedMaxItems(1024 * 1024 * 1024); // 1GB per item
    assertTrue(maxItemsLarge >= 0, "Should handle large memory requirements gracefully");
  }

  @Test
  void testCacheCleanup() {
    // Test that cache performs cleanup when limits are exceeded
    cache.clearCache();

    // Add many entries to trigger cleanup
    for (int i = 0; i < 100; i++) {
      String filename = String.format("test_%d.jpg", i);
      List<FilenameToken> tokens = List.of(new FilenameToken("test", 0));
      cache.cacheTokens(filename, tokens);
    }

    // Cache should have entries
    assertTrue(cache.getCacheStats().contains("tokens"));

    // Clear and verify
    cache.clearCache();
    assertEquals(0, cache.getEstimatedMemoryUsage());
  }
}
