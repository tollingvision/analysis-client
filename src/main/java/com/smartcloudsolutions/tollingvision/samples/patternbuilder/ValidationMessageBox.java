package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A validation message box that displays validation errors and warnings. Automatically hides itself
 * when there are no errors or warnings to show, ensuring no empty containers are displayed in the
 * UI.
 */
public class ValidationMessageBox extends VBox {

  private final ValidationModel validationModel;
  private final Label summaryLabel;
  private final TitledPane errorPane;
  private final TitledPane warningPane;
  private final VBox errorContent;
  private final VBox warningContent;

  // Properties for controlling visibility
  private final BooleanProperty showWhenEmpty = new SimpleBooleanProperty(false);
  private final BooleanProperty hasBeenActivated = new SimpleBooleanProperty(false);

  private final java.util.ResourceBundle messages;

  /**
   * Creates a new ValidationMessageBox bound to the specified ValidationModel.
   *
   * @param validationModel the validation model to observe
   * @param messages the resource bundle for i18n strings
   */
  public ValidationMessageBox(ValidationModel validationModel, java.util.ResourceBundle messages) {
    this.validationModel = validationModel;
    this.messages = messages;

    // Initialize components
    this.summaryLabel = createSummaryLabel();
    this.errorContent = new VBox(5);
    this.warningContent = new VBox(5);
    this.errorPane = createErrorPane();
    this.warningPane = createWarningPane();

    setupLayout();
    setupBindings();
  }

  /** Creates the summary label for displaying overall validation status. */
  private Label createSummaryLabel() {
    Label label = new Label();
    label.setFont(Font.font("System", FontWeight.BOLD, 12));
    label.setWrapText(true);
    label.setAlignment(Pos.CENTER_LEFT);
    return label;
  }

  /**
   * Creates the error pane for displaying validation errors.
   *
   * @return the configured error pane
   */
  private TitledPane createErrorPane() {
    TitledPane pane = new TitledPane(messages.getString("validation.title.errors"), errorContent);
    pane.setExpanded(true);
    pane.getStyleClass().add("validation-error-pane");
    return pane;
  }

  /**
   * Creates the warning pane for displaying validation warnings.
   *
   * @return the configured warning pane
   */
  private TitledPane createWarningPane() {
    TitledPane pane =
        new TitledPane(messages.getString("validation.title.warnings"), warningContent);
    pane.setExpanded(false);
    pane.getStyleClass().add("validation-warning-pane");
    return pane;
  }

  /** Sets up the layout of the validation message box. */
  private void setupLayout() {
    setSpacing(10);
    setPadding(new Insets(10));
    getStyleClass().add("validation-message-box");

    // Add components
    getChildren().addAll(summaryLabel, errorPane, warningPane);
  }

  /** Sets up bindings to the validation model. */
  private void setupBindings() {
    // Bind summary label text and style
    summaryLabel.textProperty().bind(validationModel.validationSummaryProperty());

    // Update summary label color based on validation state
    validationModel.hasErrorsProperty().addListener((obs, oldVal, newVal) -> updateSummaryStyle());
    validationModel
        .hasWarningsProperty()
        .addListener((obs, oldVal, newVal) -> updateSummaryStyle());
    validationModel.isValidProperty().addListener((obs, oldVal, newVal) -> updateSummaryStyle());

    // Bind error pane visibility and content
    errorPane.visibleProperty().bind(validationModel.hasErrorsProperty());
    errorPane.managedProperty().bind(validationModel.hasErrorsProperty());

    // Bind warning pane visibility and content
    warningPane.visibleProperty().bind(validationModel.hasWarningsProperty());
    warningPane.managedProperty().bind(validationModel.hasWarningsProperty());

    // Update error content when errors change
    validationModel
        .getErrors()
        .addListener(
            (javafx.collections.ListChangeListener<ValidationError>)
                c -> {
                  updateErrorContent();
                });

    // Update warning content when warnings change
    validationModel
        .getWarnings()
        .addListener(
            (javafx.collections.ListChangeListener<ValidationWarning>)
                c -> {
                  updateWarningContent();
                });

    // Hide entire message box when there are no errors or warnings (unless
    // showWhenEmpty is true) AND only show after activation (after Generate button
    // press)
    BooleanProperty hasAnyMessages = new SimpleBooleanProperty();
    hasAnyMessages.bind(
        Bindings.or(validationModel.hasErrorsProperty(), validationModel.hasWarningsProperty()));

    BooleanProperty shouldShow = new SimpleBooleanProperty();
    shouldShow.bind(Bindings.and(hasBeenActivated, Bindings.or(hasAnyMessages, showWhenEmpty)));

    visibleProperty().bind(shouldShow);
    managedProperty().bind(shouldShow);

    // Initial updates
    updateSummaryStyle();
    updateErrorContent();
    updateWarningContent();
  }

