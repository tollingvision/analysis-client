package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a saved preset configuration with metadata. Contains the pattern configuration along
 * with name, description, and timestamps for managing saved presets.
 */
public class PresetConfiguration {
  private String name;
  private String description;
  private PatternConfiguration patternConfig;
  private LocalDateTime created;
  private LocalDateTime lastUsed;

  /** Creates a new preset configuration. */
  public PresetConfiguration() {
    this.created = LocalDateTime.now();
    this.lastUsed = LocalDateTime.now();
  }

  /**
   * Creates a new preset configuration with the specified parameters.
   *
   * @param name the preset name
   * @param description the preset description
   * @param patternConfig the pattern configuration
   */
  public PresetConfiguration(String name, String description, PatternConfiguration patternConfig) {
    this();
    this.name = name;
    this.description = description;
    this.patternConfig = patternConfig != null ? patternConfig.copy() : new PatternConfiguration();
  }

  /**
   * @return the preset name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the preset name.
   *
   * @param name the preset name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the preset description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the preset description.
   *
   * @param description the preset description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the pattern configuration
   */
  public PatternConfiguration getPatternConfig() {
    return patternConfig;
  }

  /**
   * Sets the pattern configuration.
   *
   * @param patternConfig the pattern configuration
   */
  public void setPatternConfig(PatternConfiguration patternConfig) {
    this.patternConfig = patternConfig;
  }

  /**
   * @return the creation timestamp
   */
  public LocalDateTime getCreated() {
    return created;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param created the creation timestamp
   */
  public void setCreated(LocalDateTime created) {
    this.created = created;
  }

  /**
   * @return the last used timestamp
   */
  public LocalDateTime getLastUsed() {
    return lastUsed;
  }

  /**
   * Sets the last used timestamp.
   *
   * @param lastUsed the last used timestamp
   */
  public void setLastUsed(LocalDateTime lastUsed) {
    this.lastUsed = lastUsed;
  }

  /** Updates the last used timestamp to now. */
  public void updateLastUsed() {
    this.lastUsed = LocalDateTime.now();
  }

  /**
   * Creates a copy of this preset configuration.
   *
   * @return a new PresetConfiguration with the same settings
   */
  public PresetConfiguration copy() {
    PresetConfiguration copy = new PresetConfiguration();
    copy.setName(this.name);
    copy.setDescription(this.description);
    copy.setPatternConfig(this.patternConfig != null ? this.patternConfig.copy() : null);
    copy.setCreated(this.created);
    copy.setLastUsed(this.lastUsed);
    return copy;
  }

  /**
   * Checks if this preset has a valid configuration.
   *
   * @return true if the preset has a valid pattern configuration
   */
  /**
   * Checks if this preset is valid. A preset is valid if it has a non-empty name and a valid
   * builder state configuration (tokens, rules, etc.). Note that presets don't require generated
   * patterns - those are stored separately in config.json.
   *
   * @return true if the preset is valid
   */
  public boolean isValid() {
    return name != null
        && !name.trim().isEmpty()
        && patternConfig != null
        && patternConfig.hasValidBuilderState();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PresetConfiguration that = (PresetConfiguration) obj;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return String.format(
        "PresetConfiguration{name='%s', description='%s', created=%s, lastUsed=%s}",
        name, description, created, lastUsed);
  }
}
