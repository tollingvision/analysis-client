package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for advanced preset management operations. Provides rename, duplicate, organize, and batch
 * import/export functionality.
 */
public class PresetManagementDialog extends Stage {
  private static final Logger LOGGER = Logger.getLogger(PresetManagementDialog.class.getName());
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final PresetManager presetManager;
  private final ResourceBundle messages = java.util.ResourceBundle.getBundle("messages");

  // UI Components
  private TableView<PresetConfiguration> presetsTable;
  private ObservableList<PresetConfiguration> presets;

  private Button renameButton;
  private Button duplicateButton;
  private Button deleteButton;
  private Button importButton;
  private Button exportButton;
  private Button closeButton;

  /**
   * Creates a new PresetManagementDialog.
   *
   * @param presetManager the preset manager to use
   */
  public PresetManagementDialog(PresetManager presetManager) {
    this.presetManager = presetManager;

    initializeDialog();
    initializeComponents();
    setupLayout();
    setupEventHandlers();

    // Load presets
    refreshPresets();
  }

  /** Initializes the dialog properties. */
  private void initializeDialog() {
    setTitle(messages.getString("preset.manage.title"));
    initModality(Modality.APPLICATION_MODAL);
    setResizable(true);

    setMinWidth(600);
    setMinHeight(400);
    setWidth(800);
    setHeight(500);

    centerOnScreen();
  }

  /** Initializes all UI components. */
  private void initializeComponents() {
    // Presets table
    presetsTable = new TableView<>();
    presets = FXCollections.observableArrayList();
    presetsTable.setItems(presets);

    // Table columns
    TableColumn<PresetConfiguration, String> nameColumn =
        new TableColumn<>(messages.getString("preset.manage.column.name"));
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    nameColumn.setPrefWidth(150);

    TableColumn<PresetConfiguration, String> descriptionColumn =
        new TableColumn<>(messages.getString("preset.manage.column.description"));
    descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    descriptionColumn.setPrefWidth(250);

    TableColumn<PresetConfiguration, String> createdColumn =
        new TableColumn<>(messages.getString("preset.manage.column.created"));
    createdColumn.setCellValueFactory(
        cellData ->
            new SimpleStringProperty(cellData.getValue().getCreated().format(DATE_FORMATTER)));
    createdColumn.setPrefWidth(120);

    TableColumn<PresetConfiguration, String> lastUsedColumn =
        new TableColumn<>(messages.getString("preset.manage.column.last.used"));
    lastUsedColumn.setCellValueFactory(
        cellData ->
            new SimpleStringProperty(cellData.getValue().getLastUsed().format(DATE_FORMATTER)));
    lastUsedColumn.setPrefWidth(120);

    presetsTable
        .getColumns()
        .addAll(
            java.util.Arrays.asList(nameColumn, descriptionColumn, createdColumn, lastUsedColumn));

    // Selection mode
    presetsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    // Action buttons
    renameButton = new Button(messages.getString("preset.manage.rename"));
    renameButton.setTooltip(new Tooltip(messages.getString("preset.manage.rename.tooltip")));

    duplicateButton = new Button(messages.getString("preset.manage.duplicate"));
    duplicateButton.setTooltip(new Tooltip(messages.getString("preset.manage.duplicate.tooltip")));

    deleteButton = new Button(messages.getString("button.delete"));
    deleteButton.setTooltip(new Tooltip(messages.getString("preset.tooltip.delete")));

    importButton = new Button(messages.getString("preset.manage.import"));
    importButton.setTooltip(new Tooltip(messages.getString("preset.manage.import.tooltip")));

    exportButton = new Button(messages.getString("preset.manage.export"));
    exportButton.setTooltip(new Tooltip(messages.getString("preset.manage.export.tooltip")));

    closeButton = new Button(messages.getString("button.close"));
    closeButton.setDefaultButton(true);
  }

