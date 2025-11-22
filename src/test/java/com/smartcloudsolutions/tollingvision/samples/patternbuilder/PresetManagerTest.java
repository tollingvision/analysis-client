package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for PresetManager. */
class PresetManagerTest {

  @TempDir Path tempDir;

  private PresetManager presetManager;

  @BeforeEach
  void setUp() {
    presetManager = new PresetManager(tempDir);
  }

  @Test
  void testSaveAndLoadPreset() throws IOException {
    // Create a test configuration
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)_\\d+");
    config.setFrontPattern(".*front.*");
    config.setRearPattern(".*rear.*");

    // Create a preset
    PresetConfiguration preset = new PresetConfiguration("Test Preset", "Test description", config);

    // Save the preset
    presetManager.savePreset(preset);

    // Load the preset
    PresetConfiguration loaded = presetManager.loadPreset("Test Preset");

    assertNotNull(loaded);
    assertEquals("Test Preset", loaded.getName());
    assertEquals("Test description", loaded.getDescription());
    assertNotNull(loaded.getPatternConfig());
    assertEquals("(.+)_\\d+", loaded.getPatternConfig().getGroupPattern());
    assertEquals(".*front.*", loaded.getPatternConfig().getFrontPattern());
    assertEquals(".*rear.*", loaded.getPatternConfig().getRearPattern());
  }

  @Test
  void testDeletePreset() throws IOException {
    // Create and save a preset
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");
    config.setFrontPattern(".*front.*"); // Add role pattern to make it valid
    PresetConfiguration preset = new PresetConfiguration("Delete Me", "To be deleted", config);
    presetManager.savePreset(preset);

    // Verify it exists
    assertNotNull(presetManager.loadPreset("Delete Me"));

    // Delete it
    boolean deleted = presetManager.deletePreset("Delete Me");
    assertTrue(deleted);

    // Verify it's gone
    assertNull(presetManager.loadPreset("Delete Me"));
  }

  @Test
  void testCannotDeleteDefaultPreset() throws IOException {
    // Try to delete the default preset
    boolean deleted = presetManager.deletePreset("Default");
    assertFalse(deleted);

    // Verify default preset still exists
    assertNotNull(presetManager.getDefaultPreset());
  }

  @Test
  void testRenamePreset() throws IOException {
    // Create and save a preset
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");
    config.setFrontPattern(".*front.*"); // Add role pattern to make it valid
    PresetConfiguration preset = new PresetConfiguration("Old Name", "Description", config);
    presetManager.savePreset(preset);

    // Rename it
    boolean renamed = presetManager.renamePreset("Old Name", "New Name");
    assertTrue(renamed);

    // Verify old name is gone and new name exists
    assertNull(presetManager.loadPreset("Old Name"));
    assertNotNull(presetManager.loadPreset("New Name"));
  }

  @Test
  void testDuplicatePreset() throws IOException {
    // Create and save a preset
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)");
    config.setFrontPattern(".*front.*");
    PresetConfiguration preset =
        new PresetConfiguration("Original", "Original description", config);
    presetManager.savePreset(preset);

    // Duplicate it
    boolean duplicated = presetManager.duplicatePreset("Original", "Copy");
    assertTrue(duplicated);

    // Verify both exist
    PresetConfiguration original = presetManager.loadPreset("Original");
    PresetConfiguration copy = presetManager.loadPreset("Copy");

    assertNotNull(original);
    assertNotNull(copy);
    assertEquals("Original", original.getName());
    assertEquals("Copy", copy.getName());
    assertEquals(
        original.getPatternConfig().getGroupPattern(), copy.getPatternConfig().getGroupPattern());
    assertEquals(
        original.getPatternConfig().getFrontPattern(), copy.getPatternConfig().getFrontPattern());
  }

  @Test
  void testListPresets() throws IOException {
    // Initially should have at least the default preset
    assertTrue(presetManager.listPresets().size() >= 1);

    // Add some presets
    PatternConfiguration config1 = new PatternConfiguration();
    config1.setGroupPattern("(.+)_1");
    config1.setFrontPattern(".*front.*"); // Add role pattern to make it valid
    presetManager.savePreset(new PresetConfiguration("Preset 1", "First preset", config1));

    PatternConfiguration config2 = new PatternConfiguration();
    config2.setGroupPattern("(.+)_2");
    config2.setRearPattern(".*rear.*"); // Add role pattern to make it valid
    presetManager.savePreset(new PresetConfiguration("Preset 2", "Second preset", config2));

    // Should now have at least 3 presets (default + 2 added)
    assertTrue(presetManager.listPresets().size() >= 3);

    // Verify our presets are in the list
    boolean found1 =
        presetManager.listPresets().stream().anyMatch(p -> "Preset 1".equals(p.getName()));
    boolean found2 =
        presetManager.listPresets().stream().anyMatch(p -> "Preset 2".equals(p.getName()));

    assertTrue(found1);
    assertTrue(found2);
  }

  @Test
  void testDefaultPresetExists() {
    PresetConfiguration defaultPreset = presetManager.getDefaultPreset();
    assertNotNull(defaultPreset);
    assertEquals("Default", defaultPreset.getName());
    assertNotNull(defaultPreset.getPatternConfig());
  }

  @Test
  void testInvalidPresetRejected() {
    // Create an invalid preset (no name)
    PresetConfiguration invalidPreset = new PresetConfiguration();
    invalidPreset.setName(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          presetManager.savePreset(invalidPreset);
        });

    // Create another invalid preset (empty name)
    PresetConfiguration emptyNamePreset = new PresetConfiguration();
    emptyNamePreset.setName("");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          presetManager.savePreset(emptyNamePreset);
        });
  }

  @Test
  void testUpdateExistingPreset() throws IOException {
    // Create and save a preset
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)_old");
    config.setFrontPattern(".*front.*"); // Add role pattern to make it valid
    PresetConfiguration preset =
        new PresetConfiguration("Update Test", "Original description", config);
    presetManager.savePreset(preset);

    // Update the preset
    PatternConfiguration newConfig = new PatternConfiguration();
    newConfig.setGroupPattern("(.+)_new");
    newConfig.setFrontPattern(".*front.*"); // Add role pattern to make it valid
    PresetConfiguration updatedPreset =
        new PresetConfiguration("Update Test", "Updated description", newConfig);
    presetManager.savePreset(updatedPreset);

    // Verify the preset was updated
    PresetConfiguration loaded = presetManager.loadPreset("Update Test");
    assertNotNull(loaded);
    assertEquals("Updated description", loaded.getDescription());
    assertEquals("(.+)_new", loaded.getPatternConfig().getGroupPattern());
  }

  @Test
  void testExportAndImportPreset() throws IOException {
    // Create a test configuration
    PatternConfiguration config = new PatternConfiguration();
    config.setGroupPattern("(.+)_vehicle");
    config.setFrontPattern(".*front.*");
    config.setRearPattern(".*rear.*");
    config.setOverviewPattern(".*overview.*");

    // Create and save a preset
    PresetConfiguration original =
        new PresetConfiguration("Export Test", "Test export/import", config);
    presetManager.savePreset(original);

    // Export to file
    Path exportFile = tempDir.resolve("export-test.json");
    presetManager.exportPreset(original, exportFile);

    // Verify file was created
    assertTrue(Files.exists(exportFile));

    // Import the preset
    PresetConfiguration imported = presetManager.importPreset(exportFile);

    // Verify imported preset matches original
    assertNotNull(imported);
    assertEquals(original.getName(), imported.getName());
    assertEquals(original.getDescription(), imported.getDescription());

    PatternConfiguration importedConfig = imported.getPatternConfig();
    assertNotNull(importedConfig);
    assertEquals(config.getGroupPattern(), importedConfig.getGroupPattern());
    assertEquals(config.getFrontPattern(), importedConfig.getFrontPattern());
    assertEquals(config.getRearPattern(), importedConfig.getRearPattern());
    assertEquals(config.getOverviewPattern(), importedConfig.getOverviewPattern());
  }
}
