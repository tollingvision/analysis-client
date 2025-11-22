package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Unit tests for PresetConfiguration. */
class PresetConfigurationTest {

  @Test
  void testDefaultConstructor() {
    PresetConfiguration preset = new PresetConfiguration();

    assertNull(preset.getName());
    assertNull(preset.getDescription());
    assertNull(preset.getPatternConfig());
    assertNotNull(preset.getCreated());
    assertNotNull(preset.getLastUsed());
  }

  @Test
  void testParameterizedConstructor() {
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");

    PresetConfiguration preset = new PresetConfiguration("Test", "Description", config);

    assertEquals("Test", preset.getName());
    assertEquals("Description", preset.getDescription());
    assertNotNull(preset.getPatternConfig());
    assertEquals("(.+)", preset.getPatternConfig().getGroupPattern());
    assertNotNull(preset.getCreated());
    assertNotNull(preset.getLastUsed());
  }

  @Test
  void testCopyConstructor() {
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");
    config.setFrontPattern(".*front.*");

    PresetConfiguration original =
        new PresetConfiguration("Original", "Original description", config);
    PresetConfiguration copy = original.copy();

    assertEquals(original.getName(), copy.getName());
    assertEquals(original.getDescription(), copy.getDescription());
    assertEquals(original.getCreated(), copy.getCreated());
    assertEquals(original.getLastUsed(), copy.getLastUsed());

    // Verify pattern config is copied
    assertNotNull(copy.getPatternConfig());
    assertEquals(
        original.getPatternConfig().getGroupPattern(), copy.getPatternConfig().getGroupPattern());
    assertEquals(
        original.getPatternConfig().getFrontPattern(), copy.getPatternConfig().getFrontPattern());

    // Verify it's a deep copy (different objects)
    assertNotSame(original.getPatternConfig(), copy.getPatternConfig());
  }

  @Test
  void testUpdateLastUsed() {
    PresetConfiguration preset = new PresetConfiguration();
    LocalDateTime originalLastUsed = preset.getLastUsed();

    // Wait a bit to ensure time difference
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    preset.updateLastUsed();
    LocalDateTime newLastUsed = preset.getLastUsed();

    assertTrue(newLastUsed.isAfter(originalLastUsed));
  }

  @Test
  void testIsValid() {
    PresetConfiguration preset = new PresetConfiguration();

    // Invalid - no name
    assertFalse(preset.isValid());

    // Invalid - empty name
    preset.setName("");
    assertFalse(preset.isValid());

    // Invalid - name but no pattern config
    preset.setName("Test");
    assertFalse(preset.isValid());

    // Invalid - name and pattern config but invalid config
    PatternConfiguration invalidConfig = new PatternConfiguration();
    preset.setPatternConfig(invalidConfig);
    assertFalse(preset.isValid());

    // Valid - name and valid pattern config
    PatternConfiguration validConfig = new PatternConfiguration();
    validConfig.setGroupPattern("(.+)");
    validConfig.setFrontPattern(".*front.*");
    preset.setPatternConfig(validConfig);
    assertTrue(preset.isValid());
  }

  @Test
  void testEqualsAndHashCode() {
    PresetConfiguration preset1 = new PresetConfiguration();
    preset1.setName("Test");

    PresetConfiguration preset2 = new PresetConfiguration();
    preset2.setName("Test");

    PresetConfiguration preset3 = new PresetConfiguration();
    preset3.setName("Different");

    // Test equals
    assertEquals(preset1, preset2);
    assertNotEquals(preset1, preset3);
    assertNotEquals(preset1, null);
    assertNotEquals(preset1, "not a preset");

    // Test hashCode
    assertEquals(preset1.hashCode(), preset2.hashCode());
    assertNotEquals(preset1.hashCode(), preset3.hashCode());
  }

  @Test
  void testToString() {
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");

    PresetConfiguration preset = new PresetConfiguration("Test", "Description", config);
    String toString = preset.toString();

    assertTrue(toString.contains("Test"));
    assertTrue(toString.contains("Description"));
    assertNotNull(toString);
  }

  @Test
  void testSettersAndGetters() {
    PresetConfiguration preset = new PresetConfiguration();
    LocalDateTime now = LocalDateTime.now();

    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");

    preset.setName("Test Name");
    preset.setDescription("Test Description");
    preset.setPatternConfig(config);
    preset.setCreated(now);
    preset.setLastUsed(now);

    assertEquals("Test Name", preset.getName());
    assertEquals("Test Description", preset.getDescription());
    assertEquals(config, preset.getPatternConfig());
    assertEquals(now, preset.getCreated());
    assertEquals(now, preset.getLastUsed());
  }
}
