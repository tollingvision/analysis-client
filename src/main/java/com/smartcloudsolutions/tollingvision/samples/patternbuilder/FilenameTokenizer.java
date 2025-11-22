package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for intelligent analysis of filename patterns with automatic token detection and type
 * suggestion based on common delimiters and pattern recognition. Includes caching support for
 * improved performance with large datasets.
 */
public class FilenameTokenizer {

  // Common delimiters used for tokenization
  private static final Pattern DELIMITER_PATTERN = Pattern.compile("[_\\-\\.\\s]+");

  // Optional cache for performance optimization
  private TokenizationCache cache;

  // Camera/side synonyms for different image roles (case-insensitive)
  private static final Map<ImageRole, Set<String>> CAMERA_SYNONYMS =
      Map.of(
          ImageRole.OVERVIEW, Set.of("overview", "ov", "ovr", "ovw", "scene", "full"),
          ImageRole.FRONT, Set.of("front", "f", "fr", "forward"),
          ImageRole.REAR, Set.of("rear", "r", "rr", "back", "behind"));

  // All camera/side synonyms flattened for easier detection
  private static final Set<String> ALL_CAMERA_SYNONYMS =
      CAMERA_SYNONYMS.values().stream()
          .flatMap(Set::stream)
          .collect(java.util.stream.Collectors.toSet());

  // Common file extensions
  private static final Set<String> IMAGE_EXTENSIONS =
      Set.of("jpg", "jpeg", "png", "bmp", "tiff", "gif");

  // Date patterns (various formats)
  private static final List<Pattern> DATE_PATTERNS =
      List.of(
          Pattern.compile("\\d{4}-\\d{2}-\\d{2}"), // 2024-01-15
          Pattern.compile("\\d{2}-\\d{2}-\\d{4}"), // 01-15-2024
          Pattern.compile("\\d{8}"), // 20240115
          Pattern.compile("\\d{4}\\d{2}\\d{2}"), // 20240115
          Pattern.compile("\\d{2}\\d{2}\\d{4}") // 01152024
          );

  // Index/sequence patterns
  private static final Pattern INDEX_PATTERN = Pattern.compile("\\d{1,6}");

  /** Creates a new FilenameTokenizer without caching. */
  public FilenameTokenizer() {
    this.cache = null;
  }

  /**
   * Creates a new FilenameTokenizer with caching support.
   *
   * @param cache the cache to use for storing tokenization results
   */
  public FilenameTokenizer(TokenizationCache cache) {
    this.cache = cache;
  }

  /**
   * Tokenizes a single filename using common delimiters. Uses caching if available to improve
   * performance.
   *
   * @param filename the filename to tokenize
   * @return list of tokens extracted from the filename
   */
  public List<FilenameToken> tokenizeFilename(String filename) {
    if (filename == null || filename.trim().isEmpty()) {
      return List.of();
    }

    // Check cache first if available
    if (cache != null) {
      List<FilenameToken> cachedTokens = cache.getCachedTokens(filename);
      if (cachedTokens != null) {
        return cachedTokens;
      }
    }

    List<FilenameToken> tokens = new ArrayList<>();

    // Split by delimiters but keep track of positions
    String[] parts = DELIMITER_PATTERN.split(filename);
    int position = 0;

    for (String part : parts) {
      if (!part.isEmpty()) {
        tokens.add(new FilenameToken(part, position));
        position++;
      }
    }

    // Post-process to merge date-like tokens
    List<FilenameToken> finalTokens = mergeDateTokens(tokens);

    // Cache the result if cache is available
    if (cache != null) {
      cache.cacheTokens(filename, finalTokens);
    }

    return finalTokens;
  }

