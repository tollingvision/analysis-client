package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * UI component for selecting and managing pattern presets. Provides dropdown selection, save/load
 * functionality, and preset management operations.
 */
public class PresetSelector extends VBox {
  private static final Logger LOGGER = Logger.getLogger(PresetSelector.class.getName());

  private final PresetManager presetManager;
  private final ResourceBundle messages;
  private final ObjectProperty<PresetConfiguration> selectedPreset = new SimpleObjectProperty<>();

  // UI Components
  private ComboBox<PresetConfiguration> presetComboBox;
  private Button updateButton;
  private Button saveAsButton;
  private Button deleteButton;
  private Button manageButton;
  private Button importButton;
  private Button exportButton;

  // Callbacks
  private Consumer<PresetConfiguration> onPresetSelected;
  private Consumer<PresetConfiguration> onPresetSaved;

  /**
   * Creates a new PresetSelector with the specified preset manager.
   *
   * @param presetManager the preset manager to use
   */
  public PresetSelector(PresetManager presetManager) {
    this(presetManager, java.util.ResourceBundle.getBundle("messages"));
  }

  /**
   * Creates a new PresetSelector with the specified preset manager and messages bundle.
   *
   * @param presetManager the preset manager to use
   * @param messages the resource bundle for i18n
   */
  public PresetSelector(PresetManager presetManager, ResourceBundle messages) {
    this.presetManager = presetManager;
    this.messages = messages;

    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setupBindings();

    // Load presets
    refreshPresets();

    // Select default preset if available
    PresetConfiguration defaultPreset = presetManager.getDefaultPreset();
    if (defaultPreset != null) {
      presetComboBox.setValue(defaultPreset);
    }
  }

  /** Initializes all UI components. */
  private void initializeComponents() {
    // Preset dropdown
    presetComboBox = new ComboBox<>();
    presetComboBox.setPromptText(messages.getString("preset.selector.prompt"));
    presetComboBox.setMaxWidth(Double.MAX_VALUE);

    // Custom cell factory to display preset names
    presetComboBox.setCellFactory(
        listView ->
            new ListCell<PresetConfiguration>() {
              @Override
              protected void updateItem(PresetConfiguration preset, boolean empty) {
                super.updateItem(preset, empty);
                if (empty || preset == null) {
                  setText(null);
                  setTooltip(null);
                } else {
                  setText(preset.getName());
                  if (preset.getDescription() != null
                      && !preset.getDescription().trim().isEmpty()) {
                    setTooltip(new Tooltip(preset.getDescription()));
                  }
                }
              }
            });

    // Button text for selected item
    presetComboBox.setButtonCell(
        new ListCell<PresetConfiguration>() {
          @Override
          protected void updateItem(PresetConfiguration preset, boolean empty) {
            super.updateItem(preset, empty);
            if (empty || preset == null) {
              setText(messages.getString("preset.selector.prompt"));
            } else {
              setText(preset.getName());
            }
          }
        });

    // Action buttons
    updateButton = new Button(messages.getString("button.save"));
    updateButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.save")));

    saveAsButton = new Button(messages.getString("button.save.as"));
    saveAsButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.save.as")));

    deleteButton = new Button(messages.getString("button.delete"));
    deleteButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.delete")));

    manageButton = new Button(messages.getString("button.manage"));
    manageButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.manage")));

    importButton = new Button(messages.getString("button.import"));
    importButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.import")));

    exportButton = new Button(messages.getString("button.export"));
    exportButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.export")));
  }

  /** Sets up the layout structure. */
  private void setupLayout() {
    setSpacing(10);
    setPadding(new Insets(10));

    // Preset selection row
    HBox selectionRow = new HBox(10);
    selectionRow.setAlignment(Pos.CENTER_LEFT);

    Label presetLabel = new Label(messages.getString("preset.label"));
    presetLabel.setMinWidth(60);

    HBox.setHgrow(presetComboBox, Priority.ALWAYS);

    selectionRow.getChildren().addAll(presetLabel, presetComboBox);

    // Action buttons row
    HBox buttonRow = new HBox(5);
    buttonRow.setAlignment(Pos.CENTER_LEFT);

    buttonRow
        .getChildren()
        .addAll(
            updateButton,
            saveAsButton,
            deleteButton,
            new Separator(),
            manageButton,
            new Separator(),
            importButton,
            exportButton);

    getChildren().addAll(selectionRow, buttonRow);
  }

