package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI component that provides real-time preview of pattern matching results. Shows how filenames
 * will be categorized using the current configuration and provides summary statistics and
 * validation feedback. Uses background processing and incremental updates for improved performance.
 */
public class PatternPreviewPane extends VBox {

  private static final int MAX_PREVIEW_ROWS = 100; // Limit displayed rows for performance

  // UI Components
  private final TableView<FilenamePreview> previewTable;
  private final Label summaryLabel;
  private final Label statusLabel;
  private final ListView<String> unmatchedFilesList;
  private final ProgressIndicator progressIndicator;
  private final Button refreshButton;
  private final Button cancelButton;
  private final java.util.ResourceBundle messages;

  // Data
  private final ObservableList<FilenamePreview> previewData;
  private final RuleEngine ruleEngine;
  private PatternConfiguration currentConfig;
  private List<String> currentFilenames;
  private PreviewSummary currentSummary;

  // Background processing
  private BackgroundAnalysisService backgroundService;
  private Task<List<FilenamePreview>> currentTask;

  /**
   * Creates a new pattern preview pane.
   *
   * @param messages i18n messages
   */
  public PatternPreviewPane(java.util.ResourceBundle messages) {
    this.messages = messages;
    this.previewData = FXCollections.observableArrayList();
    this.ruleEngine = new RuleEngine();
    this.currentFilenames = new ArrayList<>();

    // Initialize UI components
    this.previewTable = createPreviewTable();
    this.summaryLabel = createSummaryLabel();
    this.statusLabel = createStatusLabel();
    this.unmatchedFilesList = createUnmatchedFilesList();
    this.progressIndicator = new ProgressIndicator();
    this.refreshButton = createRefreshButton();
    this.cancelButton = createCancelButton();

    // Layout components
    setupLayout();

    // Initial state
    updateSummaryDisplay();
  }

  /**
   * Creates a new pattern preview pane with shared background service.
   *
   * @param backgroundService the background service to use for processing
   * @param messages i18n messages
   */
  public PatternPreviewPane(
      BackgroundAnalysisService backgroundService, java.util.ResourceBundle messages) {
    this(messages);
    this.backgroundService = backgroundService;
  }

  /** Creates and configures the preview table. */
  private TableView<FilenamePreview> createPreviewTable() {
    TableView<FilenamePreview> table = new TableView<>();
    table.setItems(previewData);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setRowFactory(
        tv -> {
          TableRow<FilenamePreview> row = new TableRow<>();
          row.itemProperty()
              .addListener(
                  (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                      updateRowStyle(row, newItem);
                    }
                  });
          return row;
        });

    // Filename column
    TableColumn<FilenamePreview, String> filenameCol =
        new TableColumn<>(messages.getString("preview.column.filename"));
    filenameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
    filenameCol.setPrefWidth(200);

