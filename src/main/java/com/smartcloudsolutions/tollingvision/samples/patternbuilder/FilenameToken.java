package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Represents a single token extracted from a filename during pattern analysis. Contains the token
 * value, its position in the filename, suggested type, and confidence level for the type
 * suggestion.
 */
public class FilenameToken {
  private final String value;
  private final int position;
  private TokenType suggestedType;
  private double confidence;

  /**
   * Creates a new filename token.
   *
   * @param value the string value of the token
   * @param position the position of this token in the filename (0-based)
   */
  public FilenameToken(String value, int position) {
    this.value = value;
    this.position = position;
    this.suggestedType = TokenType.UNKNOWN;
    this.confidence = 0.0;
  }

  /**
   * Creates a new filename token with suggested type and confidence.
   *
   * @param value the string value of the token
   * @param position the position of this token in the filename (0-based)
   * @param suggestedType the suggested token type
   * @param confidence confidence level for the suggestion (0.0 to 1.0)
   */
  public FilenameToken(String value, int position, TokenType suggestedType, double confidence) {
    this.value = value;
    this.position = position;
    this.suggestedType = suggestedType;
    this.confidence = Math.max(0.0, Math.min(1.0, confidence));
  }

  /**
   * @return the string value of this token
   */
  public String getValue() {
    return value;
  }

  /**
   * @return the position of this token in the filename (0-based)
   */
  public int getPosition() {
    return position;
  }

  /**
   * @return the suggested token type
   */
  public TokenType getSuggestedType() {
    return suggestedType;
  }

  /**
   * Sets the suggested token type.
   *
   * @param suggestedType the suggested token type
   */
  public void setSuggestedType(TokenType suggestedType) {
    this.suggestedType = suggestedType;
  }

  /**
   * @return the confidence level for the type suggestion (0.0 to 1.0)
   */
  public double getConfidence() {
    return confidence;
  }

  /**
   * Sets the confidence level for the type suggestion.
   *
   * @param confidence confidence level (will be clamped to 0.0-1.0 range)
   */
  public void setConfidence(double confidence) {
    this.confidence = Math.max(0.0, Math.min(1.0, confidence));
  }

  @Override
  public String toString() {
    return String.format(
        "FilenameToken{value='%s', position=%d, type=%s, confidence=%.2f}",
        value, position, suggestedType, confidence);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    FilenameToken that = (FilenameToken) obj;
    return position == that.position
        && Double.compare(that.confidence, confidence) == 0
        && value.equals(that.value)
        && suggestedType == that.suggestedType;
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + position;
    result = 31 * result + suggestedType.hashCode();
    result = 31 * result + Double.hashCode(confidence);
    return result;
  }
}
