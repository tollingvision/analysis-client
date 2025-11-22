package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all pattern settings and generated regex patterns for the filename pattern builder. This
 * class serves as the central configuration object that contains both the visual rule-based
 * configuration and the generated regex patterns.
 */
public class PatternConfiguration {
  private String groupPattern;
  private String frontPattern;
  private String rearPattern;
  private String overviewPattern;
  private List<RoleRule> roleRules;
  private List<FilenameToken> tokens;
  private TokenType groupIdTokenType;
  private List<String> optionalTokenTypes;
  private List<String> optionalCustomTokenNames;

  /** Creates a new pattern configuration with default empty values. */
  public PatternConfiguration() {
    this.groupPattern = "";
    this.frontPattern = "";
    this.rearPattern = "";
    this.overviewPattern = "";
    this.roleRules = new ArrayList<>();
    this.tokens = new ArrayList<>();
    this.groupIdTokenType = null;
    this.optionalTokenTypes = new ArrayList<>();
    this.optionalCustomTokenNames = new ArrayList<>();
  }

  /**
   * @return the regex pattern for extracting group IDs from filenames
   */
  public String getGroupPattern() {
    return groupPattern;
  }

  /**
   * Sets the regex pattern for extracting group IDs from filenames.
   *
   * @param groupPattern the group pattern regex
   */
  public void setGroupPattern(String groupPattern) {
    this.groupPattern = groupPattern;
  }

  /**
   * @return the regex pattern for identifying front view images
   */
  public String getFrontPattern() {
    return frontPattern;
  }

  /**
   * Sets the regex pattern for identifying front view images.
   *
   * @param frontPattern the front pattern regex
   */
  public void setFrontPattern(String frontPattern) {
    this.frontPattern = frontPattern;
  }

  /**
   * @return the regex pattern for identifying rear view images
   */
  public String getRearPattern() {
    return rearPattern;
  }

  /**
   * Sets the regex pattern for identifying rear view images.
   *
   * @param rearPattern the rear pattern regex
   */
  public void setRearPattern(String rearPattern) {
    this.rearPattern = rearPattern;
  }

  /**
   * @return the regex pattern for identifying overview images
   */
  public String getOverviewPattern() {
    return overviewPattern;
  }

  /**
   * Sets the regex pattern for identifying overview images.
   *
   * @param overviewPattern the overview pattern regex
   */
  public void setOverviewPattern(String overviewPattern) {
    this.overviewPattern = overviewPattern;
  }

  /**
   * @return the list of role rules for image classification
   */
  public List<RoleRule> getRoleRules() {
    return roleRules;
  }

  /**
   * Sets the list of role rules for image classification.
   *
   * @param roleRules the role rules list
   */
  public void setRoleRules(List<RoleRule> roleRules) {
    this.roleRules = roleRules != null ? roleRules : new ArrayList<>();
  }

  /**
   * Adds a role rule to the configuration.
   *
   * @param rule the role rule to add
   */
  public void addRoleRule(RoleRule rule) {
    if (rule != null) {
      this.roleRules.add(rule);
    }
  }

  /**
   * Removes a role rule from the configuration.
   *
   * @param rule the role rule to remove
   * @return true if the rule was removed
   */
  public boolean removeRoleRule(RoleRule rule) {
    return this.roleRules.remove(rule);
  }

  /**
   * @return the list of filename tokens detected during analysis
   */
  public List<FilenameToken> getTokens() {
    return tokens;
  }

  /**
   * Sets the list of filename tokens detected during analysis.
   *
   * @param tokens the tokens list
   */
  public void setTokens(List<FilenameToken> tokens) {
    this.tokens = tokens != null ? tokens : new ArrayList<>();
  }

  /**
   * @return the token type selected as the group ID, or null if none selected
   */
  public TokenType getGroupIdToken() {
    return groupIdTokenType;
  }

  /**
   * Sets the token type to use as the group ID.
   *
   * @param groupIdTokenType the group ID token type
   */
  public void setGroupIdToken(TokenType groupIdTokenType) {
    this.groupIdTokenType = groupIdTokenType;
  }

