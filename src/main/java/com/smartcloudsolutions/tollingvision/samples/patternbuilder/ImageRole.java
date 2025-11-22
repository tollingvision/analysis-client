package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Enumeration of image roles with precedence values for classification. Lower precedence values are
 * processed first, allowing overview rules to exclude files from front/rear consideration.
 */
public enum ImageRole {
  /** Overview/scene image - processed first (precedence 1) */
  OVERVIEW(1),

  /** Front view image - processed second (precedence 2) */
  FRONT(2),

  /** Rear view image - processed last (precedence 3) */
  REAR(3);

  private final int precedence;

  /**
   * Creates an image role with the specified precedence value.
   *
   * @param precedence the precedence value (lower values processed first)
   */
  ImageRole(int precedence) {
    this.precedence = precedence;
  }

  /**
   * @return the precedence value for this role (lower values processed first)
   */
  public int getPrecedence() {
    return precedence;
  }

  /**
   * Gets the image role with the lowest precedence value (processed first).
   *
   * @return the highest priority image role
   */
  public static ImageRole getHighestPriority() {
    return OVERVIEW;
  }

  /**
   * Gets the image role with the highest precedence value (processed last).
   *
   * @return the lowest priority image role
   */
  public static ImageRole getLowestPriority() {
    return REAR;
  }
}
