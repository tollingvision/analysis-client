package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Advanced pattern builder for regex power users with enhanced regex editors, syntax highlighting,
 * validation, and live preview integration. Provides direct regex input with comprehensive
 * validation and explanation features.
 */
public class AdvancedPatternBuilder extends VBox {

  // Services
  private final PatternGenerator patternGenerator = new PatternGenerator();
  private final ValidationBlocker validationBlocker = new ValidationBlocker();

  // Task 15: ValidationModel for live validation refresh and empty-state handling
  private ValidationModel validationModel;
  private ValidationMessageBox validationMessageBox;

  // Configuration property
  private final ObjectProperty<PatternConfiguration> configurationProperty =
      new SimpleObjectProperty<>();

  // UI Components
  private TextArea groupPatternField;
  private TextArea frontPatternField;
  private TextArea rearPatternField;
  private TextArea overviewPatternField;

  private Label groupPatternValidation;
  private Label frontPatternValidation;
  private Label rearPatternValidation;
  private Label overviewPatternValidation;

  private Button explainGroupPatternButton;
  private Button explainFrontPatternButton;
  private Button explainRearPatternButton;
  private Button explainOverviewPatternButton;

  private Button copyGroupPatternButton;
  private Button copyFrontPatternButton;
  private Button copyRearPatternButton;
  private Button copyOverviewPatternButton;

  private PatternPreviewPane previewPane;
  private Label overallValidationStatus;
  private CheckBox extensionMatchingCheckBox;
  private TitledPane errorDetailsPane;
  private TitledPane warningDetailsPane;
  private TextField directoryField;
  private Button browseButton;

  // Sample filenames for preview
  private List<String> sampleFilenames = new ArrayList<>();

  // Background validation task
  private Task<ValidationResult> currentValidationTask;

  // Input folder and i18n resources
  private final String inputFolder;
  private final ResourceBundle messages;

  // Configuration ready callback
  private java.util.function.Consumer<PatternConfiguration> onConfigurationReady;

  /**
   * Creates a new AdvancedPatternBuilder with regex input fields and live preview.
   *
   * @param inputFolder the input folder from the main screen
   * @param messages the resource bundle for i18n
   */
  public AdvancedPatternBuilder(String inputFolder, ResourceBundle messages) {
    this.inputFolder = inputFolder;
    this.messages = messages;

    initializeComponents();
    setupLayout();
    setupValidation();
    setupEventHandlers();

    // Initialize with empty configuration
    setConfiguration(new PatternConfiguration());

    // Automatically load files from input folder
    if (inputFolder != null && !inputFolder.trim().isEmpty()) {
      setSelectedDirectory(inputFolder);
    }
  }

  /**
   * Initializes all UI components. Task 15: Creates ValidationModel and ValidationMessageBox for
   * live validation.
   */
  private void initializeComponents() {
    // Task 15: Initialize validation model with messages for i18n support
    validationModel = new ValidationModel(messages);
    validationMessageBox = new ValidationMessageBox(validationModel, messages);
    // Pattern input fields
    groupPatternField =
        createPatternField(
            messages.getString("pattern.builder.advanced.group.pattern.placeholder"));
    frontPatternField =
        createPatternField(
            messages.getString("pattern.builder.advanced.front.pattern.placeholder"));
    rearPatternField =
        createPatternField(messages.getString("pattern.builder.advanced.rear.pattern.placeholder"));
    overviewPatternField =
        createPatternField(
            messages.getString("pattern.builder.advanced.overview.pattern.placeholder"));

    // Validation labels
    groupPatternValidation = createValidationLabel();
    frontPatternValidation = createValidationLabel();
    rearPatternValidation = createValidationLabel();
    overviewPatternValidation = createValidationLabel();

    // Explain buttons
    explainGroupPatternButton = createExplainButton(messages.getString("button.explain"));
    explainFrontPatternButton = createExplainButton(messages.getString("button.explain"));
    explainRearPatternButton = createExplainButton(messages.getString("button.explain"));
    explainOverviewPatternButton = createExplainButton(messages.getString("button.explain"));

    // Copy buttons
    copyGroupPatternButton = createCopyButton(messages.getString("button.copy"));
    copyFrontPatternButton = createCopyButton(messages.getString("button.copy"));
    copyRearPatternButton = createCopyButton(messages.getString("button.copy"));
    copyOverviewPatternButton = createCopyButton(messages.getString("button.copy"));

    // Preview pane
    previewPane = new PatternPreviewPane(messages);

    // Overall validation status
    overallValidationStatus = new Label(messages.getString("status.ready"));
    overallValidationStatus.setFont(Font.font("System", FontWeight.BOLD, 12));

    // Extension matching checkbox
    extensionMatchingCheckBox = new CheckBox(messages.getString("extension.matching.label"));
    ContextualHelpProvider.addTooltip(extensionMatchingCheckBox, "extension-flexible");

    // Error and warning details panes
    errorDetailsPane =
        new TitledPane(
            messages.getString("validation.title.errors"),
            new Label(messages.getString("validation.status.no.errors.warnings")));
    errorDetailsPane.setExpanded(false);
    errorDetailsPane.getStyleClass().add("validation-error-pane");
    errorDetailsPane.setVisible(false);
    errorDetailsPane.setManaged(false);

    warningDetailsPane =
        new TitledPane(
            messages.getString("validation.title.warnings"),
            new Label(messages.getString("validation.status.no.errors.warnings")));
    warningDetailsPane.setExpanded(false);
    warningDetailsPane.getStyleClass().add("validation-warning-pane");
    warningDetailsPane.setVisible(false);
    warningDetailsPane.setManaged(false);

    // Input folder display (read-only)
    directoryField = new TextField();
    directoryField.setPromptText(messages.getString("placeholder.input.folder"));
    directoryField.setEditable(false);
    directoryField.setPrefWidth(400);
    directoryField.setText(inputFolder != null ? inputFolder : "");

    // No browse button - using inherited folder

    // Add contextual help to pattern fields
    ContextualHelpProvider.addTooltip(groupPatternField, "capturing-group-required");
    ContextualHelpProvider.addTooltip(frontPatternField, "regex-syntax");
    ContextualHelpProvider.addTooltip(rearPatternField, "regex-syntax");
    ContextualHelpProvider.addTooltip(overviewPatternField, "regex-syntax");
    ContextualHelpProvider.addTooltip(directoryField, "sample-files");
    ContextualHelpProvider.addTooltip(browseButton, "sample-files");
  }