  /**
   * Checks if the configuration has all required patterns set.
   *
   * @return true if group pattern and at least one role pattern are set
   */
  /**
   * Checks if this configuration is valid for pattern matching. Requires at least a valid group
   * pattern and one role pattern.
   *
   * @return true if the configuration has valid patterns for matching files
   */
  public boolean isValid() {
    return groupPattern != null
        && !groupPattern.trim().isEmpty()
        && (hasValidPattern(frontPattern)
            || hasValidPattern(rearPattern)
            || hasValidPattern(overviewPattern));
  }

  /**
   * Checks if this configuration has valid builder state. This is used for preset validation where
   * we only store the builder state (tokens, rules, etc.) without the generated patterns. A preset
   * is valid if it has at least one token OR at least one role rule OR a group ID token set.
   *
   * @return true if the configuration has valid builder state
   */
  public boolean hasValidBuilderState() {
    // Valid if has builder state (tokens/rules/groupId) OR has at least one pattern
    // defined
    boolean hasBuilderState =
        (tokens != null && !tokens.isEmpty())
            || (roleRules != null && !roleRules.isEmpty())
            || groupIdTokenType != null;

    boolean hasPatterns =
        hasValidPattern(groupPattern)
            || hasValidPattern(frontPattern)
            || hasValidPattern(rearPattern)
            || hasValidPattern(overviewPattern);

    return hasBuilderState || hasPatterns;
  }

  /**
   * Checks if a pattern string is valid (not null or empty).
   *
   * @param pattern the pattern to check
   * @return true if the pattern is valid
   */
  private boolean hasValidPattern(String pattern) {
    return pattern != null && !pattern.trim().isEmpty();
  }

  /**
   * @return the list of optional token type names
   */
  public List<String> getOptionalTokenTypes() {
    return optionalTokenTypes;
  }

  /**
   * Sets the list of optional token type names.
   *
   * @param optionalTokenTypes the optional token types list
   */
  public void setOptionalTokenTypes(List<String> optionalTokenTypes) {
    this.optionalTokenTypes = optionalTokenTypes != null ? optionalTokenTypes : new ArrayList<>();
  }

  /**
   * @return the list of optional custom token names
   */
  public List<String> getOptionalCustomTokenNames() {
    return optionalCustomTokenNames;
  }

  /**
   * Sets the list of optional custom token names.
   *
   * @param optionalCustomTokenNames the optional custom token names list
   */
  public void setOptionalCustomTokenNames(List<String> optionalCustomTokenNames) {
    this.optionalCustomTokenNames =
        optionalCustomTokenNames != null ? optionalCustomTokenNames : new ArrayList<>();
  }

  /**
   * Creates a copy of this configuration.
   *
   * @return a new PatternConfiguration with the same settings
   */
  public PatternConfiguration copy() {
    PatternConfiguration copy = new PatternConfiguration();
    copy.setGroupPattern(this.groupPattern);
    copy.setFrontPattern(this.frontPattern);
    copy.setRearPattern(this.rearPattern);
    copy.setOverviewPattern(this.overviewPattern);
    copy.setRoleRules(new ArrayList<>(this.roleRules));
    copy.setTokens(new ArrayList<>(this.tokens));
    copy.setGroupIdToken(this.groupIdTokenType);
    copy.setOptionalTokenTypes(new ArrayList<>(this.optionalTokenTypes));
    copy.setOptionalCustomTokenNames(new ArrayList<>(this.optionalCustomTokenNames));
    return copy;
  }

  @Override
  public String toString() {
    return String.format(
        "PatternConfiguration{groupPattern='%s', frontPattern='%s', "
            + "rearPattern='%s', overviewPattern='%s', roleRules=%d, tokens=%d, "
            + "groupIdToken=%s}",
        groupPattern,
        frontPattern,
        rearPattern,
        overviewPattern,
        roleRules.size(),
        tokens.size(),
        groupIdTokenType != null ? groupIdTokenType.name() : "null");
  }
}