  /** Merges consecutive tokens that form date patterns. */
  private List<FilenameToken> mergeDateTokens(List<FilenameToken> tokens) {
    List<FilenameToken> merged = new ArrayList<>();

    for (int i = 0; i < tokens.size(); i++) {
      FilenameToken current = tokens.get(i);

      // Check if this could be the start of a date pattern (YYYY-MM-DD)
      if (i + 2 < tokens.size()
          && current.getValue().matches("\\d{4}")
          && tokens.get(i + 1).getValue().matches("\\d{2}")
          && tokens.get(i + 2).getValue().matches("\\d{2}")) {

        // Create merged date token
        String dateValue =
            current.getValue()
                + "-"
                + tokens.get(i + 1).getValue()
                + "-"
                + tokens.get(i + 2).getValue();
        merged.add(new FilenameToken(dateValue, current.getPosition()));
        i += 2; // Skip the next two tokens
      }
      // Check for MM-DD-YYYY pattern
      else if (i + 2 < tokens.size()
          && current.getValue().matches("\\d{2}")
          && tokens.get(i + 1).getValue().matches("\\d{2}")
          && tokens.get(i + 2).getValue().matches("\\d{4}")) {

        String dateValue =
            current.getValue()
                + "-"
                + tokens.get(i + 1).getValue()
                + "-"
                + tokens.get(i + 2).getValue();
        merged.add(new FilenameToken(dateValue, current.getPosition()));
        i += 2; // Skip the next two tokens
      } else {
        merged.add(current);
      }
    }

    // Reassign positions after merging
    for (int i = 0; i < merged.size(); i++) {
      FilenameToken token = merged.get(i);
      merged.set(
          i,
          new FilenameToken(token.getValue(), i, token.getSuggestedType(), token.getConfidence()));
    }

    return merged;
  }

  /**
   * Analyzes multiple filenames to detect patterns and suggest token types.
   *
   * @param filenames list of filenames to analyze
   * @return comprehensive analysis results with suggestions
   */
  public TokenAnalysis analyzeFilenames(List<String> filenames) {
    if (filenames == null || filenames.isEmpty()) {
      return new TokenAnalysis(List.of(), Map.of(), List.of(), Map.of());
    }

    // Tokenize all filenames
    Map<String, List<FilenameToken>> tokenizedFilenames = new HashMap<>();
    for (String filename : filenames) {
      tokenizedFilenames.put(filename, tokenizeFilename(filename));
    }

    // Analyze patterns and suggest token types
    List<TokenSuggestion> suggestions = suggestTokenTypes(tokenizedFilenames);

    // Calculate confidence scores for each token type
    Map<TokenType, Double> confidenceScores = calculateConfidenceScores(suggestions);

    // Apply suggestions to tokens
    applyTokenSuggestions(tokenizedFilenames, suggestions);

    return new TokenAnalysis(filenames, tokenizedFilenames, suggestions, confidenceScores);
  }

  /**
   * Suggests token types based on analysis of tokenized filenames.
   *
   * @param tokenizedFilenames map of filename to tokens
   * @return list of token type suggestions with confidence scores
   */
  public List<TokenSuggestion> suggestTokenTypes(
      Map<String, List<FilenameToken>> tokenizedFilenames) {
    List<TokenSuggestion> suggestions = new ArrayList<>();

    if (tokenizedFilenames.isEmpty()) {
      return suggestions;
    }

    // Analyze token positions and values across all files
    Map<Integer, List<String>> tokensByPosition = groupTokensByPosition(tokenizedFilenames);

    // Suggest token types for each position
    for (Map.Entry<Integer, List<String>> entry : tokensByPosition.entrySet()) {
      int position = entry.getKey();
      List<String> values = entry.getValue();

      suggestions.addAll(analyzeTokenPosition(position, values, tokenizedFilenames.size()));
    }

    return suggestions;
  }

