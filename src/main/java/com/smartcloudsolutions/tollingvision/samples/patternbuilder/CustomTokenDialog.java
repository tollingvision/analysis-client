package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for managing custom tokens when switching into Simple mode. Allows users to review and
 * configure custom tokens that will be available in the Token Selection screen.
 */
public class CustomTokenDialog extends Stage {

  private final CustomTokenManager customTokenManager;
  private final java.util.ResourceBundle messages;
  private boolean confirmed = false;
  private Runnable onTokenUpdated;

  /**
   * Creates a new CustomTokenDialog.
   *
   * @param customTokenManager the custom token manager
   * @param messages the resource bundle for i18n
   */
  public CustomTokenDialog(
      CustomTokenManager customTokenManager, java.util.ResourceBundle messages) {
    this.customTokenManager = customTokenManager;
    this.messages = messages;

    initializeDialog();
    createContent();
  }

  /**
   * Sets the callback to be called when tokens are updated.
   *
   * @param onTokenUpdated the callback to call when tokens are updated
   */
  public void setOnTokenUpdated(Runnable onTokenUpdated) {
    this.onTokenUpdated = onTokenUpdated;
  }

  /** Initializes the dialog properties. */
  private void initializeDialog() {
    setTitle(messages.getString("custom.tokens.title"));
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
    Label titleLabel = new Label(messages.getString("custom.tokens.title"));
    titleLabel.getStyleClass().add("dialog-header");
    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    Label descriptionLabel = new Label(messages.getString("custom.tokens.description"));
    descriptionLabel.setWrapText(true);
    descriptionLabel.getStyleClass().add("dialog-description");

    // Custom tokens list
    ListView<CustomTokenManager.CustomToken> tokenListView = createTokenListView();

    // Buttons for managing tokens
    HBox tokenButtonBox = createTokenButtonBox(tokenListView);

    // Dialog buttons
    HBox dialogButtonBox = createDialogButtonBox();

    root.getChildren()
        .addAll(
            titleLabel,
            descriptionLabel,
            new Label(messages.getString("custom.tokens.configured.title")),
            tokenListView,
            tokenButtonBox,
            dialogButtonBox);

    VBox.setVgrow(tokenListView, Priority.ALWAYS);

    Scene scene = new Scene(root);
    setScene(scene);
  }

  /** Creates the token list view. */
  private ListView<CustomTokenManager.CustomToken> createTokenListView() {
    ListView<CustomTokenManager.CustomToken> listView = new ListView<>();
    listView.setPrefHeight(200);

    // Custom cell factory to show token details
    listView.setCellFactory(
        param ->
            new ListCell<CustomTokenManager.CustomToken>() {
              @Override
              protected void updateItem(CustomTokenManager.CustomToken token, boolean empty) {
                super.updateItem(token, empty);
                if (empty || token == null) {
                  setText(null);
                  setGraphic(null);
                } else {
                  VBox content = new VBox(2);

                  Label nameLabel = new Label(token.getName());
                  nameLabel.setStyle("-fx-font-weight: bold;");

                  Label descLabel = new Label(token.getDescription());
                  descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

                  Label examplesLabel =
                      new Label(
                          String.format(
                              messages.getString("custom.tokens.examples.label"),
                              String.join(", ", token.getExamples())));
                  examplesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: darkgray;");

                  content.getChildren().addAll(nameLabel, descLabel, examplesLabel);
                  setGraphic(content);
                }
              }
            });

    // Populate with existing custom tokens
    listView.getItems().addAll(customTokenManager.getAllCustomTokens());

    return listView;
  }

  /** Creates the button box for token management. */
  private HBox createTokenButtonBox(ListView<CustomTokenManager.CustomToken> listView) {
    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_LEFT);

    Button addButton = new Button(messages.getString("custom.tokens.add"));
    Button removeButton = new Button(messages.getString("custom.tokens.remove"));
    Button loadDefaultsButton = new Button(messages.getString("custom.tokens.load.defaults"));

    addButton.setOnAction(e -> showAddTokenDialog(listView));

