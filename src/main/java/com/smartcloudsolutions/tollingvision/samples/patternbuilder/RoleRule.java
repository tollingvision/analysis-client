package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

/**
 * Represents a rule for classifying filenames into image roles. Rules are applied in priority order
 * to determine whether a filename matches a specific image role (front, rear, overview).
 */
public class RoleRule {
  private ImageRole targetRole;
  private RuleType ruleType;
  private String ruleValue;
  private boolean caseSensitive;
  private int priority;

  /** Creates a new role rule with default settings. */
  public RoleRule() {
    this.targetRole = ImageRole.FRONT;
    this.ruleType = RuleType.CONTAINS;
    this.ruleValue = "";
    this.caseSensitive = false;
    this.priority = 0;
  }

  /**
   * Creates a new role rule with specified parameters.
   *
   * @param targetRole the image role this rule targets
   * @param ruleType the type of matching to perform
   * @param ruleValue the value to match against
   * @param caseSensitive whether matching should be case-sensitive
   * @param priority the priority of this rule (lower values processed first)
   */
  public RoleRule(
      ImageRole targetRole,
      RuleType ruleType,
      String ruleValue,
      boolean caseSensitive,
      int priority) {
    this.targetRole = targetRole;
    this.ruleType = ruleType;
    this.ruleValue = ruleValue;
    this.caseSensitive = caseSensitive;
    this.priority = priority;
  }

  /**
   * @return the target image role for this rule
   */
  public ImageRole getTargetRole() {
    return targetRole;
  }

  /**
   * Sets the target image role for this rule.
   *
   * @param targetRole the target image role
   */
  public void setTargetRole(ImageRole targetRole) {
    this.targetRole = targetRole;
  }

  /**
   * @return the rule type (how matching is performed)
   */
  public RuleType getRuleType() {
    return ruleType;
  }

  /**
   * Sets the rule type.
   *
   * @param ruleType the rule type
   */
  public void setRuleType(RuleType ruleType) {
    this.ruleType = ruleType;
  }

  /**
   * @return the value to match against
   */
  public String getRuleValue() {
    return ruleValue;
  }

  /**
   * Sets the value to match against.
   *
   * @param ruleValue the rule value
   */
  public void setRuleValue(String ruleValue) {
    this.ruleValue = ruleValue;
  }

  /**
   * @return true if matching should be case-sensitive
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * Sets whether matching should be case-sensitive.
   *
   * @param caseSensitive true for case-sensitive matching
   */
  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * @return the priority of this rule (lower values processed first)
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Sets the priority of this rule.
   *
   * @param priority the priority (lower values processed first)
   */
  public void setPriority(int priority) {
    this.priority = priority;
  }

  @Override
  public String toString() {
    return String.format(
        "RoleRule{role=%s, type=%s, value='%s', caseSensitive=%s, priority=%d}",
        targetRole, ruleType, ruleValue, caseSensitive, priority);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    RoleRule roleRule = (RoleRule) obj;
    return caseSensitive == roleRule.caseSensitive
        && priority == roleRule.priority
        && targetRole == roleRule.targetRole
        && ruleType == roleRule.ruleType
        && ruleValue.equals(roleRule.ruleValue);
  }

  @Override
  public int hashCode() {
    int result = targetRole.hashCode();
    result = 31 * result + ruleType.hashCode();
    result = 31 * result + ruleValue.hashCode();
    result = 31 * result + Boolean.hashCode(caseSensitive);
    result = 31 * result + priority;
    return result;
  }
}
