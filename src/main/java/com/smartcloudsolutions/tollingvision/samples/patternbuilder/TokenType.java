package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Enumeration of different token types that can be identified in filenames during pattern analysis.
 */
public enum TokenType {
  /** Fixed text at the beginning of filenames */
  PREFIX,

  /** Fixed text at the end of filenames (excluding extension) */
  SUFFIX,

  /** Token that identifies the vehicle group (used for grouping images) */
  GROUP_ID,

  /** Token that identifies camera position or image side (front, rear, overview) */
  CAMERA_SIDE,

  /** Date or timestamp information */
  DATE,

  /** Numeric index or sequence number */
  INDEX,

  /** File extension (.jpg, .png, etc.) */
  EXTENSION,

  /** Token type could not be determined */
  UNKNOWN
}
