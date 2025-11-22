package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Dialog for saving a new preset configuration. Allows the user to specify a name and description
 * for the preset.
 */
public class SavePresetDialog extends Dialog<SavePresetDialog.PresetInfo> {
  private final ResourceBundle messages;

  /** Container for preset information entered by the user. */
  public static class PresetInfo {
    private final String name;
    private final String description;

    public PresetInfo(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  private TextField nameField;
  private TextArea descriptionArea;

  /** Creates a new SavePresetDialog. */
  public SavePresetDialog() {
    this(java.util.ResourceBundle.getBundle("messages"));
  }

  public SavePresetDialog(ResourceBundle messages) {
    this.messages = messages;
    initializeDialog();
    initializeComponents();
    setupLayout();
    setupValidation();
  }

  /** Initializes the dialog properties. */
  private void initializeDialog() {
    setTitle(messages.getString("save.preset.title"));
    setHeaderText(messages.getString("save.preset.header"));
    setResizable(true);

    // Add OK and Cancel buttons
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    // Set result converter
    setResultConverter(
        buttonType -> {
          if (buttonType == ButtonType.OK) {
            return new PresetInfo(nameField.getText().trim(), descriptionArea.getText().trim());
          }
          return null;
        });
  }

  /** Initializes all UI components. */
  private void initializeComponents() {
    nameField = new TextField();
    nameField.setPromptText(messages.getString("save.preset.name.placeholder"));
    nameField.setPrefColumnCount(20);

    descriptionArea = new TextArea();
    descriptionArea.setPromptText(messages.getString("save.preset.description.placeholder"));
    descriptionArea.setPrefRowCount(3);
    descriptionArea.setPrefColumnCount(20);
    descriptionArea.setWrapText(true);
  }

  /** Sets up the layout structure. */
  private void setupLayout() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    // Name field
    Label nameLabel = new Label(messages.getString("save.preset.label.name"));
    grid.add(nameLabel, 0, 0);
    grid.add(nameField, 1, 0);

    // Description field
    Label descriptionLabel = new Label(messages.getString("save.preset.label.description"));
    grid.add(descriptionLabel, 0, 1);
    grid.add(descriptionArea, 1, 1);

    // Make description area grow
    GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
    GridPane.setVgrow(descriptionArea, Priority.ALWAYS);

    getDialogPane().setContent(grid);

    // Request focus on name field
    nameField.requestFocus();
  }

  /** Sets up validation for the input fields. */
  private void setupValidation() {
    // Disable OK button initially
    Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
    okButton.setDisable(true);

    // Enable OK button only when name is not empty
    nameField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              okButton.setDisable(newValue == null || newValue.trim().isEmpty());
            });

    // Add validation tooltip
    nameField.setTooltip(new Tooltip(messages.getString("save.preset.validation.name.required")));
  }

  /**
   * Sets the initial name for the preset.
   *
   * @param name the initial name
   */
  public void setInitialName(String name) {
    if (name != null) {
      nameField.setText(name);
    }
  }

  /**
   * Sets the initial description for the preset.
   *
   * @param description the initial description
   */
  public void setInitialDescription(String description) {
    if (description != null) {
      descriptionArea.setText(description);
    }
  }
}
