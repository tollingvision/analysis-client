package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import com.google.protobuf.util.JsonFormat;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages preset configurations for the pattern builder. Provides save, load, delete, and list
 * operations for pattern presets, along with import/export functionality for JSON preset files.
 */
public class PresetManager {
  private static final Logger LOGGER = Logger.getLogger(PresetManager.class.getName());

  private static final String PRESETS_DIR = ".tollingvision-client";
  private static final String PRESETS_FILE = "pattern-presets.json";
  private static final String DEFAULT_PRESET_NAME = "Default";

  private final Path presetsFile;
  private final ObservableList<PresetConfiguration> presets;
  private PresetConfiguration defaultPreset;

  /** Creates a new PresetManager with the default presets directory. */
  public PresetManager() {
    this(getDefaultPresetsDirectory());
  }

  /**
   * Creates a new PresetManager with the specified presets directory.
   *
   * @param presetsDirectory the directory to store presets
   */
  public PresetManager(Path presetsDirectory) {
    this.presetsFile = presetsDirectory.resolve(PRESETS_FILE);
    this.presets = FXCollections.observableArrayList();

    // Ensure presets directory exists
    try {
      Files.createDirectories(presetsDirectory);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to create presets directory: " + presetsDirectory, e);
    }

    // Load existing presets
    loadPresets();

    // Ensure default preset exists
    ensureDefaultPreset();
  }

  /**
   * Gets the default presets directory in the user's home directory.
   *
   * @return the default presets directory path
   */
  private static Path getDefaultPresetsDirectory() {
    String userHome = System.getProperty("user.home");
    return Paths.get(userHome, PRESETS_DIR);
  }

  /**
   * Saves a preset configuration.
   *
   * @param preset the preset to save
   * @throws IllegalArgumentException if the preset is invalid
   * @throws IOException if saving fails
   */
  public void savePreset(PresetConfiguration preset) throws IOException {
    if (preset == null) {
      throw new IllegalArgumentException("Preset cannot be null");
    }

    if (!preset.isValid()) {
      throw new IllegalArgumentException("Preset configuration is invalid");
    }

    // Check for duplicate names
    Optional<PresetConfiguration> existing =
        presets.stream().filter(p -> Objects.equals(p.getName(), preset.getName())).findFirst();

    if (existing.isPresent()) {
      // Update existing preset
      PresetConfiguration existingPreset = existing.get();
      existingPreset.setDescription(preset.getDescription());
      existingPreset.setPatternConfig(preset.getPatternConfig().copy());
      existingPreset.updateLastUsed();
    } else {
      // Add new preset
      presets.add(preset.copy());
    }

    // Persist to file
    savePresetsToFile();

    LOGGER.info("Saved preset: " + preset.getName());
  }

