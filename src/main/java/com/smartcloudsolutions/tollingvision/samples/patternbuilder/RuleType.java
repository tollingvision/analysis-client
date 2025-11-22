package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/** Enumeration of rule types for matching filename tokens to image roles. */
public enum RuleType {
  /** Exact string match */
  EQUALS,

  /** Substring match (token contains the rule value) */
  CONTAINS,

  /** Prefix match (token starts with the rule value) */
  STARTS_WITH,

  /** Suffix match (token ends with the rule value) */
  ENDS_WITH,

  /** Custom regex pattern override for advanced users */
  REGEX_OVERRIDE
}
