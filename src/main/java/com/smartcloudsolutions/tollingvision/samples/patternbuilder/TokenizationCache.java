package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caching system for tokenization results to avoid repeated analysis. Uses memory-efficient storage
 * with automatic cleanup for large datasets.
 */
public class TokenizationCache {

  private static final int MAX_CACHE_SIZE = 10000; // Maximum cached entries
  private static final long MAX_MEMORY_MB = 50; // Maximum memory usage in MB

  // Cache storage
  private final Map<String, List<FilenameToken>> tokenCache = new ConcurrentHashMap<>();
  private final Map<String, TokenAnalysis> analysisCache = new ConcurrentHashMap<>();

  // Memory tracking
  private final AtomicLong estimatedMemoryUsage = new AtomicLong(0);

  // Statistics
  private long cacheHits = 0;
  private long cacheMisses = 0;

  /**
   * Gets cached tokens for a filename.
   *
   * @param filename the filename to get tokens for
   * @return cached tokens, or null if not cached
   */
  public List<FilenameToken> getCachedTokens(String filename) {
    List<FilenameToken> tokens = tokenCache.get(filename);
    if (tokens != null) {
      cacheHits++;
      return tokens;
    }
    cacheMisses++;
    return null;
  }

  /**
   * Caches tokens for a filename.
   *
   * @param filename the filename
   * @param tokens the tokens to cache
   */
  public void cacheTokens(String filename, List<FilenameToken> tokens) {
    if (shouldCache()) {
      tokenCache.put(filename, tokens);
      updateMemoryUsage(filename, tokens);

      // Cleanup if necessary
      if (tokenCache.size() > MAX_CACHE_SIZE || isMemoryLimitExceeded()) {
        performCleanup();
      }
    }
  }

  /**
   * Gets cached analysis results for a set of filenames.
   *
   * @param filenamesKey unique key representing the set of filenames
   * @return cached analysis, or null if not cached
   */
  public TokenAnalysis getCachedAnalysis(String filenamesKey) {
    TokenAnalysis analysis = analysisCache.get(filenamesKey);
    if (analysis != null) {
      cacheHits++;
      return analysis;
    }
    cacheMisses++;
    return null;
  }

  /**
   * Caches analysis results for a set of filenames.
   *
   * @param filenamesKey unique key representing the set of filenames
   * @param analysis the analysis to cache
   */
  public void cacheAnalysis(String filenamesKey, TokenAnalysis analysis) {
    if (shouldCache()) {
      analysisCache.put(filenamesKey, analysis);
      updateMemoryUsageForAnalysis(filenamesKey, analysis);

      // Cleanup if necessary
      if (analysisCache.size() > 100 || isMemoryLimitExceeded()) {
        performAnalysisCleanup();
      }
    }
  }

  /**
   * Generates a cache key for a list of filenames.
   *
   * @param filenames the filenames to generate a key for
   * @return cache key string
   */
  public String generateFilenamesKey(List<String> filenames) {
    if (filenames == null || filenames.isEmpty()) {
      return "empty";
    }

    // Use hash of sorted filenames for consistent key
    return String.valueOf(
        filenames.stream().sorted().mapToInt(String::hashCode).reduce(0, (a, b) -> 31 * a + b));
  }

  /** Checks if an entry should be cached based on current memory usage. */
  private boolean shouldCache() {
    return !isMemoryLimitExceeded();
  }

  /** Checks if memory limit is exceeded. */
  private boolean isMemoryLimitExceeded() {
    long currentMemoryMB = estimatedMemoryUsage.get() / (1024 * 1024);
    return currentMemoryMB > MAX_MEMORY_MB;
  }

  /** Updates estimated memory usage for cached tokens. */
  private void updateMemoryUsage(String filename, List<FilenameToken> tokens) {
    // Rough estimation: filename + tokens
    long filenameSize = filename.length() * 2; // chars to bytes
    long tokensSize = tokens.size() * 100; // rough estimate per token
    estimatedMemoryUsage.addAndGet(filenameSize + tokensSize);
  }

  /** Updates estimated memory usage for cached analysis. */
  private void updateMemoryUsageForAnalysis(String key, TokenAnalysis analysis) {
    // Rough estimation based on number of files and suggestions
    long keySize = key.length() * 2;
    long analysisSize =
        analysis.getFilenames().size() * 50 + analysis.getSuggestions().size() * 200;
    estimatedMemoryUsage.addAndGet(keySize + analysisSize);
  }

  /** Performs cleanup of token cache when limits are exceeded. */
  private void performCleanup() {
    if (tokenCache.size() > MAX_CACHE_SIZE * 0.8) {
      // Remove oldest 20% of entries (simple LRU approximation)
      int toRemove = (int) (tokenCache.size() * 0.2);
      tokenCache.entrySet().stream()
          .limit(toRemove)
          .map(Map.Entry::getKey)
          .forEach(tokenCache::remove);

      // Recalculate memory usage
      recalculateMemoryUsage();
    }
  }

  /** Performs cleanup of analysis cache when limits are exceeded. */
  private void performAnalysisCleanup() {
    if (analysisCache.size() > 50) {
      // Remove oldest 50% of entries
      int toRemove = analysisCache.size() / 2;
      analysisCache.entrySet().stream()
          .limit(toRemove)
          .map(Map.Entry::getKey)
          .forEach(analysisCache::remove);

      // Recalculate memory usage
      recalculateMemoryUsage();
    }
  }

  /** Recalculates memory usage after cleanup. */
  private void recalculateMemoryUsage() {
    long newUsage = 0;

    // Recalculate token cache usage
    for (Map.Entry<String, List<FilenameToken>> entry : tokenCache.entrySet()) {
      newUsage += entry.getKey().length() * 2;
      newUsage += entry.getValue().size() * 100;
    }

    // Recalculate analysis cache usage
    for (Map.Entry<String, TokenAnalysis> entry : analysisCache.entrySet()) {
      newUsage += entry.getKey().length() * 2;
      TokenAnalysis analysis = entry.getValue();
      newUsage += analysis.getFilenames().size() * 50 + analysis.getSuggestions().size() * 200;
    }

    estimatedMemoryUsage.set(newUsage);
  }

  /** Clears all cached data. */
  public void clearCache() {
    tokenCache.clear();
    analysisCache.clear();
    estimatedMemoryUsage.set(0);
    cacheHits = 0;
    cacheMisses = 0;
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics as a formatted string
   */
  public String getCacheStats() {
    long totalRequests = cacheHits + cacheMisses;
    double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
    long memoryMB = estimatedMemoryUsage.get() / (1024 * 1024);

    return String.format(
        "Cache: %d tokens, %d analyses, %.1f%% hit rate, %d MB",
        tokenCache.size(), analysisCache.size(), hitRate, memoryMB);
  }

  /**
   * Gets the current cache hit rate.
   *
   * @return hit rate as a percentage (0-100)
   */
  public double getHitRate() {
    long totalRequests = cacheHits + cacheMisses;
    return totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
  }

  /**
   * Gets the estimated memory usage in bytes.
   *
   * @return estimated memory usage
   */
  public long getEstimatedMemoryUsage() {
    return estimatedMemoryUsage.get();
  }
}