  /**
   * Loads a preset configuration by name.
   *
   * @param name the preset name
   * @return the preset configuration, or null if not found
   */
  public PresetConfiguration loadPreset(String name) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }

    Optional<PresetConfiguration> preset =
        presets.stream().filter(p -> Objects.equals(p.getName(), name)).findFirst();

    if (preset.isPresent()) {
      PresetConfiguration found = preset.get();
      found.updateLastUsed();

      // Save updated timestamp
      try {
        savePresetsToFile();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to update last used timestamp for preset: " + name, e);
      }

      return found.copy();
    }

    return null;
  }

  /**
   * Deletes a preset configuration by name.
   *
   * @param name the preset name
   * @return true if the preset was deleted
   */
  public boolean deletePreset(String name) {
    if (name == null || name.trim().isEmpty()) {
      return false;
    }

    // Don't allow deletion of default preset
    if (DEFAULT_PRESET_NAME.equals(name)) {
      LOGGER.warning("Cannot delete default preset");
      return false;
    }

    boolean removed = presets.removeIf(p -> Objects.equals(p.getName(), name));

    if (removed) {
      try {
        savePresetsToFile();
        LOGGER.info("Deleted preset: " + name);
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save presets after deletion", e);
      }
    }

    return removed;
  }

  /**
   * Lists all available preset configurations.
   *
   * @return an observable list of presets
   */
  public ObservableList<PresetConfiguration> listPresets() {
    return FXCollections.unmodifiableObservableList(presets);
  }

  /**
   * Gets the default preset configuration.
   *
   * @return the default preset
   */
  public PresetConfiguration getDefaultPreset() {
    return defaultPreset != null ? defaultPreset.copy() : null;
  }

  /**
   * Sets the default preset configuration.
   *
   * @param preset the preset to set as default
   */
  public void setDefaultPreset(PresetConfiguration preset) {
    if (preset != null && preset.isValid()) {
      preset.setName(DEFAULT_PRESET_NAME);
      this.defaultPreset = preset.copy();

      // Update or add to presets list
      Optional<PresetConfiguration> existing =
          presets.stream().filter(p -> DEFAULT_PRESET_NAME.equals(p.getName())).findFirst();

      if (existing.isPresent()) {
        existing.get().setPatternConfig(preset.getPatternConfig().copy());
        existing.get().updateLastUsed();
      } else {
        presets.add(0, this.defaultPreset.copy());
      }

      try {
        savePresetsToFile();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save default preset", e);
      }
    }
  }

  /**
   * Renames a preset configuration.
   *
   * @param oldName the current preset name
   * @param newName the new preset name
   * @return true if the preset was renamed
   */
  public boolean renamePreset(String oldName, String newName) {
    if (oldName == null
        || newName == null
        || oldName.trim().isEmpty()
        || newName.trim().isEmpty()) {
      return false;
    }

    if (oldName.equals(newName)) {
      return true; // No change needed
    }

    // Don't allow renaming default preset
    if (DEFAULT_PRESET_NAME.equals(oldName)) {
      LOGGER.warning("Cannot rename default preset");
      return false;
    }

    // Check if new name already exists
    boolean nameExists = presets.stream().anyMatch(p -> Objects.equals(p.getName(), newName));

    if (nameExists) {
      LOGGER.warning("Preset name already exists: " + newName);
      return false;
    }

    // Find and rename the preset
    Optional<PresetConfiguration> preset =
        presets.stream().filter(p -> Objects.equals(p.getName(), oldName)).findFirst();

    if (preset.isPresent()) {
      preset.get().setName(newName);

      try {
        savePresetsToFile();
        LOGGER.info("Renamed preset from '" + oldName + "' to '" + newName + "'");
        return true;
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save presets after rename", e);
        // Revert the name change
        preset.get().setName(oldName);
      }
    }

    return false;
  }

  /**
   * Duplicates a preset configuration with a new name.
   *
   * @param sourceName the name of the preset to duplicate
   * @param newName the name for the duplicated preset
   * @return true if the preset was duplicated
   */
  public boolean duplicatePreset(String sourceName, String newName) {
    if (sourceName == null
        || newName == null
        || sourceName.trim().isEmpty()
        || newName.trim().isEmpty()) {
      return false;
    }

    // Check if new name already exists
    boolean nameExists = presets.stream().anyMatch(p -> Objects.equals(p.getName(), newName));

    if (nameExists) {
      LOGGER.warning("Preset name already exists: " + newName);
      return false;
    }

    // Find source preset
    Optional<PresetConfiguration> sourcePreset =
        presets.stream().filter(p -> Objects.equals(p.getName(), sourceName)).findFirst();

    if (sourcePreset.isPresent()) {
      PresetConfiguration duplicate = sourcePreset.get().copy();
      duplicate.setName(newName);
      duplicate.setCreated(LocalDateTime.now());
      duplicate.setLastUsed(LocalDateTime.now());

      try {
        savePreset(duplicate);
        LOGGER.info("Duplicated preset '" + sourceName + "' as '" + newName + "'");
        return true;
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save duplicated preset", e);
      }
    }

    return false;
  }

  /**
   * Exports a preset to a JSON file.
   *
   * @param presetName the name of the preset to export
   * @param exportFile the file to export to
   * @throws IOException if export fails
   */
  public void exportPreset(String presetName, Path exportFile) throws IOException {
    PresetConfiguration preset = loadPreset(presetName);
    if (preset == null) {
      throw new IllegalArgumentException("Preset not found: " + presetName);
    }

    exportPreset(preset, exportFile);
  }

  /**
   * Exports a preset configuration to a JSON file.
   *
   * @param preset the preset to export
   * @param exportFile the file to export to
   * @throws IOException if export fails
   */
  public void exportPreset(PresetConfiguration preset, Path exportFile) throws IOException {
    if (preset == null) {
      throw new IllegalArgumentException("Preset cannot be null");
    }

    Map<String, Object> exportData = createExportData(preset);
    String json =
        JsonFormat.printer()
            .print(
                com.google.protobuf.Struct.newBuilder()
                    .putAllFields(convertToProtobufFields(exportData))
                    .build());

    Files.write(exportFile, json.getBytes());
    LOGGER.info("Exported preset '" + preset.getName() + "' to: " + exportFile);
  }

  /**
   * Imports a preset from a JSON file.
   *
   * @param importFile the file to import from
   * @return the imported preset configuration
   * @throws IOException if import fails
   */
  public PresetConfiguration importPreset(Path importFile) throws IOException {
    if (!Files.exists(importFile)) {
      throw new FileNotFoundException("Import file not found: " + importFile);
    }

    String json = Files.readString(importFile);

    // Parse JSON and create preset
    PresetConfiguration preset = parseImportData(json);

    // Validate the imported preset
    if (!preset.isValid()) {
      throw new IOException("Imported preset configuration is invalid");
    }

    LOGGER.info("Imported preset: " + preset.getName());
    return preset;
  }

  /**
   * Imports and saves a preset from a JSON file.
   *
   * @param importFile the file to import from
   * @param overwriteExisting whether to overwrite existing presets with the same name
   * @throws IOException if import fails
   */
  public void importAndSavePreset(Path importFile, boolean overwriteExisting) throws IOException {
    PresetConfiguration preset = importPreset(importFile);

    // Check if preset already exists
    boolean exists = presets.stream().anyMatch(p -> Objects.equals(p.getName(), preset.getName()));

    if (exists && !overwriteExisting) {
      throw new IOException("Preset already exists: " + preset.getName());
    }

    savePreset(preset);
  }

  /**
   * Validates and migrates preset configurations for compatibility. This method can be used to
   * update presets when the configuration format changes.
   *
   * @return the number of presets that were migrated
   */
  public int validateAndMigratePresets() {
    int migratedCount = 0;

    for (PresetConfiguration preset : presets) {
      if (migratePresetIfNeeded(preset)) {
        migratedCount++;
      }
    }

    if (migratedCount > 0) {
      try {
        savePresetsToFile();
        LOGGER.info("Migrated " + migratedCount + " presets");
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save migrated presets", e);
      }
    }

    return migratedCount;
  }

  /** Loads presets from the presets file. */
  private void loadPresets() {
    if (!Files.exists(presetsFile)) {
      LOGGER.info("No presets file found, starting with empty presets");
      return;
    }

    try {
      String json = Files.readString(presetsFile);
      List<PresetConfiguration> loadedPresets = parsePresetsJson(json);

      presets.clear();
      presets.addAll(loadedPresets);

      // Find default preset
      defaultPreset =
          presets.stream()
              .filter(p -> DEFAULT_PRESET_NAME.equals(p.getName()))
              .findFirst()
              .orElse(null);

      LOGGER.info("Loaded " + presets.size() + " presets");

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load presets from file", e);
    }
  }

  /** Saves presets to the presets file. */
  private void savePresetsToFile() throws IOException {
    String json = createPresetsJson(presets);
    Files.write(presetsFile, json.getBytes());
  }

  /** Ensures that a default preset exists. */
  private void ensureDefaultPreset() {
    if (defaultPreset == null) {
      // Create a basic default preset
      PatternConfiguration defaultConfig = new PatternConfiguration();
      defaultConfig.setGroupPattern("(.+)");

      defaultPreset =
          new PresetConfiguration(
              DEFAULT_PRESET_NAME, "Default pattern configuration", defaultConfig);

      presets.add(0, defaultPreset.copy());

      try {
        savePresetsToFile();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to save default preset", e);
      }
    }
  }

  /** Creates export data map for a preset. */
  private Map<String, Object> createExportData(PresetConfiguration preset) {
    Map<String, Object> data = new HashMap<>();
    data.put("name", preset.getName() != null ? preset.getName() : "");
    data.put("description", preset.getDescription() != null ? preset.getDescription() : "");
    data.put("created", preset.getCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    data.put("lastUsed", preset.getLastUsed().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    // Pattern configuration
    PatternConfiguration config = preset.getPatternConfig();
    if (config != null) {
      Map<String, Object> configData = new HashMap<>();

      // Only include patterns if they are not empty (presets store builder state, not
      // generated
      // patterns)
      if (config.getGroupPattern() != null && !config.getGroupPattern().isEmpty()) {
        configData.put("groupPattern", config.getGroupPattern());
      }
      if (config.getFrontPattern() != null && !config.getFrontPattern().isEmpty()) {
        configData.put("frontPattern", config.getFrontPattern());
      }
      if (config.getRearPattern() != null && !config.getRearPattern().isEmpty()) {
        configData.put("rearPattern", config.getRearPattern());
      }
      if (config.getOverviewPattern() != null && !config.getOverviewPattern().isEmpty()) {
        configData.put("overviewPattern", config.getOverviewPattern());
      }

      // Add optional token types
      if (config.getOptionalTokenTypes() != null && !config.getOptionalTokenTypes().isEmpty()) {
        configData.put("optionalTokenTypes", new ArrayList<>(config.getOptionalTokenTypes()));
      }

      // Add optional custom token names
      if (config.getOptionalCustomTokenNames() != null
          && !config.getOptionalCustomTokenNames().isEmpty()) {
        configData.put(
            "optionalCustomTokenNames", new ArrayList<>(config.getOptionalCustomTokenNames()));
      }

      // Add tokens (FilenameToken list with type assignments)
      if (config.getTokens() != null && !config.getTokens().isEmpty()) {
        List<Map<String, Object>> tokensList = new ArrayList<>();
        for (FilenameToken token : config.getTokens()) {
          Map<String, Object> tokenData = new HashMap<>();
          tokenData.put("value", token.getValue() != null ? token.getValue() : "");
          tokenData.put("position", token.getPosition());
          tokenData.put(
              "suggestedType",
              token.getSuggestedType() != null ? token.getSuggestedType().name() : "UNKNOWN");
          tokenData.put("confidence", token.getConfidence());
          tokensList.add(tokenData);
        }
        configData.put("tokens", tokensList);
      }

      // Add group ID token type
      if (config.getGroupIdToken() != null) {
        configData.put("groupIdToken", config.getGroupIdToken().name());
      }

      // Add role rules
      if (config.getRoleRules() != null && !config.getRoleRules().isEmpty()) {
        List<Map<String, Object>> rulesList = new ArrayList<>();
        for (RoleRule rule : config.getRoleRules()) {
          Map<String, Object> ruleData = new HashMap<>();
          ruleData.put("role", rule.getTargetRole() != null ? rule.getTargetRole().name() : "");
          ruleData.put("type", rule.getRuleType() != null ? rule.getRuleType().name() : "");
          ruleData.put("value", rule.getRuleValue() != null ? rule.getRuleValue() : "");
          ruleData.put("caseSensitive", rule.isCaseSensitive());
          ruleData.put("priority", rule.getPriority());
          rulesList.add(ruleData);
        }
        configData.put("roleRules", rulesList);
      }

      data.put("patternConfig", configData);
    }

    return data;
  }

  /** Converts a map to protobuf fields for JSON serialization. */
  private Map<String, com.google.protobuf.Value> convertToProtobufFields(Map<String, Object> data) {
    Map<String, com.google.protobuf.Value> fields = new HashMap<>();

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      com.google.protobuf.Value.Builder valueBuilder = com.google.protobuf.Value.newBuilder();

      Object value = entry.getValue();
      if (value instanceof String) {
        valueBuilder.setStringValue((String) value);
      } else if (value instanceof Number) {
        valueBuilder.setNumberValue(((Number) value).doubleValue());
      } else if (value instanceof Boolean) {
        valueBuilder.setBoolValue((Boolean) value);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> listValue = (List<Object>) value;
        com.google.protobuf.ListValue.Builder listBuilder =
            com.google.protobuf.ListValue.newBuilder();
        for (Object item : listValue) {
          if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapItem = (Map<String, Object>) item;
            listBuilder.addValues(
                com.google.protobuf.Value.newBuilder()
                    .setStructValue(
                        com.google.protobuf.Struct.newBuilder()
                            .putAllFields(convertToProtobufFields(mapItem))
                            .build())
                    .build());
          } else if (item instanceof String) {
            listBuilder.addValues(
                com.google.protobuf.Value.newBuilder().setStringValue((String) item).build());
          } else if (item instanceof Number) {
            listBuilder.addValues(
                com.google.protobuf.Value.newBuilder()
                    .setNumberValue(((Number) item).doubleValue())
                    .build());
          } else if (item instanceof Boolean) {
            listBuilder.addValues(
                com.google.protobuf.Value.newBuilder().setBoolValue((Boolean) item).build());
          }
        }
        valueBuilder.setListValue(listBuilder.build());
      } else if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mapValue = (Map<String, Object>) value;
        valueBuilder.setStructValue(
            com.google.protobuf.Struct.newBuilder()
                .putAllFields(convertToProtobufFields(mapValue))
                .build());
      }

      fields.put(entry.getKey(), valueBuilder.build());
    }

    return fields;
  }

  /** Parses import data from JSON string. */
  private PresetConfiguration parseImportData(String json) throws IOException {
    try {
      // Parse JSON using protobuf JsonFormat
      com.google.protobuf.Struct.Builder structBuilder = com.google.protobuf.Struct.newBuilder();
      JsonFormat.parser().merge(json, structBuilder);
      com.google.protobuf.Struct struct = structBuilder.build();

      // Extract preset data
      PresetConfiguration preset = new PresetConfiguration();

      Map<String, com.google.protobuf.Value> fields = struct.getFieldsMap();

      if (fields.containsKey("name")) {
        preset.setName(fields.get("name").getStringValue());
      }

      if (fields.containsKey("description")) {
        preset.setDescription(fields.get("description").getStringValue());
      }

      if (fields.containsKey("created")) {
        try {
          preset.setCreated(
              LocalDateTime.parse(
                  fields.get("created").getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
          // Use current time if parsing fails
          preset.setCreated(LocalDateTime.now());
        }
      }

      if (fields.containsKey("lastUsed")) {
        try {
          preset.setLastUsed(
              LocalDateTime.parse(
                  fields.get("lastUsed").getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
          // Use current time if parsing fails
          preset.setLastUsed(LocalDateTime.now());
        }
      }

      // Parse pattern configuration
      if (fields.containsKey("patternConfig")) {
        com.google.protobuf.Value configValue = fields.get("patternConfig");
        if (configValue.hasStructValue()) {
          PatternConfiguration config = new PatternConfiguration();
          Map<String, com.google.protobuf.Value> configFields =
              configValue.getStructValue().getFieldsMap();

          if (configFields.containsKey("groupPattern")) {
            config.setGroupPattern(configFields.get("groupPattern").getStringValue());
          }

          if (configFields.containsKey("frontPattern")) {
            config.setFrontPattern(configFields.get("frontPattern").getStringValue());
          }

          if (configFields.containsKey("rearPattern")) {
            config.setRearPattern(configFields.get("rearPattern").getStringValue());
          }

          if (configFields.containsKey("overviewPattern")) {
            config.setOverviewPattern(configFields.get("overviewPattern").getStringValue());
          }

          preset.setPatternConfig(config);
        }
      }

      return preset;

    } catch (Exception e) {
      throw new IOException("Failed to parse preset JSON: " + e.getMessage(), e);
    }
  }

  /** Parses presets from JSON string. */
  private List<PresetConfiguration> parsePresetsJson(String json) throws IOException {
    List<PresetConfiguration> presets = new ArrayList<>();

    try {
      // Parse JSON using protobuf JsonFormat
      com.google.protobuf.Struct.Builder structBuilder = com.google.protobuf.Struct.newBuilder();
      JsonFormat.parser().merge(json, structBuilder);
      com.google.protobuf.Struct struct = structBuilder.build();

      Map<String, com.google.protobuf.Value> fields = struct.getFieldsMap();

      if (fields.containsKey("presets")) {
        com.google.protobuf.Value presetsValue = fields.get("presets");
        if (presetsValue.hasListValue()) {
          for (com.google.protobuf.Value presetValue :
              presetsValue.getListValue().getValuesList()) {
            if (presetValue.hasStructValue()) {
              PresetConfiguration preset = parsePresetFromStruct(presetValue.getStructValue());
              if (preset != null) {
                presets.add(preset);
              }
            }
          }
        }
      }

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to parse presets JSON, starting with empty list", e);
    }

    return presets;
  }

  /** Parses a single preset from a protobuf Struct. */
  private PresetConfiguration parsePresetFromStruct(com.google.protobuf.Struct struct) {
    try {
      PresetConfiguration preset = new PresetConfiguration();
      Map<String, com.google.protobuf.Value> fields = struct.getFieldsMap();

      if (fields.containsKey("name")) {
        preset.setName(fields.get("name").getStringValue());
      }

      if (fields.containsKey("description")) {
        preset.setDescription(fields.get("description").getStringValue());
      }

      if (fields.containsKey("created")) {
        try {
          preset.setCreated(
              LocalDateTime.parse(
                  fields.get("created").getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
          preset.setCreated(LocalDateTime.now());
        }
      }

      if (fields.containsKey("lastUsed")) {
        try {
          preset.setLastUsed(
              LocalDateTime.parse(
                  fields.get("lastUsed").getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
          preset.setLastUsed(LocalDateTime.now());
        }
      }

      // Parse pattern configuration
      if (fields.containsKey("patternConfig")) {
        com.google.protobuf.Value configValue = fields.get("patternConfig");
        if (configValue.hasStructValue()) {
          PatternConfiguration config = new PatternConfiguration();
          Map<String, com.google.protobuf.Value> configFields =
              configValue.getStructValue().getFieldsMap();

          if (configFields.containsKey("groupPattern")) {
            config.setGroupPattern(configFields.get("groupPattern").getStringValue());
          }

          if (configFields.containsKey("frontPattern")) {
            config.setFrontPattern(configFields.get("frontPattern").getStringValue());
          }

          if (configFields.containsKey("rearPattern")) {
            config.setRearPattern(configFields.get("rearPattern").getStringValue());
          }

          if (configFields.containsKey("overviewPattern")) {
            config.setOverviewPattern(configFields.get("overviewPattern").getStringValue());
          }

          // Parse optional token types
          if (configFields.containsKey("optionalTokenTypes")) {
            com.google.protobuf.Value optTypesValue = configFields.get("optionalTokenTypes");
            if (optTypesValue.hasListValue()) {
              List<String> optionalTypes = new ArrayList<>();
              for (com.google.protobuf.Value val : optTypesValue.getListValue().getValuesList()) {
                optionalTypes.add(val.getStringValue());
              }
              config.setOptionalTokenTypes(optionalTypes);
            }
          }

          // Parse optional custom token names
          if (configFields.containsKey("optionalCustomTokenNames")) {
            com.google.protobuf.Value optCustomValue = configFields.get("optionalCustomTokenNames");
            if (optCustomValue.hasListValue()) {
              List<String> optionalCustom = new ArrayList<>();
              for (com.google.protobuf.Value val : optCustomValue.getListValue().getValuesList()) {
                optionalCustom.add(val.getStringValue());
              }
              config.setOptionalCustomTokenNames(optionalCustom);
            }
          }

          // Parse tokens
          if (configFields.containsKey("tokens")) {
            com.google.protobuf.Value tokensValue = configFields.get("tokens");
            if (tokensValue.hasListValue()) {
              List<FilenameToken> tokens = new ArrayList<>();
              LOGGER.info("Parsing tokens: count = " + tokensValue.getListValue().getValuesCount());
              for (com.google.protobuf.Value tokenVal :
                  tokensValue.getListValue().getValuesList()) {
                if (tokenVal.hasStructValue()) {
                  Map<String, com.google.protobuf.Value> tokenFields =
                      tokenVal.getStructValue().getFieldsMap();
                  String value =
                      tokenFields.containsKey("value")
                          ? tokenFields.get("value").getStringValue()
                          : "";
                  int position =
                      tokenFields.containsKey("position")
                          ? (int) tokenFields.get("position").getNumberValue()
                          : 0;
                  TokenType suggestedType =
                      tokenFields.containsKey("suggestedType")
                          ? TokenType.valueOf(tokenFields.get("suggestedType").getStringValue())
                          : TokenType.UNKNOWN;
                  double confidence =
                      tokenFields.containsKey("confidence")
                          ? tokenFields.get("confidence").getNumberValue()
                          : 0.0;

                  FilenameToken token = new FilenameToken(value, position);
                  token.setSuggestedType(suggestedType);
                  token.setConfidence(confidence);
                  tokens.add(token);
                  LOGGER.info(
                      "Added token: value="
                          + value
                          + ", position="
                          + position
                          + ", type="
                          + suggestedType);
                }
              }
              config.setTokens(tokens);
              LOGGER.info("Total tokens parsed: " + tokens.size());
            } else {
              LOGGER.warning("tokensValue has no ListValue!");
            }
          } else {
            LOGGER.warning("configFields does not contain 'tokens' key!");
          }

          // Parse group ID token
          if (configFields.containsKey("groupIdToken")) {
            try {
              String groupIdStr = configFields.get("groupIdToken").getStringValue();
              LOGGER.info("Parsing groupIdToken: " + groupIdStr);
              TokenType groupIdToken = TokenType.valueOf(groupIdStr);
              config.setGroupIdToken(groupIdToken);
              LOGGER.info("Successfully set groupIdToken: " + groupIdToken);
            } catch (IllegalArgumentException e) {
              LOGGER.warning("Invalid groupIdToken value: " + e.getMessage());
            }
          } else {
            LOGGER.warning("configFields does not contain 'groupIdToken' key!");
          }

          // Parse role rules
          if (configFields.containsKey("roleRules")) {
            com.google.protobuf.Value rulesValue = configFields.get("roleRules");
            if (rulesValue.hasListValue()) {
              List<RoleRule> roleRules = new ArrayList<>();
              for (com.google.protobuf.Value ruleVal : rulesValue.getListValue().getValuesList()) {
                if (ruleVal.hasStructValue()) {
                  Map<String, com.google.protobuf.Value> ruleFields =
                      ruleVal.getStructValue().getFieldsMap();
                  try {
                    ImageRole role =
                        ruleFields.containsKey("role")
                            ? ImageRole.valueOf(ruleFields.get("role").getStringValue())
                            : null;
                    RuleType type =
                        ruleFields.containsKey("type")
                            ? RuleType.valueOf(ruleFields.get("type").getStringValue())
                            : null;
                    String value =
                        ruleFields.containsKey("value")
                            ? ruleFields.get("value").getStringValue()
                            : "";
                    boolean caseSensitive =
                        ruleFields.containsKey("caseSensitive")
                            && ruleFields.get("caseSensitive").getBoolValue();
                    int priority =
                        ruleFields.containsKey("priority")
                            ? (int) ruleFields.get("priority").getNumberValue()
                            : 0;

                    if (role != null && type != null) {
                      RoleRule rule = new RoleRule(role, type, value, caseSensitive, priority);
                      roleRules.add(rule);
                    }
                  } catch (IllegalArgumentException e) {
                    // Invalid enum value, skip this rule
                  }
                }
              }
              config.setRoleRules(roleRules);
            }
          }

          preset.setPatternConfig(config);
        }
      }

      return preset;

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to parse preset from struct", e);
      return null;
    }
  }

  /** Creates JSON string from presets list. */
  private String createPresetsJson(List<PresetConfiguration> presets) throws IOException {
    try {
      // Create the main structure
      com.google.protobuf.Struct.Builder mainBuilder = com.google.protobuf.Struct.newBuilder();

      // Create presets array
      com.google.protobuf.ListValue.Builder presetsListBuilder =
          com.google.protobuf.ListValue.newBuilder();

      for (PresetConfiguration preset : presets) {
        // Use createExportData to get complete preset data including tokens, roleRules,
        // etc.
        Map<String, Object> presetData = createExportData(preset);

        // Convert to protobuf struct and add to list
        com.google.protobuf.Struct presetStruct =
            com.google.protobuf.Struct.newBuilder()
                .putAllFields(convertToProtobufFields(presetData))
                .build();

        presetsListBuilder.addValues(
            com.google.protobuf.Value.newBuilder().setStructValue(presetStruct).build());
      }

      // Add presets list to main structure
      mainBuilder.putFields(
          "presets",
          com.google.protobuf.Value.newBuilder().setListValue(presetsListBuilder.build()).build());

      // Convert to JSON
      return JsonFormat.printer().print(mainBuilder.build());

    } catch (Exception e) {
      throw new IOException("Failed to create presets JSON: " + e.getMessage(), e);
    }
  }

  /** Migrates a preset configuration if needed. */
  private boolean migratePresetIfNeeded(PresetConfiguration preset) {
    // This method would contain migration logic for configuration format changes
    // For now, no migration is needed
    return false;
  }
}
