package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;

/** Represents a suggestion for a token type with examples and confidence level. */
public class TokenSuggestion {
  private final TokenType type;
  private final String description;
  private final List<String> examples;
  private final double confidence;

  /**
   * Creates a new token suggestion.
   *
   * @param type the suggested token type
   * @param description human-readable description of the token type
   * @param examples list of example values for this token type
   * @param confidence confidence level (0.0 to 1.0)
   */
  public TokenSuggestion(
      TokenType type, String description, List<String> examples, double confidence) {
    this.type = type;
    this.description = description;
    this.examples = List.copyOf(examples);
    this.confidence = Math.max(0.0, Math.min(1.0, confidence));
  }

  /**
   * @return the suggested token type
   */
  public TokenType getType() {
    return type;
  }

  /**
   * @return human-readable description of the token type
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return list of example values for this token type
   */
  public List<String> getExamples() {
    return examples;
  }

  /**
   * @return confidence level for this suggestion (0.0 to 1.0)
   */
  public double getConfidence() {
    return confidence;
  }

  @Override
  public String toString() {
    return String.format(
        "TokenSuggestion{type=%s, description='%s', examples=%s, confidence=%.2f}",
        type, description, examples, confidence);
  }
}