  /** Groups tokens by their position across all filenames. */
  private Map<Integer, List<String>> groupTokensByPosition(
      Map<String, List<FilenameToken>> tokenizedFilenames) {
    Map<Integer, List<String>> tokensByPosition = new HashMap<>();

    for (List<FilenameToken> tokens : tokenizedFilenames.values()) {
      for (FilenameToken token : tokens) {
        tokensByPosition
            .computeIfAbsent(token.getPosition(), k -> new ArrayList<>())
            .add(token.getValue());
      }
    }

    return tokensByPosition;
  }

  /** Analyzes tokens at a specific position to suggest token types. */
  private List<TokenSuggestion> analyzeTokenPosition(
      int position, List<String> values, int totalFiles) {
    List<TokenSuggestion> suggestions = new ArrayList<>();

    // Calculate uniqueness ratio
    Set<String> uniqueValues = new HashSet<>(values);
    double uniquenessRatio = (double) uniqueValues.size() / values.size();

    // Check for extensions (last position)
    if (isLastPosition(position, values, totalFiles)) {
      double extConfidence = analyzeExtensions(uniqueValues);
      if (extConfidence > 0.5) {
        suggestions.add(
            new TokenSuggestion(
                TokenType.EXTENSION,
                "File extension",
                uniqueValues.stream().limit(3).toList(),
                extConfidence));
      }
    }

    // Check for camera/side identifiers
    double cameraConfidence = analyzeCameraSide(uniqueValues);
    if (cameraConfidence > 0.3) {
      suggestions.add(
          new TokenSuggestion(
              TokenType.CAMERA_SIDE,
              "Camera position or image side",
              uniqueValues.stream().limit(3).toList(),
              cameraConfidence));
    }

    // Check for dates
    double dateConfidence = analyzeDates(uniqueValues);
    if (dateConfidence > 0.5) {
      suggestions.add(
          new TokenSuggestion(
              TokenType.DATE,
              "Date or timestamp",
              uniqueValues.stream().limit(3).toList(),
              dateConfidence));
    }

    // Check for indexes/sequences
    double indexConfidence = analyzeIndexes(uniqueValues);
    if (indexConfidence > 0.4) {
      suggestions.add(
          new TokenSuggestion(
              TokenType.INDEX,
              "Numeric index or sequence",
              uniqueValues.stream().limit(3).toList(),
              indexConfidence));
    }

    // Check for group IDs (high uniqueness, not camera/date/index)
    if (uniquenessRatio > 0.7
        && cameraConfidence < 0.3
        && dateConfidence < 0.3
        && indexConfidence < 0.3) {
      suggestions.add(
          new TokenSuggestion(
              TokenType.GROUP_ID,
              "Vehicle group identifier",
              uniqueValues.stream().limit(3).toList(),
              uniquenessRatio * 0.8));
    }

    // Check for prefixes/suffixes (low uniqueness, consistent values)
    if (uniquenessRatio < 0.3) {
      if (position == 0) {
        suggestions.add(
            new TokenSuggestion(
                TokenType.PREFIX,
                "Fixed prefix",
                uniqueValues.stream().limit(3).toList(),
                (1.0 - uniquenessRatio) * 0.7));
      } else {
        suggestions.add(
            new TokenSuggestion(
                TokenType.SUFFIX,
                "Fixed suffix",
                uniqueValues.stream().limit(3).toList(),
                (1.0 - uniquenessRatio) * 0.6));
      }
    }

    return suggestions;
  }

  /** Checks if this is the last position across all files. */
  private boolean isLastPosition(int position, List<String> values, int totalFiles) {
    return values.size() == totalFiles; // All files have a token at this position
  }

  /** Analyzes values for file extension patterns. */
  private double analyzeExtensions(Set<String> values) {
    long extensionCount =
        values.stream().mapToLong(v -> IMAGE_EXTENSIONS.contains(v.toLowerCase()) ? 1 : 0).sum();

    return (double) extensionCount / values.size();
  }