  /** Creates a pattern input field with syntax highlighting support. */
  private TextArea createPatternField(String promptText) {
    TextArea textArea = new TextArea();
    textArea.setPromptText(promptText);
    textArea.setPrefRowCount(3);
    textArea.setWrapText(true);
    textArea.getStyleClass().add("regex-editor");

    // Add basic syntax highlighting through CSS classes
    textArea
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              updateSyntaxHighlighting(textArea, newText);
            });

    return textArea;
  }

  /** Creates a validation label for displaying pattern validation results. */
  private Label createValidationLabel() {
    Label label = new Label();
    label.setWrapText(true);
    label.setFont(Font.font("System", 10));
    label.setVisible(false);
    label.setManaged(false);
    return label;
  }

  /** Creates an explain button for pattern explanation. */
  private Button createExplainButton(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("explain-button");
    button.setTooltip(
        new javafx.scene.control.Tooltip(
            "Show a human-readable explanation of this regex pattern.\n"
                + "Breaks down the pattern components and their meaning."));
    return button;
  }

  /** Creates a copy button for copying patterns to clipboard. */
  private Button createCopyButton(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("copy-button");
    button.setTooltip(
        new javafx.scene.control.Tooltip(
            "Copy this regex pattern to the clipboard for use elsewhere."));
    return button;
  }

  /** Sets up the main layout structure. */
  private void setupLayout() {
    setSpacing(15);
    setPadding(new Insets(20));

    // Header
    Label title = new Label(messages.getString("pattern.builder.advanced.title"));
    title.setFont(Font.font("System", FontWeight.BOLD, 16));

    Label description = new Label(messages.getString("pattern.builder.advanced.description"));
    description.setWrapText(true);
    description.setFont(Font.font("System", 11));
    description.setTextFill(Color.GRAY);

    // Input folder display
    Label directoryLabel = new Label(messages.getString("label.input.folder"));
    directoryLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

    HBox directoryBox = new HBox(10);
    directoryBox.setAlignment(Pos.CENTER_LEFT);
    directoryBox.getChildren().add(directoryField);
    HBox.setHgrow(directoryField, Priority.ALWAYS);

    // Pattern input grid
    GridPane patternGrid = createPatternGrid();

    // Overall validation status
    HBox statusBox = new HBox(10);
    statusBox.setAlignment(Pos.CENTER_LEFT);
    statusBox
        .getChildren()
        .addAll(new Label(messages.getString("label.status")), overallValidationStatus);

    // Extension matching option
    HBox extensionBox = new HBox(10);
    extensionBox.setAlignment(Pos.CENTER_LEFT);
    extensionBox.getChildren().add(extensionMatchingCheckBox);

    // Task 15: Add ValidationMessageBox for live validation display
    // Validation details section
    VBox validationDetails = new VBox(5);
    validationDetails
        .getChildren()
        .addAll(validationMessageBox, errorDetailsPane, warningDetailsPane);

    // Preview section
    Label previewTitle = new Label(messages.getString("preview.title"));
    previewTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

    VBox.setVgrow(previewPane, Priority.ALWAYS);

    getChildren()
        .addAll(
            title,
            description,
            directoryLabel,
            directoryBox,
            patternGrid,
            statusBox,
            extensionBox,
            validationDetails,
            previewTitle,
            previewPane);
  }

  /** Creates the pattern input grid with fields, validation, and buttons. */
  private GridPane createPatternGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10));

    // Column constraints
    grid.getColumnConstraints()
        .addAll(
            new javafx.scene.layout.ColumnConstraints(120), // Label column
            new javafx.scene.layout.ColumnConstraints(), // Field column (grows)
            new javafx.scene.layout.ColumnConstraints(100), // Button column
            new javafx.scene.layout.ColumnConstraints(100) // Button column
            );
    grid.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);

    int row = 0;

    // Group Pattern
    addPatternRow(
        grid,
        row++,
        messages.getString("pattern.builder.advanced.group.pattern"),
        groupPatternField,
        groupPatternValidation,
        explainGroupPatternButton,
        copyGroupPatternButton);

    // Front Pattern
    addPatternRow(
        grid,
        row++,
        messages.getString("pattern.builder.advanced.front.pattern"),
        frontPatternField,
        frontPatternValidation,
        explainFrontPatternButton,
        copyFrontPatternButton);

    // Rear Pattern
    addPatternRow(
        grid,
        row++,
        messages.getString("pattern.builder.advanced.rear.pattern"),
        rearPatternField,
        rearPatternValidation,
        explainRearPatternButton,
        copyRearPatternButton);

    // Overview Pattern
    addPatternRow(
        grid,
        row++,
        messages.getString("pattern.builder.advanced.overview.pattern"),
        overviewPatternField,
        overviewPatternValidation,
        explainOverviewPatternButton,
        copyOverviewPatternButton);

    return grid;
  }

  /** Adds a pattern input row to the grid. */
  private void addPatternRow(
      GridPane grid,
      int row,
      String labelText,
      TextArea field,
      Label validation,
      Button explainButton,
      Button copyButton) {
    // Label
    Label label = new Label(labelText);
    label.setFont(Font.font("System", FontWeight.BOLD, 11));
    grid.add(label, 0, row * 2);

    // Field
    grid.add(field, 1, row * 2);

    // Buttons
    grid.add(explainButton, 2, row * 2);
    grid.add(copyButton, 3, row * 2);

    // Validation label (spans all columns)
    grid.add(validation, 0, row * 2 + 1, 4, 1);
  }

  /**
   * Sets up validation for all pattern fields. Task 15: Enhanced with ValidationModel for debounced
   * validation refresh.
   */
  private void setupValidation() {
    // Task 15: Add validation listeners to all fields that trigger ValidationModel
    // refresh
    groupPatternField
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              validatePatterns();
              updateValidationModel();
            });
    frontPatternField
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              validatePatterns();
              updateValidationModel();
            });
    rearPatternField
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              validatePatterns();
              updateValidationModel();
            });
    overviewPatternField
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              validatePatterns();
              updateValidationModel();
            });

    // Task 15: Bind sample filenames to validation model for automatic validation
    // refresh
    // This will be updated when directory changes
    if (inputFolder != null && !inputFolder.trim().isEmpty()) {
      loadSampleFilenames(inputFolder);
    }
  }

  /** Sets up event handlers for buttons and other interactions. */
  private void setupEventHandlers() {
    // Explain button handlers
    explainGroupPatternButton.setOnAction(
        e -> explainPattern(groupPatternField.getText(), messages.getString("pattern.name.group")));
    explainFrontPatternButton.setOnAction(
        e -> explainPattern(frontPatternField.getText(), messages.getString("pattern.name.front")));
    explainRearPatternButton.setOnAction(
        e -> explainPattern(rearPatternField.getText(), messages.getString("pattern.name.rear")));
    explainOverviewPatternButton.setOnAction(
        e ->
            explainPattern(
                overviewPatternField.getText(), messages.getString("pattern.name.overview")));

    // Copy button handlers
    copyGroupPatternButton.setOnAction(
        e ->
            copyToClipboard(groupPatternField.getText(), messages.getString("pattern.name.group")));
    copyFrontPatternButton.setOnAction(
        e ->
            copyToClipboard(frontPatternField.getText(), messages.getString("pattern.name.front")));
    copyRearPatternButton.setOnAction(
        e -> copyToClipboard(rearPatternField.getText(), messages.getString("pattern.name.rear")));
    copyOverviewPatternButton.setOnAction(
        e ->
            copyToClipboard(
                overviewPatternField.getText(), messages.getString("pattern.name.overview")));

    // Extension matching handler
    extensionMatchingCheckBox
        .selectedProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              ValidationLogger.logConfigurationChange(
                  "Extension Matching", oldVal.toString(), newVal.toString());
              validatePatterns();
              // Task 15: Trigger validation model refresh on extension matching change
              updateValidationModel();
            });

    // Configuration property listener
    configurationProperty.addListener(
        (obs, oldConfig, newConfig) -> {
          if (newConfig != null) {
            updateFieldsFromConfiguration(newConfig);
            updatePreview();
            // Task 15: Trigger validation model refresh on configuration change
            updateValidationModel();
          }
        });

    // Validation blocker listeners
    validationBlocker
        .blockedProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              updateValidationStatusDisplay();
            });

    validationBlocker
        .blockingReasonProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              updateValidationStatusDisplay();
            });
  }

  /** Updates syntax highlighting for a text area (basic implementation). */
  private void updateSyntaxHighlighting(TextArea textArea, String text) {
    // Remove existing style classes
    textArea.getStyleClass().removeAll("regex-valid", "regex-invalid");

    // Add style class based on regex validity
    if (text != null && !text.trim().isEmpty()) {
      try {
        Pattern.compile(text);
        textArea.getStyleClass().add("regex-valid");
      } catch (PatternSyntaxException e) {
        textArea.getStyleClass().add("regex-invalid");
      }
    }
  }

  /** Validates all patterns and updates validation displays. */
  private void validatePatterns() {
    // Cancel any running validation task
    if (currentValidationTask != null && !currentValidationTask.isDone()) {
      currentValidationTask.cancel(true);
    }

    // Create configuration from current field values
    PatternConfiguration config = createConfigurationFromFields();

    // Start background validation
    currentValidationTask =
        new Task<ValidationResult>() {
          @Override
          protected ValidationResult call() throws Exception {
            // Apply extension matching if enabled
            if (extensionMatchingCheckBox.isSelected()) {
              config.setGroupPattern(
                  ExtensionMatcher.applyExtensionMatching(config.getGroupPattern(), true));
              config.setFrontPattern(
                  ExtensionMatcher.applyExtensionMatching(config.getFrontPattern(), true));
              config.setRearPattern(
                  ExtensionMatcher.applyExtensionMatching(config.getRearPattern(), true));
              config.setOverviewPattern(
                  ExtensionMatcher.applyExtensionMatching(config.getOverviewPattern(), true));
              List<RoleRule> roleRules = new ArrayList<>();
              roleRules.add(
                  new RoleRule(
                      ImageRole.FRONT,
                      RuleType.REGEX_OVERRIDE,
                      frontPatternField.getText(),
                      false,
                      1));
              roleRules.add(
                  new RoleRule(
                      ImageRole.REAR,
                      RuleType.REGEX_OVERRIDE,
                      rearPatternField.getText(),
                      false,
                      1));
              roleRules.add(
                  new RoleRule(
                      ImageRole.OVERVIEW,
                      RuleType.REGEX_OVERRIDE,
                      overviewPatternField.getText(),
                      false,
                      1));
              config.setRoleRules(roleRules);
            }

            return patternGenerator.validatePatterns(config);
          }

          @Override
          protected void succeeded() {
            Platform.runLater(
                () -> {
                  ValidationResult result = getValue();
                  updateValidationDisplay(result);
                  validationBlocker.updateValidationState(result);
                  updatePreview();

                  // Log validation results
                  for (ValidationError error : result.getErrors()) {
                    ValidationLogger.logValidationError(error, "Advanced Pattern Builder");
                  }

                  for (ValidationWarning warning : result.getWarnings()) {
                    ValidationLogger.logValidationWarning(warning, "Advanced Pattern Builder");
                  }
                });
          }

          @Override
          protected void failed() {
            Platform.runLater(
                () -> {
                  Throwable throwable = getException();
                  Exception exception =
                      throwable instanceof Exception
                          ? (Exception) throwable
                          : new Exception(throwable);
                  ValidationLogger.logException(exception, "Pattern validation failed");

                  overallValidationStatus.setText(
                      String.format(
                          messages.getString("error.validation.failed"), exception.getMessage()));
                  overallValidationStatus.setTextFill(Color.RED);

                  validationBlocker.clearValidationState();
                });
          }
        };

    Thread.startVirtualThread(currentValidationTask);
  }

  /** Creates a PatternConfiguration from current field values. */
  private PatternConfiguration createConfigurationFromFields() {
    // Get the current configuration to preserve tokens, groupIdToken, and
    // optionalTokenTypes
    PatternConfiguration currentConfig = configurationProperty.get();
    PatternConfiguration config = new PatternConfiguration();

    // Set patterns from UI fields
    config.setGroupPattern(groupPatternField.getText());
    config.setFrontPattern(frontPatternField.getText());
    config.setRearPattern(rearPatternField.getText());
    config.setOverviewPattern(overviewPatternField.getText());

    // Create role rules from patterns
    List<RoleRule> roleRules = new ArrayList<>();
    roleRules.add(
        new RoleRule(
            ImageRole.FRONT, RuleType.REGEX_OVERRIDE, frontPatternField.getText(), false, 1));
    roleRules.add(
        new RoleRule(
            ImageRole.REAR, RuleType.REGEX_OVERRIDE, rearPatternField.getText(), false, 1));
    roleRules.add(
        new RoleRule(
            ImageRole.OVERVIEW, RuleType.REGEX_OVERRIDE, overviewPatternField.getText(), false, 1));
    config.setRoleRules(roleRules);

    // Preserve tokens, groupIdToken, and optionalTokenTypes from current
    // configuration
    if (currentConfig != null) {
      if (currentConfig.getTokens() != null) {
        config.setTokens(new ArrayList<>(currentConfig.getTokens()));
      }
      config.setGroupIdToken(currentConfig.getGroupIdToken());
      if (currentConfig.getOptionalTokenTypes() != null) {
        config.setOptionalTokenTypes(new ArrayList<>(currentConfig.getOptionalTokenTypes()));
      }
      if (currentConfig.getOptionalCustomTokenNames() != null) {
        config.setOptionalCustomTokenNames(
            new ArrayList<>(currentConfig.getOptionalCustomTokenNames()));
      }
    }

    return config;
  }

  /** Updates the validation display with results. */
  private void updateValidationDisplay(ValidationResult result) {
    // Clear all validation labels
    clearValidationLabel(groupPatternValidation);
    clearValidationLabel(frontPatternValidation);
    clearValidationLabel(rearPatternValidation);
    clearValidationLabel(overviewPatternValidation);

    // Show errors for specific patterns
    for (ValidationError error : result.getErrors()) {
      switch (error.getType()) {
        case NO_GROUP_ID_SELECTED,
                INVALID_GROUP_PATTERN,
                EMPTY_GROUP_PATTERN,
                MULTIPLE_CAPTURING_GROUPS,
                NO_CAPTURING_GROUPS,
                INCOMPLETE_GROUPS ->
            showValidationMessage(groupPatternValidation, error.getMessage(), true);
        case NO_ROLE_PATTERNS, NO_ROLE_RULES_DEFINED -> {
          // Show on all role pattern fields
          showValidationMessage(frontPatternValidation, error.getMessage(), true);
          showValidationMessage(rearPatternValidation, error.getMessage(), true);
          showValidationMessage(overviewPatternValidation, error.getMessage(), true);
        }
        case REGEX_SYNTAX_ERROR,
            NO_FILES_MATCHED,
            INVALID_REGEX_PATTERN,
            INVALID_RULE_CONFIGURATION,
            INVALID_RULE_VALUE -> {
          // Try to determine which field has the syntax error
          showRegexSyntaxError(error.getMessage());
        }
      }
    }

    // Update overall status
    if (result.isValid()) {
      if (result.hasWarnings()) {
        overallValidationStatus.setText(
            String.format(
                messages.getString("status.configuration.valid.warnings"),
                result.getWarnings().size()));
        overallValidationStatus.setTextFill(Color.ORANGE);
      } else {
        overallValidationStatus.setText(messages.getString("status.configuration.valid"));
        overallValidationStatus.setTextFill(Color.GREEN);
      }

      // Notify parent dialog that configuration is ready when valid
      if (onConfigurationReady != null) {
        PatternConfiguration config = createConfigurationFromFields();
        onConfigurationReady.accept(config);
      }
    } else {
      overallValidationStatus.setText(
          String.format(messages.getString("status.validation.errors"), result.getErrors().size()));
      overallValidationStatus.setTextFill(Color.RED);
    }

    // Update detailed validation panes
    updateValidationDetailPanes(result);
  }

  /** Updates the validation status display based on validation blocker state. */
  private void updateValidationStatusDisplay() {
    String status = validationBlocker.getValidationSummary();
    overallValidationStatus.setText(status);

    // Update style based on validation state
    if (validationBlocker.isBlocked()) {
      overallValidationStatus.setTextFill(Color.RED);
    } else if (!validationBlocker.getActiveWarnings().isEmpty()) {
      overallValidationStatus.setTextFill(Color.ORANGE);
    } else {
      overallValidationStatus.setTextFill(Color.GREEN);
    }
  }

  /** Updates the detailed validation panes with error and warning information. */
  private void updateValidationDetailPanes(ValidationResult result) {
    // Update error details pane
    if (result.hasErrors()) {
      String errorDetails = validationBlocker.getErrorDetails();
      Label errorLabel = new Label(errorDetails);
      errorLabel.setWrapText(true);
      errorDetailsPane.setContent(errorLabel);
      errorDetailsPane.setVisible(true);
      errorDetailsPane.setManaged(true);
      errorDetailsPane.setExpanded(true);
    } else {
      errorDetailsPane.setVisible(false);
      errorDetailsPane.setManaged(false);
    }

    // Update warning details pane
    if (result.hasWarnings()) {
      String warningDetails = validationBlocker.getWarningDetails();
      Label warningLabel = new Label(warningDetails);
      warningLabel.setWrapText(true);
      warningDetailsPane.setContent(warningLabel);
      warningDetailsPane.setVisible(true);
      warningDetailsPane.setManaged(true);
      warningDetailsPane.setExpanded(false);
    } else {
      warningDetailsPane.setVisible(false);
      warningDetailsPane.setManaged(false);
    }
  }

  /** Shows a regex syntax error on the appropriate field. */
  private void showRegexSyntaxError(String message) {
    // Check each field for syntax errors
    checkFieldSyntax(
        groupPatternField,
        groupPatternValidation,
        messages.getString("pattern.name.group"),
        message);
    checkFieldSyntax(
        frontPatternField,
        frontPatternValidation,
        messages.getString("pattern.name.front"),
        message);
    checkFieldSyntax(
        rearPatternField, rearPatternValidation, messages.getString("pattern.name.rear"), message);
    checkFieldSyntax(
        overviewPatternField,
        overviewPatternValidation,
        messages.getString("pattern.name.overview"),
        message);
  }

  /** Checks a field for regex syntax errors and shows validation message if found. */
  private void checkFieldSyntax(
      TextArea field, Label validationLabel, String fieldName, String errorMessage) {
    String text = field.getText();
    if (text != null && !text.trim().isEmpty()) {
      try {
        Pattern.compile(text);
      } catch (PatternSyntaxException e) {
        showValidationMessage(
            validationLabel,
            String.format(messages.getString("pattern.syntax.error"), fieldName, e.getMessage()),
            true);
      }
    }
  }

  /** Shows a validation message on a label. */
  private void showValidationMessage(Label label, String message, boolean isError) {
    label.setText(message);
    label.setTextFill(isError ? Color.RED : Color.ORANGE);
    label.setVisible(true);
    label.setManaged(true);
  }

  /** Clears a validation label. */
  private void clearValidationLabel(Label label) {
    label.setText("");
    label.setVisible(false);
    label.setManaged(false);
  }

  /** Updates the fields from a configuration object. */
  private void updateFieldsFromConfiguration(PatternConfiguration config) {
    // Get patterns from config, or generate from roleRules if empty
    String groupPattern = config.getGroupPattern();
    String frontPattern = config.getFrontPattern();
    String rearPattern = config.getRearPattern();
    String overviewPattern = config.getOverviewPattern();

    // If patterns are empty but roleRules exist, generate patterns from roleRules
    if ((frontPattern == null || frontPattern.isEmpty())
        && (rearPattern == null || rearPattern.isEmpty())
        && (overviewPattern == null || overviewPattern.isEmpty())
        && config.getRoleRules() != null
        && !config.getRoleRules().isEmpty()) {

      // Generate patterns from roleRules
      for (RoleRule rule : config.getRoleRules()) {
        String pattern = generatePatternFromRule(rule);
        if (pattern != null && !pattern.isEmpty()) {
          switch (rule.getTargetRole()) {
            case FRONT -> frontPattern = pattern;
            case REAR -> rearPattern = pattern;
            case OVERVIEW -> overviewPattern = pattern;
          }
        }
      }
    }

    groupPatternField.setText(groupPattern != null ? groupPattern : "");
    frontPatternField.setText(frontPattern != null ? frontPattern : "");
    rearPatternField.setText(rearPattern != null ? rearPattern : "");
    overviewPatternField.setText(overviewPattern != null ? overviewPattern : "");
  }

  /**
   * Generates a regex pattern from a role rule.
   *
   * @param rule the role rule
   * @return the generated pattern, or empty string if cannot generate
   */
  private String generatePatternFromRule(RoleRule rule) {
    if (rule.getRuleValue() == null || rule.getRuleValue().isEmpty()) {
      return "";
    }

    String value = rule.getRuleValue();
    boolean caseSensitive = rule.isCaseSensitive();

    switch (rule.getRuleType()) {
      case CONTAINS:
        return (caseSensitive ? "" : "(?i:") + ".*" + value + ".*" + (caseSensitive ? "" : ")");
      case STARTS_WITH:
        return (caseSensitive ? "" : "(?i:") + "^" + value + ".*" + (caseSensitive ? "" : ")");
      case ENDS_WITH:
        return (caseSensitive ? "" : "(?i:") + ".*" + value + "$" + (caseSensitive ? "" : ")");
      case EQUALS:
        return (caseSensitive ? "" : "(?i:") + "^" + value + "$" + (caseSensitive ? "" : ")");
      case REGEX_OVERRIDE:
        return value; // Already a regex pattern
      default:
        return "";
    }
  }

  /** Updates the preview pane with current configuration. */
  private void updatePreview() {
    if (!sampleFilenames.isEmpty()) {
      PatternConfiguration config = createConfigurationFromFields();
      previewPane.updatePreview(config, sampleFilenames);
    }
  }

  /** Explains a regex pattern in a user-friendly dialog with concrete examples. */
  private void explainPattern(String pattern, String patternName) {
    if (pattern == null || pattern.trim().isEmpty()) {
      showAlert(
          Alert.AlertType.INFORMATION,
          messages.getString("alert.pattern.explanation.title"),
          String.format(messages.getString("alert.copy.pattern.empty.header"), patternName),
          messages.getString("alert.pattern.explanation.empty"));
      return;
    }

    // Determine which pattern type this is for context-specific explanation
    boolean isGroupPattern = patternName.toLowerCase().contains("group");
    boolean isFrontPattern = patternName.toLowerCase().contains("front");
    boolean isRearPattern = patternName.toLowerCase().contains("rear");
    boolean isOverviewPattern = patternName.toLowerCase().contains("overview");

    String explanation;
    if (isGroupPattern) {
      explanation = generateGroupPatternExplanation(pattern);
    } else if (isFrontPattern || isRearPattern || isOverviewPattern) {
      String roleType = isFrontPattern ? "Front" : (isRearPattern ? "Rear" : "Overview/Scene");
      explanation = generateRolePatternExplanation(pattern, roleType);
    } else {
      explanation = generateGenericPatternExplanation(pattern);
    }

    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Pattern Explanation: " + patternName);
    alert.setHeaderText("Detailed Pattern Analysis");

    TextArea explanationArea = new TextArea(explanation);
    explanationArea.setEditable(false);
    explanationArea.setWrapText(true);
    explanationArea.setPrefRowCount(20);
    explanationArea.setPrefColumnCount(80);
    explanationArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

    alert.getDialogPane().setContent(explanationArea);
    alert.getDialogPane().setPrefWidth(700);
    alert.showAndWait();
  }

  /** Generates detailed explanation for group pattern (extraction logic). */
  private String generateGroupPatternExplanation(String pattern) {
    StringBuilder explanation = new StringBuilder();
    explanation.append("═══════════════════════════════════════════════════════════════\n");
    explanation.append("  GROUP PATTERN - Vehicle/Event Identifier Extraction\n");
    explanation.append("═══════════════════════════════════════════════════════════════\n\n");

    explanation.append("Pattern: ").append(pattern).append("\n\n");

    explanation.append("━━━ PURPOSE ━━━\n");
    explanation.append("This pattern extracts the GROUP ID from filenames. Files with the same\n");
    explanation.append(
        "group ID will be processed together as a single vehicle/event. The first\n");
    explanation.append("capturing group (parentheses) extracts the identifier.\n\n");

    // Analyze the pattern structure
    explanation.append("━━━ PATTERN STRUCTURE ━━━\n");
    analyzePatternComponents(pattern, explanation);
    explanation.append("\n");

    // Find capturing group
    int groupStart = pattern.indexOf('(');
    int groupEnd = -1;
    if (groupStart != -1) {
      int depth = 0;
      for (int i = groupStart; i < pattern.length(); i++) {
        if (pattern.charAt(i) == '(' && (i == 0 || pattern.charAt(i - 1) != '\\')) depth++;
        if (pattern.charAt(i) == ')' && (i == 0 || pattern.charAt(i - 1) != '\\')) {
          depth--;
          if (depth == 0) {
            groupEnd = i;
            break;
          }
        }
      }
    }

    if (groupStart != -1 && groupEnd != -1) {
      String capturedPart = pattern.substring(groupStart + 1, groupEnd);
      explanation.append("━━━ EXTRACTED GROUP ID ━━━\n");
      explanation.append("Capturing group: (").append(capturedPart).append(")\n");
      explanation.append("This part will be extracted as the group identifier.\n\n");
    }

    // Generate examples
    explanation.append("━━━ EXAMPLES ━━━\n");
    generateGroupExamples(pattern, explanation);
    explanation.append("\n");

    explanation.append("━━━ HOW GROUPING WORKS ━━━\n");
    explanation.append("1. Pattern is applied to each filename in the folder\n");
    explanation.append("2. The first capturing group (...) extracts the group ID\n");
    explanation.append("3. Files with the same group ID are grouped together\n");
    explanation.append("4. Within each group, front/rear/overview roles are assigned\n");
    explanation.append("5. Each group is sent as one event to the analysis service\n");

    return explanation.toString();
  }

  /** Generates detailed explanation for role patterns (front/rear/overview). */
  private String generateRolePatternExplanation(String pattern, String roleType) {
    StringBuilder explanation = new StringBuilder();
    explanation.append("═══════════════════════════════════════════════════════════════\n");
    explanation
        .append("  ROLE PATTERN - ")
        .append(roleType.toUpperCase())
        .append(" Image Detection\n");
    explanation.append("═══════════════════════════════════════════════════════════════\n\n");

    explanation.append("Pattern: ").append(pattern).append("\n\n");

    explanation.append("━━━ PURPOSE ━━━\n");
    explanation
        .append("This pattern identifies which images in a group are ")
        .append(roleType)
        .append(" views.\n");
    explanation.append("Multiple images in one group can match this pattern.\n\n");

    explanation.append("━━━ PATTERN STRUCTURE ━━━\n");
    analyzePatternComponents(pattern, explanation);
    explanation.append("\n");

    explanation.append("━━━ MATCHING EXAMPLES ━━━\n");
    generateRoleExamples(pattern, roleType, explanation);
    explanation.append("\n");

    explanation.append("━━━ HOW ROLE ASSIGNMENT WORKS ━━━\n");
    explanation.append("1. After files are grouped by group ID\n");
    explanation.append("2. This pattern is checked against each filename in the group\n");
    explanation
        .append("3. If the pattern matches, the image is marked as: ")
        .append(roleType)
        .append("\n");
    explanation.append("4. One group can have multiple images of the same role\n");
    explanation.append("5. Typical setup: one front, one rear, optionally overview/scene\n");

    return explanation.toString();
  }

  /** Generates generic pattern explanation for unknown pattern types. */
  private String generateGenericPatternExplanation(String pattern) {
    StringBuilder explanation = new StringBuilder();
    explanation.append("Pattern: ").append(pattern).append("\n\n");

    explanation.append("━━━ PATTERN STRUCTURE ━━━\n");
    analyzePatternComponents(pattern, explanation);

    return explanation.toString();
  }

  /** Analyzes and explains individual pattern components. */
  private void analyzePatternComponents(String pattern, StringBuilder explanation) {
    if (pattern.startsWith("^")) {
      explanation.append("  ^ = Match must start at the beginning of the filename\n");
    }
    if (pattern.endsWith("$")) {
      explanation.append("  $ = Match must end at the end of the filename\n");
    }
    if (pattern.contains(".*")) {
      explanation.append("  .* = Match any characters (zero or more)\n");
    }
    if (pattern.contains(".+")) {
      explanation.append("  .+ = Match any characters (one or more)\n");
    }
    if (pattern.contains("\\d+")) {
      explanation.append("  \\d+ = Match one or more digits (0-9)\n");
    }
    if (pattern.contains("\\d{")) {
      java.util.regex.Pattern digitPattern =
          java.util.regex.Pattern.compile("\\\\d\\{(\\d+)(,\\d+)?\\}");
      java.util.regex.Matcher matcher = digitPattern.matcher(pattern);
      while (matcher.find()) {
        String quantifier = matcher.group();
        explanation.append("  ").append(quantifier).append(" = Match exactly ");
        if (matcher.group(2) != null) {
          explanation
              .append(matcher.group(1))
              .append(" to ")
              .append(matcher.group(2).substring(1))
              .append(" digits\n");
        } else {
          explanation.append(matcher.group(1)).append(" digits\n");
        }
      }
    }
    if (pattern.contains("\\w+")) {
      explanation.append("  \\w+ = Match one or more word characters (letters, digits, _)\n");
    }
    if (pattern.contains("[_\\-\\.\\s]+")) {
      explanation.append("  [_\\-\\.\\s]+ = Match one or more delimiters (_, -, ., space)\n");
    }
    if (pattern.contains("(?i:")) {
      explanation.append("  (?i:...) = Case-insensitive matching\n");
    }

    int capturingGroups = countCapturingGroups(pattern);
    if (capturingGroups > 0) {
      explanation.append("  (...) = Capturing group");
      if (capturingGroups > 1) {
        explanation.append("s");
      }
      explanation.append(" (").append(capturingGroups).append(" total)\n");
    }
  }

  /** Generates concrete examples for group pattern matching. */
  private void generateGroupExamples(String pattern, StringBuilder explanation) {
    // Example filenames
    String[] examples = {
      "vehicle_12345_front.jpg",
      "52_00171301_11707_1_front.jpg",
      "IMG_2024_001_scene.jpg",
      "cam1-ev-5789-rear.png"
    };

    try {
      java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
      for (String example : examples) {
        java.util.regex.Matcher matcher = compiledPattern.matcher(example);
        if (matcher.find() && matcher.groupCount() > 0) {
          String extracted = matcher.group(1);
          explanation.append("  ✓ \"").append(example).append("\"\n");
          explanation.append("    → Group ID extracted: \"").append(extracted).append("\"\n\n");
        }
      }

      // If no examples matched, show what WOULD match
      if (explanation.toString().contains("✓") == false) {
        explanation.append(
            "  (Showing pattern structure - provide sample files for real examples)\n");
      }
    } catch (Exception e) {
      explanation.append("  (Pattern syntax error - cannot generate examples)\n");
    }
  }

  /** Generates concrete examples for role pattern matching. */
  private void generateRoleExamples(String pattern, String roleType, StringBuilder explanation) {
    // Example filenames based on role type
    String[] frontExamples = {
      "vehicle_001_front.jpg", "52_00171301_11707_1_front.jpg", "IMG_FRONT_2024.jpg"
    };
    String[] rearExamples = {
      "vehicle_001_rear.jpg", "52_00171301_11707_1_rear.jpg", "IMG_BACK_2024.jpg"
    };
    String[] overviewExamples = {
      "vehicle_001_scene.jpg", "52_00171301_11707_lpr.jpg", "IMG_OVERVIEW_2024.jpg"
    };

    String[] examples =
        roleType.equals("Front")
            ? frontExamples
            : (roleType.equals("Rear") ? rearExamples : overviewExamples);

    try {
      java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
      for (String example : examples) {
        java.util.regex.Matcher matcher = compiledPattern.matcher(example);
        if (matcher.find()) {
          explanation.append("  ✓ MATCH: \"").append(example).append("\"\n");
        } else {
          explanation.append("  ✗ NO MATCH: \"").append(example).append("\"\n");
        }
      }
    } catch (Exception e) {
      explanation.append("  (Pattern syntax error - cannot generate examples)\n");
    }
  }

  /** Counts capturing groups in a regex pattern. */
  private int countCapturingGroups(String pattern) {
    if (pattern == null) return 0;

    int count = 0;
    boolean inCharClass = false;
    boolean escaped = false;

    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '[') {
        inCharClass = true;
      } else if (c == ']') {
        inCharClass = false;
      } else if (!inCharClass && c == '(' && i + 1 < pattern.length()) {
        // Check if it's a non-capturing group
        if (pattern.charAt(i + 1) != '?') {
          count++;
        }
      }
    }

    return count;
  }

  /** Copies text to the system clipboard. */
  private void copyToClipboard(String text, String patternName) {
    if (text == null || text.trim().isEmpty()) {
      showAlert(
          Alert.AlertType.WARNING,
          messages.getString("alert.copy.pattern.title"),
          String.format(messages.getString("alert.copy.pattern.empty.header"), patternName),
          messages.getString("alert.copy.pattern.empty.content"));
      return;
    }

    Clipboard clipboard = Clipboard.getSystemClipboard();
    ClipboardContent content = new ClipboardContent();
    content.putString(text);
    clipboard.setContent(content);

    // Show brief confirmation
    overallValidationStatus.setText(
        String.format(messages.getString("message.copy.pattern.success"), patternName));
    overallValidationStatus.setTextFill(Color.BLUE);

    // Reset status after 2 seconds
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> validatePatterns()));
    timeline.play();
  }

  /** Shows an alert dialog. */
  private void showAlert(Alert.AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
  }

  /**
   * Sets the pattern configuration.
   *
   * @param configuration the pattern configuration
   */
  public void setConfiguration(PatternConfiguration configuration) {
    configurationProperty.set(configuration);
  }

  /**
   * Gets the current pattern configuration from the input fields.
   *
   * @return the current pattern configuration
   */
  public PatternConfiguration getConfiguration() {
    return createConfigurationFromFields();
  }

  /**
   * Sets the configuration ready callback.
   *
   * @param callback the callback to call when configuration is ready
   */
  public void setOnConfigurationReady(java.util.function.Consumer<PatternConfiguration> callback) {
    this.onConfigurationReady = callback;
  }

  /**
   * @return the configuration property for binding
   */
  public ObjectProperty<PatternConfiguration> configurationProperty() {
    return configurationProperty;
  }

  /** Loads sample files from the selected directory. */
  private void loadSampleFiles(java.nio.file.Path directory) {
    try {
      sampleFilenames.clear();

      try (java.util.stream.Stream<java.nio.file.Path> files =
          java.nio.file.Files.list(directory)) {
        List<String> imageFiles =
            files
                .filter(java.nio.file.Files::isRegularFile)
                .map(java.nio.file.Path::getFileName)
                .map(java.nio.file.Path::toString)
                .filter(this::isImageFile)
                .limit(20) // Limit for performance
                .toList();

        sampleFilenames.addAll(imageFiles);
        updatePreview();

        ValidationLogger.logFileAnalysis(imageFiles.size(), 0, 0);
      }

    } catch (Exception e) {
      ValidationLogger.logException(e, "Failed to load sample files");
      overallValidationStatus.setText(
          String.format(messages.getString("error.loading.files"), e.getMessage()));
      overallValidationStatus.setTextFill(javafx.scene.paint.Color.RED);
    }
  }

  /** Checks if a filename represents an image file. */
  private boolean isImageFile(String filename) {
    return ExtensionMatcher.hasImageExtension(filename);
  }

  /**
   * Sets the sample filenames for preview.
   *
   * @param filenames the sample filenames
   */
  public void setSampleFilenames(List<String> filenames) {
    this.sampleFilenames = filenames != null ? new ArrayList<>(filenames) : new ArrayList<>();
    updatePreview();
  }

  /**
   * Gets the preview pane for external access.
   *
   * @return the pattern preview pane
   */
  public PatternPreviewPane getPreviewPane() {
    return previewPane;
  }

  /** Clears all pattern fields. */
  public void clearPatterns() {
    groupPatternField.clear();
    frontPatternField.clear();
    rearPatternField.clear();
    overviewPatternField.clear();
  }

  /**
   * Validates the current configuration and returns the result.
   *
   * @return the validation result
   */
  public ValidationResult validateConfiguration() {
    PatternConfiguration config = createConfigurationFromFields();
    return patternGenerator.validatePatterns(config);
  }

  /**
   * Gets the validation blocker for external access.
   *
   * @return the validation blocker
   */
  public ValidationBlocker getValidationBlocker() {
    return validationBlocker;
  }

  /**
   * Gets the extension matching checkbox for external binding.
   *
   * @return the extension matching checkbox
   */
  public CheckBox getExtensionMatchingCheckBox() {
    return extensionMatchingCheckBox;
  }

  /**
   * Gets whether flexible extension matching is enabled.
   *
   * @return true if flexible extension matching is enabled
   */
  public boolean isFlexibleExtensionMatchingEnabled() {
    return extensionMatchingCheckBox.isSelected();
  }

  /**
   * Sets whether flexible extension matching is enabled.
   *
   * @param enabled true to enable flexible extension matching
   */
  public void setFlexibleExtensionMatchingEnabled(boolean enabled) {
    extensionMatchingCheckBox.setSelected(enabled);
  }

  /**
   * Gets the selected directory path.
   *
   * @return the selected directory path, or null if none selected
   */
  public String getSelectedDirectory() {
    return directoryField.getText();
  }

  /**
   * Sets the selected directory path and loads sample files. Task 15: Enhanced to trigger
   * validation refresh when folder changes.
   *
   * @param directoryPath the directory path to set
   */
  public void setSelectedDirectory(String directoryPath) {
    if (directoryPath != null && !directoryPath.trim().isEmpty()) {
      directoryField.setText(directoryPath);
      loadSampleFiles(java.nio.file.Paths.get(directoryPath));
      // Task 15: Load sample filenames for validation model
      loadSampleFilenames(directoryPath);
    }
  }

  /**
   * Cleanup method to shut down background processing. Task 15: Enhanced to include ValidationModel
   * cleanup.
   */
  public void shutdown() {
    if (currentValidationTask != null && !currentValidationTask.isDone()) {
      currentValidationTask.cancel(true);
    }
    if (previewPane != null) {
      previewPane.shutdown();
    }
    // Task 15: Shutdown ValidationModel background processing
    if (validationModel != null) {
      validationModel.shutdown();
    }
    // Virtual threads don't need explicit shutdown
  }

  /**
   * Task 15: Gets the ValidationModel for external access and mode coordination.
   *
   * @return the validation model instance
   */
  public ValidationModel getValidationModel() {
    return validationModel;
  }

  /**
   * Task 15: Updates the ValidationModel with current configuration. This triggers debounced
   * validation refresh automatically.
   */
  private void updateValidationModel() {
    try {
      PatternConfiguration config = createConfigurationFromFields();
      // Task 15: Update configuration triggers automatic debounced validation
      validationModel.updateConfiguration(config);
    } catch (Exception e) {
      ValidationLogger.logException(
          e, "Failed to update validation model in AdvancedPatternBuilder");
      validationModel.updateConfiguration(null);
    }
  }

  /**
   * Task 15: Loads sample filenames from the specified directory for validation.
   *
   * @param directoryPath the directory path to load filenames from
   */
  private void loadSampleFilenames(String directoryPath) {
    try {
      java.nio.file.Path directory = java.nio.file.Path.of(directoryPath);
      if (java.nio.file.Files.exists(directory) && java.nio.file.Files.isDirectory(directory)) {
        List<String> filenames = new ArrayList<>();

        try (java.util.stream.Stream<java.nio.file.Path> files =
            java.nio.file.Files.list(directory)) {
          files
              .filter(java.nio.file.Files::isRegularFile)
              .map(java.nio.file.Path::getFileName)
              .map(java.nio.file.Path::toString)
              .filter(this::isImageFile)
              .limit(500) // Limit for performance
              .forEach(filenames::add);
        }

        sampleFilenames = filenames;
        // Task 15: Update validation model with sample filenames
        validationModel.updateSampleFilenames(filenames);
      }
    } catch (Exception e) {
      ValidationLogger.logException(e, "Failed to load sample filenames in AdvancedPatternBuilder");
      sampleFilenames = new ArrayList<>();
      validationModel.updateSampleFilenames(new ArrayList<>());
    }
  }
}