    // Group ID column
    TableColumn<FilenamePreview, String> groupIdCol =
        new TableColumn<>(messages.getString("preview.column.group.id"));
    groupIdCol.setCellValueFactory(new PropertyValueFactory<>("groupId"));
    groupIdCol.setPrefWidth(100);
    groupIdCol.setCellFactory(
        col ->
            new TableCell<FilenamePreview, String>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  setStyle("");
                } else {
                  setText(item);
                  // Highlight missing group IDs
                  if (item.trim().isEmpty()) {
                    setTextFill(Color.RED);
                    setText(messages.getString("common.none"));
                  } else {
                    setTextFill(Color.BLACK);
                  }
                }
              }
            });

    // Role column
    TableColumn<FilenamePreview, ImageRole> roleCol =
        new TableColumn<>(messages.getString("preview.column.role"));
    roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
    roleCol.setPrefWidth(80);
    roleCol.setCellFactory(
        col ->
            new TableCell<FilenamePreview, ImageRole>() {
              @Override
              protected void updateItem(ImageRole item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(messages.getString("common.none"));
                  setTextFill(Color.GRAY);
                } else {
                  setText(item.toString());
                  // Color-code roles
                  switch (item) {
                    case OVERVIEW:
                      setTextFill(Color.BLUE);
                      break;
                    case FRONT:
                      setTextFill(Color.GREEN);
                      break;
                    case REAR:
                      setTextFill(Color.ORANGE);
                      break;
                  }
                }
              }
            });

    // Status column
    TableColumn<FilenamePreview, String> statusCol =
        new TableColumn<>(messages.getString("preview.column.status"));
    statusCol.setCellValueFactory(
        cellData -> {
          FilenamePreview preview = cellData.getValue();
          if (preview.hasError()) {
            return new ReadOnlyObjectWrapper<>(
                String.format(
                    messages.getString("preview.status.error"), preview.getErrorMessage()));
          } else if (preview.isMatched()) {
            return new ReadOnlyObjectWrapper<>(messages.getString("preview.status.matched"));
          } else {
            return new ReadOnlyObjectWrapper<>(messages.getString("preview.status.unmatched"));
          }
        });
    statusCol.setPrefWidth(150);
    statusCol.setCellFactory(
        col ->
            new TableCell<FilenamePreview, String>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  setStyle("");
                } else {
                  setText(item);
                  if (item.startsWith("Error:")) {
                    setTextFill(Color.RED);
                  } else if (item.equals(messages.getString("preview.status.matched"))) {
                    setTextFill(Color.GREEN);
                  } else {
                    setTextFill(Color.GRAY);
                  }
                }
              }
            });

    table.getColumns().add(filenameCol);
    table.getColumns().add(groupIdCol);
    table.getColumns().add(roleCol);
    table.getColumns().add(statusCol);
    return table;
  }

  /** Updates the visual style of a table row based on the preview data. */
  private void updateRowStyle(TableRow<FilenamePreview> row, FilenamePreview preview) {
    row.getStyleClass().removeAll("preview-error", "preview-warning", "preview-success");

    if (preview.hasError()) {
      row.getStyleClass().add("preview-error");
    } else if (!preview.isMatched()) {
      row.getStyleClass().add("preview-warning");
    } else {
      row.getStyleClass().add("preview-success");
    }
  }

  /** Creates the summary label for displaying statistics. */
  private Label createSummaryLabel() {
    Label label = new Label(messages.getString("preview.status.empty"));
    label.setFont(Font.font("System", FontWeight.BOLD, 12));
    label.setWrapText(true);
    return label;
  }

  /** Creates the status label for displaying processing status. */
  private Label createStatusLabel() {
    Label label = new Label(messages.getString("status.ready"));
    label.setFont(Font.font("System", 10));
    label.setTextFill(Color.GRAY);
    return label;
  }

  /** Creates the list view for displaying unmatched files. */
  private ListView<String> createUnmatchedFilesList() {
    ListView<String> listView = new ListView<>();
    listView.setPrefHeight(100);
    listView.setVisible(false);
    listView.setManaged(false);
    return listView;
  }

  /** Creates the refresh button. */
  private Button createRefreshButton() {
    Button button = new Button(messages.getString("preview.button.refresh"));
    button.setOnAction(e -> refreshPreview());
    return button;
  }

  /** Creates the cancel button. */
  private Button createCancelButton() {
    Button button = new Button(messages.getString("preview.button.cancel"));
    button.setVisible(false);
    button.setOnAction(e -> cancelPreviewUpdate());
    return button;
  }

  /** Sets up the layout of the preview pane. */
  private void setupLayout() {
    setSpacing(10);
    setPadding(new Insets(10));

    // Header with summary and controls
    HBox header = new HBox(10);
    header.setAlignment(Pos.CENTER_LEFT);

    VBox summaryBox = new VBox(5);
    summaryBox.getChildren().addAll(summaryLabel, statusLabel);

    HBox controls = new HBox(10);
    controls.setAlignment(Pos.CENTER_RIGHT);
    progressIndicator.setVisible(false);
    progressIndicator.setPrefSize(20, 20);
    controls.getChildren().addAll(progressIndicator, cancelButton, refreshButton);

    header.getChildren().addAll(summaryBox, controls);
    HBox.setHgrow(summaryBox, Priority.ALWAYS);

    // Main table
    VBox.setVgrow(previewTable, Priority.ALWAYS);

    // Unmatched files section (initially hidden)
    Label unmatchedLabel = new Label(messages.getString("preview.label.unmatched"));
    unmatchedLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
    unmatchedLabel.visibleProperty().bind(unmatchedFilesList.visibleProperty());
    unmatchedLabel.managedProperty().bind(unmatchedFilesList.managedProperty());

    getChildren().addAll(header, previewTable, unmatchedLabel, unmatchedFilesList);
  }

  /**
   * Updates the preview with the current configuration and filenames. This method processes
   * filenames in the background to avoid blocking the UI.
   *
   * @param config the pattern configuration to use
   * @param filenames the list of filenames to process
   */
  public void updatePreview(PatternConfiguration config, List<String> filenames) {
    if (config == null) {
      config = new PatternConfiguration();
    }
    if (filenames == null) {
      filenames = new ArrayList<>();
    }

    this.currentConfig = config;
    this.currentFilenames = new ArrayList<>(filenames);

    // Cancel any running task
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }

    // Clear current data
    previewData.clear();
    updateSummaryDisplay();

    if (filenames.isEmpty()) {
      setStatus(messages.getString("preview.status.empty"));
      return;
    }

    // Limit the number of files for performance
    List<String> limitedFilenames =
        filenames.size() > MAX_PREVIEW_ROWS ? filenames.subList(0, MAX_PREVIEW_ROWS) : filenames;

    // Use background service if available, otherwise fallback to local processing
    if (backgroundService != null) {
      currentTask =
          backgroundService.createPreviewUpdateTask(config, limitedFilenames, MAX_PREVIEW_ROWS);
    } else {
      currentTask = createPreviewTask(config, limitedFilenames);
    }

    // Bind UI to task
    progressIndicator.progressProperty().bind(currentTask.progressProperty());
    statusLabel.textProperty().bind(currentTask.messageProperty());

    // Handle task completion
    currentTask.setOnSucceeded(
        e -> {
          List<FilenamePreview> previews = currentTask.getValue();
          if (previews != null) {
            previewData.setAll(previews);
          }
          currentSummary = new PreviewSummary(previews, messages);
          updateSummaryDisplay();

          resetPreviewUI();

          if (currentFilenames.size() > MAX_PREVIEW_ROWS) {
            setStatus(
                String.format(
                    messages.getString("preview.status.showing.first"),
                    MAX_PREVIEW_ROWS,
                    currentFilenames.size()));
          } else {
            setStatus(messages.getString("preview.status.updated"));
          }
        });

    currentTask.setOnFailed(
        e -> {
          resetPreviewUI();
          setStatus(
              String.format(
                  messages.getString("preview.status.failed"),
                  currentTask.getException().getMessage()));
        });

    currentTask.setOnCancelled(
        e -> {
          resetPreviewUI();
          setStatus(messages.getString("preview.status.cancelled"));
        });

    // Start processing
    setPreviewUIRunning();

    if (backgroundService != null) {
      backgroundService.executePreviewTask(currentTask);
    } else {
      Thread previewThread = new Thread(currentTask);
      previewThread.setDaemon(true);
      previewThread.start();
    }
  }

  /**
   * Creates a background task to process filenames and generate previews. This is a fallback method
   * when no background service is available.
   */
  private Task<List<FilenamePreview>> createPreviewTask(
      PatternConfiguration config, List<String> filenames) {
    return new Task<List<FilenamePreview>>() {
      @Override
      protected List<FilenamePreview> call() throws Exception {
        updateMessage(messages.getString("preview.task.processing"));
        updateProgress(0, 100);

        List<FilenamePreview> previews = new ArrayList<>();

        for (int i = 0; i < filenames.size(); i++) {
          if (isCancelled()) {
            break;
          }

          String filename = filenames.get(i);
          FilenamePreview preview = processFilename(filename, config);
          previews.add(preview);

          // Update progress
          updateProgress(i + 1, filenames.size());
          updateMessage(
              String.format(
                  messages.getString("preview.task.processed.count"), i + 1, filenames.size()));
        }

        updateMessage(messages.getString("preview.task.completed"));
        updateProgress(100, 100);
        return previews;
      }
    };
  }

  /** Processes a single filename and creates a preview entry. */
  private FilenamePreview processFilename(String filename, PatternConfiguration config) {
    FilenamePreview preview = new FilenamePreview(filename);

    try {
      // Extract group ID
      String groupId = extractGroupId(filename, config.getGroupPattern());

      // Classify role
      ImageRole role = null;
      if (!config.getRoleRules().isEmpty()) {
        role = ruleEngine.classifyFilename(filename, config.getRoleRules());
      }

      // Determine if processing was successful
      boolean matched = groupId != null && !groupId.trim().isEmpty();

      if (matched) {
        preview.setSuccess(groupId, role);
      } else {
        preview.setError(messages.getString("preview.error.no.group.id"));
      }

    } catch (Exception e) {
      preview.setError(
          String.format(messages.getString("preview.error.processing"), e.getMessage()));
    }

    return preview;
  }

  /** Extracts the group ID from a filename using the group pattern. */
  private String extractGroupId(String filename, String groupPattern) {
    if (groupPattern == null || groupPattern.trim().isEmpty()) {
      return null;
    }

    try {
      Pattern pattern = Pattern.compile(groupPattern);
      Matcher matcher = pattern.matcher(filename);

      if (matcher.find() && matcher.groupCount() >= 1) {
        return matcher.group(1);
      }
    } catch (PatternSyntaxException e) {
      // Invalid regex pattern
      return null;
    }

    return null;
  }

  /** Updates the summary display with current statistics. */
  private void updateSummaryDisplay() {
    if (currentSummary == null) {
      summaryLabel.setText(messages.getString("preview.status.empty"));
      unmatchedFilesList.setVisible(false);
      unmatchedFilesList.setManaged(false);
      return;
    }

    // Update summary text
    summaryLabel.setText(currentSummary.getSummaryText());

    // Update summary label color based on health
    if (currentSummary.hasErrors()) {
      summaryLabel.setTextFill(Color.RED);
    } else if (currentSummary.hasWarnings()) {
      summaryLabel.setTextFill(Color.ORANGE);
    } else {
      summaryLabel.setTextFill(Color.GREEN);
    }

    // Update unmatched files list
    List<String> unmatchedFiles = currentSummary.getUnmatchedFilenames();
    if (!unmatchedFiles.isEmpty() && unmatchedFiles.size() <= 20) {
      unmatchedFilesList.setItems(FXCollections.observableArrayList(unmatchedFiles));
      unmatchedFilesList.setVisible(true);
      unmatchedFilesList.setManaged(true);
    } else {
      unmatchedFilesList.setVisible(false);
      unmatchedFilesList.setManaged(false);
    }
  }

  /** Sets the status message. */
  private void setStatus(String status) {
    // Ensure we are not setting text while bound to a task message
    if (statusLabel.textProperty().isBound()) {
      statusLabel.textProperty().unbind();
    }
    statusLabel.setText(status);
  }

  /** Refreshes the preview with the current configuration and filenames. */
  public void refreshPreview() {
    if (currentConfig != null && currentFilenames != null) {
      updatePreview(currentConfig, currentFilenames);
    }
  }

  /**
   * @return the current preview summary, or null if no preview has been generated
   */
  public PreviewSummary getPreviewSummary() {
    return currentSummary;
  }

  /**
   * @return the list of filename previews currently displayed
   */
  public List<FilenamePreview> getPreviewData() {
    return new ArrayList<>(previewData);
  }

  /** Clears the preview data and resets the display. */
  public void clearPreview() {
    previewData.clear();
    currentSummary = null;
    currentConfig = null;
    currentFilenames = new ArrayList<>();
    updateSummaryDisplay();
    setStatus("Ready");
  }

  /**
   * Updates the preview using a GroupingResult from the GroupingEngine. This provides more accurate
   * preview with proper role assignment and unmatched reasons.
   *
   * @param result the grouping result from GroupingEngine
   * @param allFilenames all filenames that were processed
   */
  public void updatePreviewWithGroupingResult(
      GroupingEngine.GroupingResult result, List<String> allFilenames) {
    // Cancel any running task
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }

    // Clear current data
    previewData.clear();

    List<FilenamePreview> previews = new ArrayList<>();

    // Process matched files
    for (String filename : result.getFileToGroupId().keySet()) {
      String groupId = result.getFileToGroupId().get(filename);
      ImageRole role = result.getFileToRole().get(filename);

      FilenamePreview preview = new FilenamePreview(filename);
      preview.setSuccess(groupId, role);

      previews.add(preview);
    }

    // Process unmatched files
    for (String filename : result.getUnmatchedFiles()) {
      String reason = result.getUnmatchedReasons().get(filename);

      FilenamePreview preview = new FilenamePreview(filename);
      preview.setError(reason != null ? reason : messages.getString("preview.error.no.match"));

      previews.add(preview);
    }

    // Limit for performance
    if (previews.size() > MAX_PREVIEW_ROWS) {
      previews = previews.subList(0, MAX_PREVIEW_ROWS);
    }

    // Update UI
    final List<FilenamePreview> finalPreviews = previews;
    Platform.runLater(
        () -> {
          previewData.setAll(finalPreviews);
          currentSummary = new PreviewSummary(finalPreviews, messages);
          updateSummaryDisplay();

          // Update unmatched files list
          List<String> unmatchedFiles = result.getUnmatchedFiles();
          if (!unmatchedFiles.isEmpty()) {
            unmatchedFilesList.getItems().setAll(unmatchedFiles);
            unmatchedFilesList.setVisible(true);
            unmatchedFilesList.setManaged(true);
          } else {
            unmatchedFilesList.setVisible(false);
            unmatchedFilesList.setManaged(false);
          }

          if (allFilenames.size() > MAX_PREVIEW_ROWS) {
            setStatus(
                String.format(
                    messages.getString("preview.status.showing.first"),
                    MAX_PREVIEW_ROWS,
                    allFilenames.size()));
          } else {
            setStatus(messages.getString("preview.status.updated.grouping"));
          }
        });
  }

  /** Cancels the current preview update operation. */
  private void cancelPreviewUpdate() {
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }
    if (backgroundService != null) {
      backgroundService.cancelAllTasks();
    }
  }

  /** Sets UI to running state during preview processing. */
  private void setPreviewUIRunning() {
    progressIndicator.setVisible(true);
    cancelButton.setVisible(true);
    refreshButton.setDisable(true);
  }

  /** Resets UI to ready state after preview completion. */
  private void resetPreviewUI() {
    progressIndicator.setVisible(false);
    progressIndicator.progressProperty().unbind();
    statusLabel.textProperty().unbind();
    cancelButton.setVisible(false);
    refreshButton.setDisable(false);
  }

  /**
   * Sets the background service to use for processing.
   *
   * @param backgroundService the background service
   */
  public void setBackgroundService(BackgroundAnalysisService backgroundService) {
    this.backgroundService = backgroundService;
  }

  /**
   * Cleanup method to shut down background processing. Should be called when the preview pane is no
   * longer needed.
   */
  public void shutdown() {
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }
    // Don't shutdown the shared background service here
    // It will be managed by the FileAnalysisPane
  }
}
