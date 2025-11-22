package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for storing Pattern Builder specific settings. This class holds the visual
 * rule-based configuration that can be persisted and restored, allowing users to maintain their
 * pattern building preferences.
 */
public class PatternBuilderConfig {
  private List<RoleRule> roleRules;
  private List<FilenameToken> tokens;
  private TokenType groupIdTokenType;
  private String lastUsedDirectory;
  private boolean autoGeneratePatterns;
  private List<String> optionalTokenTypes;
  private List<String> optionalCustomTokenNames;

  /** Creates a new PatternBuilderConfig with default values. */
  public PatternBuilderConfig() {
    this.roleRules = new ArrayList<>();
    this.tokens = new ArrayList<>();
    this.groupIdTokenType = null;
    this.lastUsedDirectory = "";
    this.autoGeneratePatterns = true;
    this.optionalTokenTypes = new ArrayList<>();
    this.optionalCustomTokenNames = new ArrayList<>();
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
   * @return the last directory used for file analysis
   */
  public String getLastUsedDirectory() {
    return lastUsedDirectory;
  }

  /**
   * Sets the last directory used for file analysis.
   *
   * @param lastUsedDirectory the directory path
   */
  public void setLastUsedDirectory(String lastUsedDirectory) {
    this.lastUsedDirectory = lastUsedDirectory != null ? lastUsedDirectory : "";
  }

  /**
   * @return true if patterns should be automatically generated from rules
   */
  public boolean isAutoGeneratePatterns() {
    return autoGeneratePatterns;
  }

  /**
   * Sets whether patterns should be automatically generated from rules.
   *
   * @param autoGeneratePatterns true to auto-generate patterns
   */
  public void setAutoGeneratePatterns(boolean autoGeneratePatterns) {
    this.autoGeneratePatterns = autoGeneratePatterns;
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
   * Creates a PatternConfiguration from this builder config.
   *
   * @return a new PatternConfiguration with current settings
   */
  public PatternConfiguration toPatternConfiguration() {
    PatternConfiguration config = new PatternConfiguration();
    config.setRoleRules(new ArrayList<>(this.roleRules));
    config.setTokens(new ArrayList<>(this.tokens));
    config.setGroupIdToken(this.groupIdTokenType);
    config.setOptionalTokenTypes(new ArrayList<>(this.optionalTokenTypes));
    config.setOptionalCustomTokenNames(new ArrayList<>(this.optionalCustomTokenNames));
    return config;
  }

  /**
   * Updates this builder config from a PatternConfiguration.
   *
   * @param config the pattern configuration to copy from
   */
  public void fromPatternConfiguration(PatternConfiguration config) {
    if (config != null) {
      this.roleRules = new ArrayList<>(config.getRoleRules());
      this.tokens = new ArrayList<>(config.getTokens());
      this.groupIdTokenType = config.getGroupIdToken();
      this.optionalTokenTypes =
          config.getOptionalTokenTypes() != null
              ? new ArrayList<>(config.getOptionalTokenTypes())
              : new ArrayList<>();
      this.optionalCustomTokenNames =
          config.getOptionalCustomTokenNames() != null
              ? new ArrayList<>(config.getOptionalCustomTokenNames())
              : new ArrayList<>();
    }
  }

  /**
   * Creates a copy of this configuration.
   *
   * @return a new PatternBuilderConfig with the same settings
   */
  public PatternBuilderConfig copy() {
    PatternBuilderConfig copy = new PatternBuilderConfig();
    copy.setRoleRules(new ArrayList<>(this.roleRules));
    copy.setTokens(new ArrayList<>(this.tokens));
    copy.setGroupIdToken(this.groupIdTokenType);
    copy.setLastUsedDirectory(this.lastUsedDirectory);
    copy.setAutoGeneratePatterns(this.autoGeneratePatterns);
    copy.setOptionalTokenTypes(new ArrayList<>(this.optionalTokenTypes));
    copy.setOptionalCustomTokenNames(new ArrayList<>(this.optionalCustomTokenNames));
    return copy;
  }

  @Override
  public String toString() {
    return String.format(
        "PatternBuilderConfig{roleRules=%d, tokens=%d, "
            + "groupIdToken=%s, lastUsedDirectory='%s', autoGeneratePatterns=%s}",
        roleRules.size(),
        tokens.size(),
        groupIdTokenType != null ? groupIdTokenType.name() : "null",
        lastUsedDirectory,
        autoGeneratePatterns);
  }
}
