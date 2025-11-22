package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Map;

/**
 * Contains the results of filename analysis including tokenized filenames, pattern suggestions, and
 * confidence scores for different token types.
 */
public class TokenAnalysis {
  private final List<String> filenames;
  private final Map<String, List<FilenameToken>> tokenizedFilenames;
  private final List<TokenSuggestion> suggestions;
  private final Map<TokenType, Double> confidenceScores;

  /**
   * Creates a new token analysis result.
   *
   * @param filenames the original filenames that were analyzed
   * @param tokenizedFilenames map of filename to its tokens
   * @param suggestions list of token type suggestions
   * @param confidenceScores confidence scores for each token type
   */
  public TokenAnalysis(
      List<String> filenames,
      Map<String, List<FilenameToken>> tokenizedFilenames,
      List<TokenSuggestion> suggestions,
      Map<TokenType, Double> confidenceScores) {
    this.filenames = List.copyOf(filenames);
    this.tokenizedFilenames = Map.copyOf(tokenizedFilenames);
    this.suggestions = List.copyOf(suggestions);
    this.confidenceScores = Map.copyOf(confidenceScores);
  }

  /**
   * @return the original filenames that were analyzed
   */
  public List<String> getFilenames() {
    return filenames;
  }

  /**
   * @return map of filename to its tokenized representation
   */
  public Map<String, List<FilenameToken>> getTokenizedFilenames() {
    return tokenizedFilenames;
  }

  /**
   * @return list of token type suggestions with examples and confidence
   */
  public List<TokenSuggestion> getSuggestions() {
    return suggestions;
  }

  /**
   * @return confidence scores for each detected token type
   */
  public Map<TokenType, Double> getConfidenceScores() {
    return confidenceScores;
  }

  /**
   * Gets the tokens for a specific filename.
   *
   * @param filename the filename to get tokens for
   * @return list of tokens for the filename, or empty list if not found
   */
  public List<FilenameToken> getTokensForFilename(String filename) {
    return tokenizedFilenames.getOrDefault(filename, List.of());
  }

  /**
   * Gets the highest confidence token type suggestion.
   *
   * @return the most confident token suggestion, or null if no suggestions
   */
  public TokenSuggestion getBestSuggestion() {
    return suggestions.stream()
        .max((s1, s2) -> Double.compare(s1.getConfidence(), s2.getConfidence()))
        .orElse(null);
  }

  /**
   * Gets suggestions for a specific token type.
   *
   * @param tokenType the token type to get suggestions for
   * @return list of suggestions for the token type
   */
  public List<TokenSuggestion> getSuggestionsForType(TokenType tokenType) {
    return suggestions.stream().filter(s -> s.getType() == tokenType).toList();
  }
}