  /** Sets up event handlers for UI interactions. */
  private void setupEventHandlers() {
    // Preset selection
    presetComboBox.setOnAction(
        e -> {
          PresetConfiguration selected = presetComboBox.getValue();
          if (selected != null) {
            selectedPreset.set(selected);
            if (onPresetSelected != null) {
              onPresetSelected.accept(selected);
            }
          }
        });

    // Update button - updates current preset
    updateButton.setOnAction(e -> updateCurrentPreset());

    // Save As button - creates new preset
    saveAsButton.setOnAction(e -> showSavePresetDialog());

    // Delete button
    deleteButton.setOnAction(e -> deleteSelectedPreset());

    // Manage button
    manageButton.setOnAction(e -> showPresetManagementDialog());

    // Import button
    importButton.setOnAction(e -> importPreset());

    // Export button
    exportButton.setOnAction(e -> exportSelectedPreset());
  }

  /** Sets up data bindings between components. */
  private void setupBindings() {
    // Enable/disable buttons based on selection
    // Update button disabled when no preset selected or when Default preset is
    // selected
    updateButton
        .disableProperty()
        .bind(
            selectedPreset.isNull().or(selectedPreset.isEqualTo(presetManager.getDefaultPreset())));

    deleteButton
        .disableProperty()
        .bind(
            selectedPreset.isNull().or(selectedPreset.isEqualTo(presetManager.getDefaultPreset())));

    exportButton.disableProperty().bind(selectedPreset.isNull());
  }

  /** Refreshes the preset list from the preset manager. */
  public void refreshPresets() {
    ObservableList<PresetConfiguration> presets = presetManager.listPresets();
    presetComboBox.setItems(FXCollections.observableArrayList(presets));
  }

  /**
   * Gets the currently selected preset.
   *
   * @return the selected preset, or null if none selected
   */
  public PresetConfiguration getSelectedPreset() {
    return selectedPreset.get();
  }

  /**
   * Sets the selected preset.
   *
   * @param preset the preset to select
   */
  public void setSelectedPreset(PresetConfiguration preset) {
    presetComboBox.setValue(preset);
    selectedPreset.set(preset);
  }

  /**
   * Sets the callback to be invoked when a preset is selected.
   *
   * @param callback the selection callback
   */
  public void setOnPresetSelected(Consumer<PresetConfiguration> callback) {
    this.onPresetSelected = callback;
  }

  /**
   * Sets the callback to be invoked when a preset is saved.
   *
   * @param callback the save callback
   */
  public void setOnPresetSaved(Consumer<PresetConfiguration> callback) {
    this.onPresetSaved = callback;
  }

  /**
   * Property for the selected preset (for binding).
   *
   * @return the selected preset property
   */
  public ObjectProperty<PresetConfiguration> selectedPresetProperty() {
    return selectedPreset;
  }

  /** Updates the currently selected preset with the current configuration. */
  private void updateCurrentPreset() {
    PresetConfiguration selected = getSelectedPreset();
    if (selected == null || selected.equals(presetManager.getDefaultPreset())) {
      return;
    }

    PatternConfiguration currentConfig = getCurrentConfiguration();
    if (currentConfig == null) {
      return;
    }

    // Update the preset's configuration
    selected.setPatternConfig(currentConfig.copy());
    selected.updateLastUsed();

    try {
      presetManager.savePreset(selected);
      refreshPresets();

      if (onPresetSaved != null) {
        onPresetSaved.accept(selected);
      }

      showInfoAlert(
          messages.getString("preset.alert.updated.title"),
          String.format(messages.getString("preset.alert.updated.message"), selected.getName()));

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to update preset", e);
      showErrorAlert(
          messages.getString("preset.alert.update.failed.title"),
          String.format(messages.getString("preset.alert.update.failed.message"), e.getMessage()));
    }
  }

  /** Shows the save preset dialog to create a new preset. */
  private void showSavePresetDialog() {
    SavePresetDialog dialog = new SavePresetDialog();
    Optional<SavePresetDialog.PresetInfo> result = dialog.showAndWait();

    if (result.isPresent()) {
      SavePresetDialog.PresetInfo info = result.get();

      // Create preset configuration
      // Note: This would need to get the current configuration from the parent dialog
      PatternConfiguration currentConfig = getCurrentConfiguration();
      if (currentConfig != null) {
        PresetConfiguration preset =
            new PresetConfiguration(info.getName(), info.getDescription(), currentConfig);

        try {
          presetManager.savePreset(preset);
          refreshPresets();
          setSelectedPreset(preset);

          if (onPresetSaved != null) {
            onPresetSaved.accept(preset);
          }

          showInfoAlert(
              messages.getString("preset.alert.saved.title"),
              String.format(messages.getString("preset.alert.saved.message"), info.getName()));

        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Failed to save preset", e);
          showErrorAlert(
              messages.getString("preset.alert.save.failed.title"),
              String.format(
                  messages.getString("preset.alert.save.failed.message"), e.getMessage()));
        }
      }
    }
  }