  /** Updates the summary label style based on validation state. */
  private void updateSummaryStyle() {
    summaryLabel
        .getStyleClass()
        .removeAll("validation-success", "validation-warning", "validation-error");

    if (validationModel.hasErrors()) {
      summaryLabel.setTextFill(Color.RED);
      summaryLabel.getStyleClass().add("validation-error");
    } else if (validationModel.hasWarnings()) {
      // Warnings without errors should show as warning, not success
      summaryLabel.setTextFill(Color.ORANGE);
      summaryLabel.getStyleClass().add("validation-warning");
    } else if (validationModel.isValid()) {
      // Only show green if truly valid (no errors AND no warnings)
      summaryLabel.setTextFill(Color.GREEN);
      summaryLabel.getStyleClass().add("validation-success");
    } else {
      summaryLabel.setTextFill(Color.GRAY);
    }
  }

  /** Updates the error content display. */
  private void updateErrorContent() {
    errorContent.getChildren().clear();

    for (int i = 0; i < validationModel.getErrors().size(); i++) {
      ValidationError error = validationModel.getErrors().get(i);

      Label errorLabel = new Label(String.format("%d. %s", i + 1, error.getMessage()));
      errorLabel.setWrapText(true);
      errorLabel.setTextFill(Color.DARKRED);
      errorLabel.getStyleClass().add("validation-error-item");

      errorContent.getChildren().add(errorLabel);

      // Add context if available
      if (error.hasContext()) {
        Label contextLabel =
            new Label(
                "   " + messages.getString("validation.context.label") + ": " + error.getContext());
        contextLabel.setWrapText(true);
        contextLabel.setTextFill(Color.GRAY);
        contextLabel.setFont(Font.font("System", 10));
        contextLabel.getStyleClass().add("validation-error-context");

        errorContent.getChildren().add(contextLabel);
      }

      // Add fix recommendations if available
      java.util.List<String> fixes = ErrorGuidanceProvider.getFixRecommendations(error);
      if (!fixes.isEmpty()) {
        Label fixesLabel = new Label("   " + messages.getString("validation.fixes.label") + ":");
        fixesLabel.setTextFill(Color.DARKBLUE);
        fixesLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        fixesLabel.getStyleClass().add("validation-error-fixes-header");

        errorContent.getChildren().add(fixesLabel);

        for (String fix : fixes) {
          Label fixLabel = new Label("   " + messages.getString("validation.bullet") + " " + fix);
          fixLabel.setWrapText(true);
          fixLabel.setTextFill(Color.DARKBLUE);
          fixLabel.setFont(Font.font("System", 10));
          fixLabel.getStyleClass().add("validation-error-fix");

          errorContent.getChildren().add(fixLabel);
        }
      }

      // Add spacing between errors
      if (i < validationModel.getErrors().size() - 1) {
        errorContent.getChildren().add(new Label(" "));
      }
    }
  }

  /** Updates the warning content display. */
  private void updateWarningContent() {
    warningContent.getChildren().clear();

    for (int i = 0; i < validationModel.getWarnings().size(); i++) {
      ValidationWarning warning = validationModel.getWarnings().get(i);

      Label warningLabel = new Label(String.format("%d. %s", i + 1, warning.getMessage()));
      warningLabel.setWrapText(true);
      warningLabel.setTextFill(Color.DARKORANGE);
      warningLabel.getStyleClass().add("validation-warning-item");

      warningContent.getChildren().add(warningLabel);

      // Add context if available
      if (warning.hasContext()) {
        Label contextLabel =
            new Label(
                "   "
                    + messages.getString("validation.context.label")
                    + ": "
                    + warning.getContext());
        contextLabel.setWrapText(true);
        contextLabel.setTextFill(Color.GRAY);
        contextLabel.setFont(Font.font("System", 10));
        contextLabel.getStyleClass().add("validation-warning-context");

        warningContent.getChildren().add(contextLabel);
      }

      // Add spacing between warnings
      if (i < validationModel.getWarnings().size() - 1) {
        warningContent.getChildren().add(new Label(" "));
      }
    }
  }

  /**
   * Sets whether the message box should be shown even when there are no errors or warnings.
   *
   * @param show true to show the box even when empty, false to hide when empty
   */
  public void setShowWhenEmpty(boolean show) {
    showWhenEmpty.set(show);
  }

  /**
   * @return true if the message box is configured to show when empty
   */
  public boolean isShowWhenEmpty() {
    return showWhenEmpty.get();
  }

  /**
   * Property for controlling whether to show the message box when empty.
   *
   * @return the showWhenEmpty property
   */
  public BooleanProperty showWhenEmptyProperty() {
    return showWhenEmpty;
  }

  /**
   * @return true if the message box should currently be hidden due to empty state
   */
  public boolean shouldHide() {
    return !validationModel.hasErrors() && !validationModel.hasWarnings() && !showWhenEmpty.get();
  }

  /** Forces a refresh of the message box content. */
  public void refresh() {
    updateSummaryStyle();
    updateErrorContent();
    updateWarningContent();
  }

  /**
   * Activates the validation message box, making it visible when errors/warnings appear. This
   * should be called after the first Generate button press to prevent showing validation errors
   * before the user has attempted to generate a pattern.
   */
  public void activate() {
    hasBeenActivated.set(true);
  }

  /**
   * Checks if the validation message box has been activated.
   *
   * @return true if activated, false otherwise
   */
  public boolean isActivated() {
    return hasBeenActivated.get();
  }
}