  /** Sets up the layout structure. */
  private void setupLayout() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(15));

    // Header
    VBox header = new VBox(5);
    Label titleLabel = new Label(messages.getString("preset.manage.header"));
    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    Label subtitleLabel = new Label(messages.getString("preset.manage.subheader"));
    subtitleLabel.setStyle("-fx-text-fill: gray;");

    header.getChildren().addAll(titleLabel, subtitleLabel);

    // Center - table
    VBox.setVgrow(presetsTable, Priority.ALWAYS);

    // Right side - action buttons
    VBox buttonPanel = new VBox(10);
    buttonPanel.setPadding(new Insets(0, 0, 0, 15));
    buttonPanel.setAlignment(Pos.TOP_CENTER);
    buttonPanel.setMinWidth(120);

    // Preset actions
    Label presetActionsLabel = new Label(messages.getString("preset.manage.actions"));
    presetActionsLabel.setStyle("-fx-font-weight: bold;");

    VBox presetActions = new VBox(5);
    presetActions.getChildren().addAll(renameButton, duplicateButton, deleteButton);

    // File operations
    Label fileOpsLabel = new Label(messages.getString("preset.manage.file.ops"));
    fileOpsLabel.setStyle("-fx-font-weight: bold;");

    VBox fileOps = new VBox(5);
    fileOps.getChildren().addAll(importButton, exportButton);

    buttonPanel
        .getChildren()
        .addAll(presetActionsLabel, presetActions, new Separator(), fileOpsLabel, fileOps);

    // Bottom - close button
    HBox footer = new HBox();
    footer.setAlignment(Pos.CENTER_RIGHT);
    footer.setPadding(new Insets(15, 0, 0, 0));
    footer.getChildren().add(closeButton);

    // Layout
    root.setTop(header);
    root.setCenter(presetsTable);
    root.setRight(buttonPanel);
    root.setBottom(footer);

    Scene scene = new Scene(root);
    scene.getStylesheets().add(getClass().getResource("/tollingvision-theme.css").toExternalForm());
    setScene(scene);
  }

  /** Sets up event handlers for UI interactions. */
  private void setupEventHandlers() {
    // Table selection
    presetsTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSelection, newSelection) -> {
              boolean hasSelection = newSelection != null;
              boolean canModify = hasSelection && !"Default".equals(newSelection.getName());

              renameButton.setDisable(!canModify);
              duplicateButton.setDisable(!hasSelection);
              deleteButton.setDisable(!canModify);
              exportButton.setDisable(!hasSelection);
            });

    // Button actions
    renameButton.setOnAction(e -> renameSelectedPreset());
    duplicateButton.setOnAction(e -> duplicateSelectedPreset());
    deleteButton.setOnAction(e -> deleteSelectedPreset());
    importButton.setOnAction(e -> importPresets());
    exportButton.setOnAction(e -> exportSelectedPreset());
    closeButton.setOnAction(e -> close());

    // Double-click to rename
    presetsTable.setRowFactory(
        tv -> {
          TableRow<PresetConfiguration> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                  PresetConfiguration preset = row.getItem();
                  if (!"Default".equals(preset.getName())) {
                    renameSelectedPreset();
                  }
                }
              });
          return row;
        });

    // Window close
    setOnCloseRequest(e -> close());
  }

  /** Refreshes the presets list from the preset manager. */
  private void refreshPresets() {
    presets.clear();
    presets.addAll(presetManager.listPresets());

    // Clear selection
    presetsTable.getSelectionModel().clearSelection();
  }

  /** Renames the selected preset. */
  private void renameSelectedPreset() {
    PresetConfiguration selected = presetsTable.getSelectionModel().getSelectedItem();
    if (selected == null || "Default".equals(selected.getName())) {
      return;
    }

    TextInputDialog dialog = new TextInputDialog(selected.getName());
    dialog.setTitle(messages.getString("preset.manage.rename.title"));
    dialog.setHeaderText(
        String.format(messages.getString("preset.manage.rename.header"), selected.getName()));
    dialog.setContentText(messages.getString("preset.manage.rename.content"));

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent()) {
      String newName = result.get().trim();
      if (!newName.isEmpty() && !newName.equals(selected.getName())) {
        boolean renamed = presetManager.renamePreset(selected.getName(), newName);
        if (renamed) {
          refreshPresets();
          showInfoAlert(
              messages.getString("preset.manage.rename.success.title"),
              String.format(messages.getString("preset.manage.rename.success.message"), newName));
        } else {
          showErrorAlert(
              messages.getString("preset.manage.rename.failed.title"),
              messages.getString("preset.manage.rename.failed.message"));
        }
      }
    }
  }

  /** Duplicates the selected preset. */
  private void duplicateSelectedPreset() {
    PresetConfiguration selected = presetsTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }

    String defaultName = selected.getName() + " Copy";

    TextInputDialog dialog = new TextInputDialog(defaultName);
    dialog.setTitle(messages.getString("preset.manage.duplicate.title"));
    dialog.setHeaderText(
        String.format(messages.getString("preset.manage.duplicate.header"), selected.getName()));
    dialog.setContentText(messages.getString("preset.manage.duplicate.content"));

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent()) {
      String newName = result.get().trim();
      if (!newName.isEmpty()) {
        boolean duplicated = presetManager.duplicatePreset(selected.getName(), newName);
        if (duplicated) {
          refreshPresets();
          showInfoAlert(
              messages.getString("preset.manage.duplicate.success.title"),
              String.format(
                  messages.getString("preset.manage.duplicate.success.message"), newName));
        } else {
          showErrorAlert(
              messages.getString("preset.manage.duplicate.failed.title"),
              messages.getString("preset.manage.duplicate.failed.message"));
        }
      }
    }
  }

  /** Deletes the selected preset. */
  private void deleteSelectedPreset() {
    PresetConfiguration selected = presetsTable.getSelectionModel().getSelectedItem();
    if (selected == null || "Default".equals(selected.getName())) {
      return;
    }

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
        showInfoAlert(
            messages.getString("preset.alert.deleted.title"),
            String.format(messages.getString("preset.alert.deleted.message"), selected.getName()));
      } else {
        showErrorAlert(
            messages.getString("preset.alert.delete.failed.title"),
            messages.getString("preset.manage.delete.failed.message"));
      }
    }
  }

  /** Imports presets from JSON files. */
  private void importPresets() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(messages.getString("preset.manage.import.title"));
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(messages.getString("file.filter.json"), "*.json"));

    File file = fileChooser.showOpenDialog(this);
    if (file != null) {
      try {
        presetManager.importAndSavePreset(file.toPath(), true);
        refreshPresets();
        showInfoAlert(
            messages.getString("preset.manage.import.success.title"),
            String.format(
                messages.getString("preset.manage.import.success.message"), file.getName()));

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
    PresetConfiguration selected = presetsTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(messages.getString("preset.export.title"));
    fileChooser.setInitialFileName(selected.getName() + ".json");
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(messages.getString("file.filter.json"), "*.json"));

    File file = fileChooser.showSaveDialog(this);
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
