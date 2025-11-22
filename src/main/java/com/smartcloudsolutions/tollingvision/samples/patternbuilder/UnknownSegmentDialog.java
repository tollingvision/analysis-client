package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for handling unknown segments in filenames. Allows users to label unknown segments as
 * ignored, custom tokens, or free text.
 */
public class UnknownSegmentDialog extends Stage {

  private final ResourceBundle messages;
  private final UnknownSegmentHandler segmentHandler;
  private final List<String> unknownSegments;

  private boolean confirmed = false;

  /**
   * Creates a new UnknownSegmentDialog.
   *
   * @param unknownSegments list of unknown segments to handle
   * @param segmentHandler the segment handler to update
   * @param messages resource bundle for i18n
   */
  public UnknownSegmentDialog(
      List<String> unknownSegments, UnknownSegmentHandler segmentHandler, ResourceBundle messages) {
    this.unknownSegments = unknownSegments;
    this.segmentHandler = segmentHandler;
    this.messages = messages;

    initializeDialog();
    createContent();
  }

  /** Initializes the dialog properties. */
  private void initializeDialog() {
    setTitle(messages.getString("unknown.segments.dialog.title"));
    initModality(Modality.APPLICATION_MODAL);
    setResizable(true);
    setMinWidth(600);
    setMinHeight(400);
  }

  /** Creates the dialog content. */
  private void createContent() {
    VBox root = new VBox(15);
    root.setPadding(new Insets(20));

    // Header
    Label titleLabel = new Label(messages.getString("unknown.segments.dialog.header"));
    titleLabel.getStyleClass().add("dialog-header");

    Label descriptionLabel = new Label(messages.getString("unknown.segments.dialog.description"));
    descriptionLabel.setWrapText(true);
    descriptionLabel.getStyleClass().add("dialog-description");

    // Segments grid
    ScrollPane scrollPane = new ScrollPane();
    GridPane segmentsGrid = createSegmentsGrid();
    scrollPane.setContent(segmentsGrid);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(300);

    // Buttons
    HBox buttonBox = createButtonBox();

    root.getChildren().addAll(titleLabel, descriptionLabel, scrollPane, buttonBox);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    Scene scene = new Scene(root);
    setScene(scene);
  }

  /** Creates the grid for segment handling options. */
  private GridPane createSegmentsGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10));

    // Headers
    grid.add(new Label(messages.getString("unknown.segments.column.segment")), 0, 0);
    grid.add(new Label(messages.getString("unknown.segments.column.action")), 1, 0);
    grid.add(new Label(messages.getString("unknown.segments.column.label")), 2, 0);

    // Add row for each unknown segment
    for (int i = 0; i < unknownSegments.size(); i++) {
      String segment = unknownSegments.get(i);
      int row = i + 1;

      // Segment value
      Label segmentLabel = new Label(segment);
      segmentLabel.getStyleClass().add("segment-value");
      grid.add(segmentLabel, 0, row);

      // Action selector
      ComboBox<UnknownSegmentHandler.SegmentAction> actionCombo = new ComboBox<>();
      actionCombo.getItems().addAll(UnknownSegmentHandler.SegmentAction.values());
      actionCombo.setValue(UnknownSegmentHandler.SegmentAction.IGNORE); // Default

      // Custom cell factory for action descriptions
      actionCombo.setCellFactory(
          listView ->
              new ListCell<UnknownSegmentHandler.SegmentAction>() {
                @Override
                protected void updateItem(UnknownSegmentHandler.SegmentAction item, boolean empty) {
                  super.updateItem(item, empty);
                  if (empty || item == null) {
                    setText(null);
                  } else {
                    setText(item.getDescription());
                  }
                }
              });

      actionCombo.setButtonCell(
          new ListCell<UnknownSegmentHandler.SegmentAction>() {
            @Override
            protected void updateItem(UnknownSegmentHandler.SegmentAction item, boolean empty) {
              super.updateItem(item, empty);
              if (empty || item == null) {
                setText(null);
              } else {
                setText(item.getDescription());
              }
            }
          });

      grid.add(actionCombo, 1, row);

      // Custom label field
      TextField labelField = new TextField();
      labelField.setPromptText(messages.getString("unknown.segments.custom.label.placeholder"));
      labelField.setDisable(true); // Initially disabled

      // Enable/disable label field based on action selection
      actionCombo
          .valueProperty()
          .addListener(
              (obs, oldVal, newVal) -> {
                labelField.setDisable(newVal != UnknownSegmentHandler.SegmentAction.CUSTOM_TOKEN);
              });

      grid.add(labelField, 2, row);

      // Store references for later retrieval
      actionCombo.setUserData(segment);
      labelField.setUserData(segment);
    }

    // Set column constraints
    grid.getColumnConstraints()
        .addAll(
            new javafx.scene.layout.ColumnConstraints(150), // Segment column
            new javafx.scene.layout.ColumnConstraints(200), // Action column
            new javafx.scene.layout.ColumnConstraints(150) // Label column
            );
    grid.getColumnConstraints().get(2).setHgrow(Priority.ALWAYS);

    return grid;
  }

  /** Creates the button box with OK and Cancel buttons. */
  private HBox createButtonBox() {
    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);

    Button cancelButton = new Button(messages.getString("button.cancel"));
    cancelButton.setOnAction(
        e -> {
          confirmed = false;
          close();
        });

    Button okButton = new Button(messages.getString("button.ok"));
    okButton.setDefaultButton(true);
    okButton.setOnAction(
        e -> {
          applySegmentLabels();
          confirmed = true;
          close();
        });

    buttonBox.getChildren().addAll(cancelButton, okButton);
    return buttonBox;
  }

  /** Applies the segment labels based on user selections. */
  @SuppressWarnings("unchecked")
  private void applySegmentLabels() {
    Scene scene = getScene();
    if (scene == null) return;

    // Find all ComboBoxes and TextFields in the scene
    scene
        .getRoot()
        .lookupAll(".combo-box")
        .forEach(
            node -> {
              if (node instanceof ComboBox) {
                ComboBox<UnknownSegmentHandler.SegmentAction> combo =
                    (ComboBox<UnknownSegmentHandler.SegmentAction>) node;
                String segment = (String) combo.getUserData();
                if (segment != null) {
                  UnknownSegmentHandler.SegmentAction action = combo.getValue();

                  // Find corresponding label field
                  final String[] customLabel = {""};
                  scene
                      .getRoot()
                      .lookupAll(".text-field")
                      .forEach(
                          labelNode -> {
                            if (labelNode instanceof TextField) {
                              TextField labelField = (TextField) labelNode;
                              if (segment.equals(labelField.getUserData())) {
                                customLabel[0] = labelField.getText();
                              }
                            }
                          });

                  // Apply the label
                  segmentHandler.labelSegment(segment, action, customLabel[0]);
                }
              }
            });
  }

  /**
   * Shows the dialog and returns whether the user confirmed the changes.
   *
   * @return true if the user clicked OK, false if cancelled
   */
  public boolean showAndWaitForConfirmation() {
    showAndWait();
    return confirmed;
  }
}