  /** Analyzes values for camera/side identifier patterns. */
  private double analyzeCameraSide(Set<String> values) {
    long cameraCount =
        values.stream()
            .mapToLong(
                v -> {
                  String lower = v.toLowerCase();
                  return ALL_CAMERA_SYNONYMS.contains(lower) ? 1 : 0;
                })
            .sum();

    return (double) cameraCount / values.size();
  }

  /**
   * Gets the specific image role for a camera/side token value.
   *
   * @param tokenValue the token value to check
   * @return the image role, or null if not a camera/side token
   */
  public ImageRole getImageRoleForToken(String tokenValue) {
    String lower = tokenValue.toLowerCase();
    for (Map.Entry<ImageRole, Set<String>> entry : CAMERA_SYNONYMS.entrySet()) {
      if (entry.getValue().contains(lower)) {
        return entry.getKey();
      }
    }
    return null;
  }

  /** Analyzes values for date patterns. */
  private double analyzeDates(Set<String> values) {
    long dateCount =
        values.stream()
            .mapToLong(
                v ->
                    DATE_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(v).matches())
                        ? 1
                        : 0)
            .sum();

    return (double) dateCount / values.size();
  }

  /** Analyzes values for numeric index patterns. */
  private double analyzeIndexes(Set<String> values) {
    long indexCount =
        values.stream().mapToLong(v -> INDEX_PATTERN.matcher(v).matches() ? 1 : 0).sum();

    return (double) indexCount / values.size();
  }

  /** Calculates overall confidence scores for each token type. */
  private Map<TokenType, Double> calculateConfidenceScores(List<TokenSuggestion> suggestions) {
    return suggestions.stream()
        .collect(
            Collectors.groupingBy(
                TokenSuggestion::getType,
                Collectors.averagingDouble(TokenSuggestion::getConfidence)));
  }

  /** Applies token suggestions to update token types and confidence scores. */
  private void applyTokenSuggestions(
      Map<String, List<FilenameToken>> tokenizedFilenames, List<TokenSuggestion> suggestions) {
    // Group suggestions by position (approximate based on examples)
    for (Map.Entry<String, List<FilenameToken>> entry : tokenizedFilenames.entrySet()) {
      List<FilenameToken> tokens = entry.getValue();

      for (FilenameToken token : tokens) {
        // Find best matching suggestion for this token
        TokenSuggestion bestMatch = findBestSuggestionForToken(token, suggestions);
        if (bestMatch != null) {
          token.setSuggestedType(bestMatch.getType());
          token.setConfidence(bestMatch.getConfidence());
        }
      }
    }
  }

  /** Finds the best token suggestion for a specific token. */
  private TokenSuggestion findBestSuggestionForToken(
      FilenameToken token, List<TokenSuggestion> suggestions) {
    return suggestions.stream()
        .filter(
            s -> s.getExamples().contains(token.getValue()) || isTokenMatchingSuggestion(token, s))
        .max(Comparator.comparingDouble(TokenSuggestion::getConfidence))
        .orElse(null);
  }

  /** Checks if a token matches a suggestion based on patterns. */
  private boolean isTokenMatchingSuggestion(FilenameToken token, TokenSuggestion suggestion) {
    String value = token.getValue();

    return switch (suggestion.getType()) {
      case EXTENSION -> IMAGE_EXTENSIONS.contains(value.toLowerCase());
      case CAMERA_SIDE -> ALL_CAMERA_SYNONYMS.contains(value.toLowerCase());
      case DATE -> DATE_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(value).matches());
      case INDEX -> INDEX_PATTERN.matcher(value).matches();
      default -> false;
    };
  }

  /**
   * Checks if a token value represents a front camera/side identifier.
   *
   * @param tokenValue the token value to check
   * @return true if the token represents a front camera/side
   */
  public boolean isFrontToken(String tokenValue) {
    return CAMERA_SYNONYMS.get(ImageRole.FRONT).contains(tokenValue.toLowerCase());
  }
}