    removeButton.setOnAction(
        e -> {
          CustomTokenManager.CustomToken selected = listView.getSelectionModel().getSelectedItem();
          if (selected != null) {
            customTokenManager.removeCustomToken(selected.getName());
            customTokenManager.saveCustomTokens();
            listView.getItems().remove(selected);

            // Trigger refresh callback
            if (onTokenUpdated != null) {
              onTokenUpdated.run();
            }
          }
        });

    loadDefaultsButton.setOnAction(
        e -> {
          customTokenManager.loadPreconfiguredCustomTokens();
          customTokenManager.saveCustomTokens();
          listView.getItems().clear();
          listView.getItems().addAll(customTokenManager.getAllCustomTokens());

          // Trigger refresh callback
          if (onTokenUpdated != null) {
            onTokenUpdated.run();
          }
        });

    // Enable/disable remove button based on selection
    removeButton
        .disableProperty()
        .bind(listView.getSelectionModel().selectedItemProperty().isNull());

    buttonBox.getChildren().addAll(addButton, removeButton, loadDefaultsButton);
    return buttonBox;
  }

  /** Shows the add token dialog. */
  private void showAddTokenDialog(ListView<CustomTokenManager.CustomToken> listView) {
    Dialog<CustomTokenManager.CustomToken> dialog = new Dialog<>();
    dialog.setTitle(messages.getString("custom.tokens.add.dialog.title"));
    dialog.setHeaderText(messages.getString("custom.tokens.add.dialog.header"));

    // Create form
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField nameField = new TextField();
    nameField.setPromptText(messages.getString("custom.tokens.prompt.name"));

    TextField descriptionField = new TextField();
    descriptionField.setPromptText(messages.getString("custom.tokens.prompt.description"));

    TextField examplesField = new TextField();
    examplesField.setPromptText(messages.getString("custom.tokens.prompt.examples"));

    ComboBox<TokenType> typeCombo = new ComboBox<>();
    typeCombo.getItems().addAll(TokenType.values());
    typeCombo.setValue(TokenType.SUFFIX);

    grid.add(new Label(messages.getString("custom.tokens.field.name")), 0, 0);
    grid.add(nameField, 1, 0);
    grid.add(new Label(messages.getString("custom.tokens.field.description")), 0, 1);
    grid.add(descriptionField, 1, 1);
    grid.add(new Label(messages.getString("custom.tokens.field.examples")), 0, 2);
    grid.add(examplesField, 1, 2);
    grid.add(new Label(messages.getString("custom.tokens.field.type")), 0, 3);
    grid.add(typeCombo, 1, 3);

    dialog.getDialogPane().setContent(grid);

    ButtonType addButtonType =
        new ButtonType(messages.getString("button.add"), ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

    // Enable/disable add button based on input
    javafx.scene.Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
    addButton.setDisable(true);

    nameField
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              addButton.setDisable(newText.trim().isEmpty());
            });

    dialog.setResultConverter(
        dialogButton -> {
          if (dialogButton == addButtonType) {
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();
            String examplesText = examplesField.getText().trim();
            TokenType type = typeCombo.getValue();

            if (!name.isEmpty()) {
              java.util.Set<String> examples =
                  examplesText.isEmpty()
                      ? java.util.Set.of()
                      : java.util.Set.of(examplesText.split(",\\s*"));

              return new CustomTokenManager.CustomToken(name, description, examples, type);
            }
          }
          return null;
        });

    dialog
        .showAndWait()
        .ifPresent(
            token -> {
              customTokenManager.addCustomToken(token);
              customTokenManager.saveCustomTokens();
              listView.getItems().clear();
              listView.getItems().addAll(customTokenManager.getAllCustomTokens());

              // Trigger refresh callback
              if (onTokenUpdated != null) {
                onTokenUpdated.run();
              }
            });
  }

  /** Creates the dialog button box. */
  private HBox createDialogButtonBox() {
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
          confirmed = true;
          close();
        });

    buttonBox.getChildren().addAll(cancelButton, okButton);
    return buttonBox;
  }

  /**
   * Shows the dialog and returns whether the user confirmed.
   *
   * @return true if the user clicked OK, false if cancelled
   */
  public boolean showAndWaitForConfirmation() {
    showAndWait();
    return confirmed;
  }
}