  /** Deletes the currently selected preset. */
  private void deleteSelectedPreset() {
    PresetConfiguration selected = getSelectedPreset();
    if (selected == null) {
      return;
    }

    // Confirm deletion
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle(messages.getString("preset.delete.title"));
    confirmAlert.setHeaderText(
        String.format(messages.getString("preset.delete.header"), selected.getName()));
    confirmAlert.setContentText(messages.getString("preset.delete.content"));

    Optional<ButtonType> result = confirmAlert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      boolean deleted = presetManager.deletePreset(selected.getName());
      if (deleted) {
        refreshPresets();
        presetComboBox.setValue(null);
        selectedPreset.set(null);

        showInfoAlert(
            messages.getString("preset.alert.deleted.title"),
            String.format(messages.getString("preset.alert.deleted.message"), selected.getName()));
      } else {
        showErrorAlert(
            messages.getString("preset.alert.delete.failed.title"),
            String.format(
                messages.getString("preset.alert.delete.failed.message"), selected.getName()));
      }
    }
  }

  /** Shows the preset management dialog. */
  private void showPresetManagementDialog() {
    PresetManagementDialog dialog = new PresetManagementDialog(presetManager);
    dialog.showAndWait();

    // Refresh presets after management
    refreshPresets();
  }

  /** Imports a preset from a JSON file. */
  private void importPreset() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(messages.getString("preset.import.title"));
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(messages.getString("file.filter.json"), "*.json"));

    Stage stage = (Stage) getScene().getWindow();
    File file = fileChooser.showOpenDialog(stage);

    if (file != null) {
      try {
        PresetConfiguration imported = presetManager.importPreset(file.toPath());

        // Check if preset already exists
        boolean exists =
            presetManager.listPresets().stream()
                .anyMatch(p -> p.getName().equals(imported.getName()));

        if (exists) {
          Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
          confirmAlert.setTitle(messages.getString("preset.exists.title"));
          confirmAlert.setHeaderText(
              String.format(messages.getString("preset.exists.header"), imported.getName()));
          confirmAlert.setContentText(messages.getString("preset.exists.content"));

          Optional<ButtonType> result = confirmAlert.showAndWait();
          if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
          }
        }

        presetManager.savePreset(imported);
        refreshPresets();
        setSelectedPreset(imported);

        showInfoAlert(
            messages.getString("preset.import.success.title"),
            String.format(messages.getString("preset.import.success.message"), imported.getName()));

      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to import preset", e);
        showErrorAlert(
            messages.getString("preset.import.failed.title"),
            String.format(messages.getString("preset.import.failed.message"), e.getMessage()));
      }
    }
  }

  /** Exports the selected preset to a JSON file. */
  private void exportSelectedPreset() {
    PresetConfiguration selected = getSelectedPreset();
    if (selected == null) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(messages.getString("preset.export.title"));
    fileChooser.setInitialFileName(selected.getName() + ".json");
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(messages.getString("file.filter.json"), "*.json"));

    Stage stage = (Stage) getScene().getWindow();
    File file = fileChooser.showSaveDialog(stage);

    if (file != null) {
      try {
        presetManager.exportPreset(selected, file.toPath());
        showInfoAlert(
            messages.getString("preset.export.success.title"),
            String.format(
                messages.getString("preset.export.success.message"),
                selected.getName(),
                file.getName()));

      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to export preset", e);
        showErrorAlert(
            messages.getString("preset.export.failed.title"),
            String.format(messages.getString("preset.export.failed.message"), e.getMessage()));
      }
    }
  }

  // Callback to get current configuration
  private java.util.function.Supplier<PatternConfiguration> currentConfigurationSupplier;

  /**
   * Sets the supplier for getting the current configuration.
   *
   * @param supplier the configuration supplier
   */
  public void setCurrentConfigurationSupplier(
      java.util.function.Supplier<PatternConfiguration> supplier) {
    this.currentConfigurationSupplier = supplier;
  }

  /**
   * Gets the current configuration from the parent dialog.
   *
   * @return the current pattern configuration
   */
  private PatternConfiguration getCurrentConfiguration() {
    if (currentConfigurationSupplier != null) {
      return currentConfigurationSupplier.get();
    }
    return new PatternConfiguration();
  }

  /** Shows an information alert. */
  private void showInfoAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  /** Shows an error alert. */
  private void showErrorAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
