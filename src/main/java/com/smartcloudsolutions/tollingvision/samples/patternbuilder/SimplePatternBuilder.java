package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

/**
 * Visual pattern builder for non-regex users with guided workflow steps.
 *
 * <p>Task 15 Implementation: This class demonstrates live validation refresh and empty-state
 * handling using the new ValidationModel and ValidationMessageBox components.
 */
public class SimplePatternBuilder extends VBox {

  // Core data properties
  private final ObservableList<String> sampleFilenames = FXCollections.observableArrayList();
  private final ObservableList<FilenameToken> detectedTokens = FXCollections.observableArrayList();
  private final ObjectProperty<TokenType> selectedGroupId = new SimpleObjectProperty<>();
  private final ObservableList<RoleRule> roleRules = FXCollections.observableArrayList();
  private final java.util.Set<TokenType> optionalTokenTypes = new java.util.HashSet<>();

  // Services
  private final PatternGenerator patternGenerator = new PatternGenerator();
  private final ValidationBlocker validationBlocker = new ValidationBlocker();
  private final CustomTokenManager customTokenManager = new CustomTokenManager();
  private final FilenameTokenizer tokenizer = new FilenameTokenizer();

  // Task 15: New validation system - ValidationModel as observable shared state
  private ValidationModel validationModel;
  private ValidationMessageBox validationMessageBox;

  // UI Components
  private FileAnalysisPane fileAnalysisPane;
  private TokenSelectionPane tokenSelectionPane;
  private GroupIdSelector groupIdSelector;
  private RoleRulesPane roleRulesPane;
  private PatternPreviewPane previewPane;

  // Navigation and validation
  private Button nextButton;
  private Button previousButton;
  private Button reanalyzeButton;
  private Label validationStatusLabel;
  private Label helpLabel;
  private Label stepIndicatorLabel;
  private CheckBox extensionMatchingCheckBox;
  private ProgressIndicator analysisProgressIndicator;
  private int currentStep = 0;
  private final int totalSteps = 3; // Token Selection, Group ID, Role Rules (File Analysis removed)

  // Caching for tokenization/grouping results
  private static final java.util.Map<String, TokenAnalysis> analysisCache =
      new java.util.HashMap<>();
  private boolean isAnalyzing = false;
  private boolean analysisComplete = false;

  // Token preview control
  private Spinner<Integer> tokenPreviewCountSpinner;

  private final BooleanProperty useFlexibleExtensionMatching = new SimpleBooleanProperty(false);

  // Input folder and i18n resources
  private final String inputFolder;
  private final ResourceBundle messages;

  // Configuration ready callback
  private java.util.function.Consumer<PatternConfiguration> onConfigurationReady;

  /**
   * Creates a new SimplePatternBuilder with guided workflow steps.
   *
   * @param inputFolder the input folder from the main screen
   * @param messages the resource bundle for i18n
   */
  public SimplePatternBuilder(String inputFolder, ResourceBundle messages) {
    this.inputFolder = inputFolder;
    this.messages = messages;

    initializeComponents();
    setupBindings();

    setupLayout();
    setupNavigation();

    // Initialize validation blocker
    validationBlocker.clearValidationState();

    // Load custom tokens from persistence (includes preconfigured ones if none
    // saved)
    customTokenManager.loadCustomTokens();

    // Auto-analyze files from input folder and start with first visible step
    if (inputFolder != null && !inputFolder.trim().isEmpty()) {
      // Start analysis immediately
      startFileAnalysis(inputFolder);
      // Start with step 0 (Token Selection - previously step 1)
      showStep(0);
    } else {
      throw new IllegalArgumentException("Input folder must be provided and exist");
    }
  }

  /**
   * Initializes all UI components. Task 15: Creates ValidationModel and ValidationMessageBox for
   * live validation. Task 12: Integrates performance optimizations with caching and background
   * processing.
   */
  private void initializeComponents() {
    // Initialize step-based UI components with performance optimizations
    fileAnalysisPane = new FileAnalysisPane(messages);
    tokenSelectionPane = new TokenSelectionPane(customTokenManager);
    tokenSelectionPane.setOptionalTokenTypes(optionalTokenTypes);
    groupIdSelector = new GroupIdSelector(messages);
    groupIdSelector.setCustomTokenManager(customTokenManager);
    roleRulesPane = new RoleRulesPane();

    // Task 12: Initialize preview pane with shared background service for
    // performance
    previewPane = new PatternPreviewPane(fileAnalysisPane.getBackgroundService(), messages);

    // Task 15: Initialize validation model with messages for i18n support
    validationModel = new ValidationModel(messages);
    validationMessageBox = new ValidationMessageBox(validationModel, messages);

    // Navigation components
    nextButton = new Button(messages.getString("button.next"));
    previousButton = new Button(messages.getString("button.previous"));
    reanalyzeButton = new Button(messages.getString("button.reanalyze"));
    validationStatusLabel = new Label();
    helpLabel = new Label();
    stepIndicatorLabel = new Label();
    extensionMatchingCheckBox = new CheckBox(messages.getString("extension.matching.label"));
    analysisProgressIndicator = new ProgressIndicator();

    // Token preview count spinner (will be configured after analysis)
    tokenPreviewCountSpinner = new Spinner<>(1, 1, 1);
    tokenPreviewCountSpinner.setEditable(true);
    tokenPreviewCountSpinner.setPrefWidth(80);

    nextButton.setDefaultButton(true);
    previousButton.setDisable(true); // Will be enabled on step 1+

    // Configure reanalyze button
    reanalyzeButton.getStyleClass().add("reanalyze-button");
    reanalyzeButton.setOnAction(e -> reanalyzeFiles());

    // Configure progress indicator
    analysisProgressIndicator.setMaxSize(20, 20);
    analysisProgressIndicator.setVisible(false);

    // Configure step indicator label
    stepIndicatorLabel.getStyleClass().add("step-indicator");
    updateStepIndicator();

    // Configure validation status label
    validationStatusLabel.getStyleClass().add("validation-status");
    validationStatusLabel.setWrapText(true);
    helpLabel.getStyleClass().add("help-text");
    helpLabel.setWrapText(true);

    // Configure extension matching checkbox
    ContextualHelpProvider.addTooltip(extensionMatchingCheckBox, "extension-flexible");

    // Add contextual help to components
    ContextualHelpProvider.addTooltip(nextButton, "validation-blocking");
    ContextualHelpProvider.addTooltip(previousButton, "preview-auto-update");
  }

  /**
   * Sets up data bindings between components. Task 15: Implements live validation refresh with
   * debounced updates.
   */
  private void setupBindings() {
    // Task 15: Bind sample filenames to validation model for automatic validation
    // refresh
    sampleFilenames.addListener(
        (javafx.collections.ListChangeListener<String>)
            c -> {
              validationModel.updateSampleFilenames(new ArrayList<>(sampleFilenames));
            });

    // Task 15: Bind detected tokens changes to trigger validation refresh
    detectedTokens.addListener(
        (javafx.collections.ListChangeListener<FilenameToken>)
            c -> {
              updateValidationModel();
            });

    // Task 15: Bind group ID selection to trigger validation refresh
    selectedGroupId.addListener(
        (obs, oldVal, newVal) -> {
          ValidationLogger.logConfigurationChange(
              "Group ID",
              oldVal != null ? oldVal.name() : "none",
              newVal != null ? newVal.name() : "none");
          updateValidationModel();
        });

    // Task 15: Bind role rules changes to trigger validation refresh
    roleRules.addListener(
        (javafx.collections.ListChangeListener<RoleRule>)
            c -> {
              ValidationLogger.logConfigurationChange(
                  "Role Rules", "previous count", String.valueOf(roleRules.size()));
              updateValidationModel();
            });

    // Task 15: Bind extension matching changes to trigger validation refresh
    useFlexibleExtensionMatching.addListener(
        (obs, oldVal, newVal) -> {
          ValidationLogger.logConfigurationChange(
              "Extension Matching", oldVal.toString(), newVal.toString());
          updateValidationModel();
        });

    // Bind token preview count changes to trigger validation refresh on final step
    // This will be set up after the spinner is configured in applyAnalysisResult

    // Bind extension matching checkbox
    extensionMatchingCheckBox.selectedProperty().bindBidirectional(useFlexibleExtensionMatching);

    // Bind validation blocker to navigation
    validationBlocker
        .blockedProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              updateNavigationState();
            });

    // Bind validation blocker to UI updates
    validationBlocker
        .blockingReasonProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              updateValidationStatusDisplay();
            });

    // File analysis is now handled automatically in constructor - no binding needed
    // Bind tokens to group ID selector and role rules pane
    detectedTokens.addListener(
        (javafx.collections.ListChangeListener<FilenameToken>)
            c -> {
              groupIdSelector.setAvailableTokens(detectedTokens);
              roleRulesPane.suggestRulesFromTokens(detectedTokens);
            });

    // Bind group ID selector to our property (now using TokenType)
    groupIdSelector
        .selectedGroupIdProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              selectedGroupId.set(newVal);
            });

    // Bind role rules pane to our list
    roleRulesPane
        .getRoleRules()
        .addListener(
            (javafx.collections.ListChangeListener<RoleRule>)
                c -> {
                  roleRules.setAll(roleRulesPane.getRoleRules());
                  // Refresh suggestions when rules change to handle deduplication
                  roleRulesPane.refreshSuggestions(new ArrayList<>(detectedTokens));
                });
  }

  /** Sets up the main layout structure. */
  private void setupLayout() {
    setSpacing(15);
    setPadding(new Insets(20));
    setAlignment(javafx.geometry.Pos.TOP_CENTER);

    // Header with step indicator and help
    javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(10);
    header.setAlignment(javafx.geometry.Pos.CENTER);

    // Use the managed step indicator label instead of creating a new one
    helpLabel.setText(
        messages.getString("workflow.token.selection")); // Start with first visible step

    header.getChildren().addAll(stepIndicatorLabel, helpLabel);

    // Validation status area
    javafx.scene.layout.VBox validationArea = new javafx.scene.layout.VBox(5);
    validationArea.setAlignment(javafx.geometry.Pos.CENTER);
    validationArea.getChildren().add(validationStatusLabel);

    // Extension matching option (shown in later steps)
    javafx.scene.layout.HBox extensionMatchingArea = new javafx.scene.layout.HBox(10);
    extensionMatchingArea.setAlignment(javafx.geometry.Pos.CENTER);
    extensionMatchingArea.getChildren().add(extensionMatchingCheckBox);
    extensionMatchingArea.setVisible(false);
    extensionMatchingArea.setManaged(false);

    // Navigation buttons
    ButtonBar navigationBar = new ButtonBar();
    navigationBar.getButtons().addAll(previousButton, nextButton);

    getChildren().addAll(header, validationArea, extensionMatchingArea, navigationBar);
  }

  /** Sets up step navigation logic. Updated for new step numbering without File Analysis step. */
  private void setupNavigation() {
    nextButton.setOnAction(
        e -> {
          if (validateCurrentStep()) {
            if (currentStep < totalSteps - 1) {
              showStep(currentStep + 1);
            } else {
              // Final step - generate configuration
              generateFinalConfiguration();
            }
          }
        });

    previousButton.setOnAction(
        e -> {
          if (currentStep > 0) {
            showStep(currentStep - 1);
          }
        });
  }

  /**
   * Shows the specified step and updates navigation. Task 15: Implements debounced validation
   * refresh on step enter/leave.
   *
   * @param step the step number to show (0-based)
   */
  private void showStep(int step) {
    // Update current step and step indicator
    currentStep = step;
    updateStepIndicator();

    // Update help text based on current step (renumbered: 0=Token Selection,
    // 1=Group ID, 2=Role
    // Rules)
    String helpKey =
        switch (step) {
          case 0 -> "workflow.token.selection";
          case 1 -> "workflow.group.id.selection";
          case 2 -> "workflow.role.rules";
          default -> "workflow.token.selection";
        };
    helpLabel.setText(messages.getString(helpKey));

    // Clear current content (keep header, validation area, extension area,
    // navigation)
    if (getChildren().size() > 4) {
      getChildren().remove(3, getChildren().size() - 1);
    }

    // Show/hide extension matching option
    javafx.scene.layout.HBox extensionArea = (javafx.scene.layout.HBox) getChildren().get(2);
    boolean showExtensionOption = step >= 1; // Show from Group ID step onwards
    extensionArea.setVisible(showExtensionOption);
    extensionArea.setManaged(showExtensionOption);

    // Add step-specific content (renumbered: 0=Token Selection, 1=Group ID, 2=Role
    // Rules)
    switch (step) {
      case 0 -> {
        // Token Selection step - combine controls on one line with padding
        javafx.scene.layout.HBox tokenHeader = new javafx.scene.layout.HBox(15);
        tokenHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        tokenHeader.setPadding(new javafx.geometry.Insets(0, 0, 0, 20)); // Match content padding

        Label previewLabel = new Label(messages.getString("token.preview.show.first"));
        Label filesLabel = new Label(messages.getString("token.preview.files.suffix"));

        // Add custom token button (manual trigger only)
        Button customTokenButton = new Button(messages.getString("button.custom.tokens"));
        customTokenButton.getStyleClass().add("secondary-button");
        customTokenButton.setOnAction(
            e -> {
              CustomTokenDialog dialog = new CustomTokenDialog(customTokenManager, messages);
              dialog.setOnTokenUpdated(
                  () -> {
                    refreshTokenizationAfterCustomTokenUpdate();
                    // Refresh token display to show custom tokens
                    if (tokenSelectionPane != null && analysisCache.get(inputFolder) != null) {
                      tokenSelectionPane.setTokenAnalysis(analysisCache.get(inputFolder));
                    }
                  });
              dialog.showAndWait();
            });

        tokenHeader
            .getChildren()
            .addAll(
                previewLabel,
                tokenPreviewCountSpinner,
                filesLabel,
                reanalyzeButton,
                analysisProgressIndicator,
                customTokenButton);

        // No validation display on early steps
        getChildren().addAll(getChildren().size() - 1, List.of(tokenHeader, tokenSelectionPane));
        nextButton.setText(messages.getString("button.next"));
        previousButton.setDisable(true); // Disable Previous on first visible step
      }
      case 1 -> {
        // Group ID Selection step - use tokens from Step 1 with CURRENT types
        // Get updated tokens from TokenSelectionPane (includes user modifications)
        TokenAnalysis updatedAnalysis = tokenSelectionPane.getTokenAnalysis();
        if (updatedAnalysis != null) {
          // Collect ALL unique tokens from ALL files with their current types
          List<FilenameToken> allTokens =
              updatedAnalysis.getTokenizedFilenames().values().stream()
                  .filter(java.util.Objects::nonNull)
                  .flatMap(java.util.List::stream)
                  .filter(java.util.Objects::nonNull)
                  .distinct()
                  .collect(java.util.stream.Collectors.toList());

          // Update detectedTokens with the modified tokens from Step 1
          detectedTokens.clear();
          detectedTokens.addAll(allTokens);
        }

        // Pass a deep copy to preserve token type changes from Step 1
        List<FilenameToken> tokensForGroupId =
            detectedTokens.stream()
                .map(
                    t ->
                        new FilenameToken(
                            t.getValue(), t.getPosition(), t.getSuggestedType(), t.getConfidence()))
                .collect(java.util.stream.Collectors.toList());
        groupIdSelector.setAvailableTokens(tokensForGroupId);
        getChildren().addAll(getChildren().size() - 1, List.of(groupIdSelector));
        nextButton.setText(messages.getString("button.next"));
        previousButton.setDisable(false);
      }
      case 2 -> {
        // Role Rules step (final step) - ONLY step with full validation display
        roleRulesPane.suggestRulesFromTokens(detectedTokens);
        previewPane.updatePreview(generateConfiguration(), sampleFilenames);

        // Listen to role rules changes for live validation updates
        roleRulesPane
            .getRoleRules()
            .addListener(
                (javafx.collections.ListChangeListener<RoleRule>)
                    change -> {
                      updateValidationState();
                      showValidationDetails();
                    });

        getChildren()
            .addAll(
                getChildren().size() - 1,
                List.of(roleRulesPane, validationMessageBox, previewPane));
        nextButton.setText(messages.getString("button.generate.pattern"));
      }
    }

    // Update validation and navigation after content change
    updateNavigationState();

    // Run validation on every step if analysis is complete to ensure status is
    // always current
    if (analysisComplete && !sampleFilenames.isEmpty()) {
      updateValidationState();
      // Only request full validation refresh on final step
      if (step == 2) {
        updateValidationModel();
        validationModel.requestValidationRefresh();
        // Show/update validation details on final step
        showValidationDetails();
      }
    }

    // Log step change
    ValidationLogger.logUserAction("Step changed", "Step " + (step + 1) + " of " + totalSteps);
  }

  /** Shows detailed validation information in the final step. */
  private void showValidationDetails() {
    // First, remove any existing validation detail panes
    getChildren()
        .removeIf(
            node ->
                node instanceof TitledPane
                    && (node.getStyleClass().contains("validation-error-pane")
                        || node.getStyleClass().contains("validation-warning-pane")));

    // Only show validation details if there are actual errors or warnings
    if (validationBlocker.isBlocked()) {
      // Show error details in a expandable area
      TitledPane errorPane =
          new TitledPane(
              messages.getString("validation.title.errors"),
              new Label(validationBlocker.getErrorDetails()));
      errorPane.setExpanded(true);
      errorPane.getStyleClass().add("validation-error-pane");
      getChildren().add(getChildren().size() - 1, errorPane);
    } else if (!validationBlocker.getActiveWarnings().isEmpty()) {
      // Only show warnings if no errors
      TitledPane warningPane =
          new TitledPane(
              messages.getString("validation.title.warnings"),
              new Label(validationBlocker.getWarningDetails()));
      warningPane.setExpanded(false);
      warningPane.getStyleClass().add("validation-warning-pane");
      getChildren().add(getChildren().size() - 1, warningPane);
    }
  }

  /** Updates navigation button states based on current step validation. */
  private void updateNavigationState() {
    boolean stepValid = validateCurrentStep();
    boolean blocked = validationBlocker.isBlocked();
    nextButton.setDisable(!stepValid || (currentStep == totalSteps - 1 && blocked));
    previousButton.setDisable(currentStep == 0);

    // Update button text based on validation state
    if (currentStep == totalSteps - 1) {
      if (blocked) {
        nextButton.setText(messages.getString("button.fix.errors"));
      } else {
        nextButton.setText(messages.getString("button.generate.pattern"));
      }
    }
  }

  /**
   * Validates the current step to determine if user can proceed. Updated for new step numbering:
   * 0=Token Selection, 1=Group ID, 2=Role Rules Early steps have lightweight validation; only final
   * step has full validation blocking.
   *
   * @return true if the current step is valid and user can proceed
   */
  private boolean validateCurrentStep() {
    return switch (currentStep) {
      case 0 ->
          analysisComplete
              && !detectedTokens
                  .isEmpty(); // Token Selection - analysis must be complete and tokens detected
      case 1 ->
          selectedGroupId.get()
              != null; // Group ID Selection - group ID must be selected (no validation
        // blocking)
      case 2 ->
          analysisComplete
              && !roleRules.isEmpty()
              && !validationBlocker.isBlocked(); // Role Rules - full validation only on final step
      default -> false;
    };
  }

  /**
   * Starts file analysis for the given directory. Checks cache first, then performs analysis if
   * needed.
   *
   * @param directoryPath the directory to analyze
   */
  private void startFileAnalysis(String directoryPath) {
    // Check cache first
    TokenAnalysis cachedResult = analysisCache.get(directoryPath);
    if (cachedResult != null) {
      // Use cached result
      applyAnalysisResult(cachedResult);
      return;
    }

    // Perform new analysis
    performFileAnalysis(directoryPath);
  }

  /**
   * Performs actual file analysis (bypassing cache).
   *
   * @param directoryPath the directory to analyze
   */
  private void performFileAnalysis(String directoryPath) {
    isAnalyzing = true;
    analysisProgressIndicator.setVisible(true);
    validationStatusLabel.setText(messages.getString("simple.analysis.status.running"));

    // Perform analysis in background
    javafx.concurrent.Task<TokenAnalysis> analysisTask =
        new javafx.concurrent.Task<TokenAnalysis>() {
          @Override
          protected TokenAnalysis call() throws Exception {
            // Load files and perform tokenization
            java.nio.file.Path directory = java.nio.file.Path.of(directoryPath);
            List<String> filenames = new ArrayList<>();

            if (java.nio.file.Files.exists(directory)
                && java.nio.file.Files.isDirectory(directory)) {
              try (java.util.stream.Stream<java.nio.file.Path> files =
                  java.nio.file.Files.list(directory)) {
                files
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .filter(SimplePatternBuilder.this::isImageFile)
                    .limit(500) // Limit for performance
                    .forEach(filenames::add);
              }
            }

            // Perform tokenization
            return tokenizer.analyzeFilenames(filenames);
          }

          @Override
          protected void succeeded() {
            TokenAnalysis result = getValue();
            // Cache the result
            analysisCache.put(directoryPath, result);
            applyAnalysisResult(result);
          }

          @Override
          protected void failed() {
            javafx.application.Platform.runLater(
                () -> {
                  isAnalyzing = false;
                  analysisProgressIndicator.setVisible(false);
                  validationStatusLabel.setText(
                      String.format(
                          messages.getString("simple.analysis.status.failed"),
                          getException().getMessage()));
                });
          }
        };

    // Run analysis task
    Thread analysisThread = new Thread(analysisTask);
    analysisThread.setDaemon(true);
    analysisThread.start();
  }

  /**
   * Applies the analysis result to the UI.
   *
   * @param analysis the analysis result to apply
   */
  private void applyAnalysisResult(TokenAnalysis analysis) {
    javafx.application.Platform.runLater(
        () -> {
          isAnalyzing = false;
          analysisProgressIndicator.setVisible(false);
          analysisComplete = true; // Mark analysis as complete

          // Update sample filenames
          sampleFilenames.clear();
          sampleFilenames.addAll(analysis.getFilenames());

          // Configure token preview spinner - start with 10 files by default
          int fileCount = sampleFilenames.size();
          int maxPreview = Math.min(500, fileCount);
          int defaultPreview = Math.min(10, fileCount); // Default to 10 files
          tokenPreviewCountSpinner.setValueFactory(
              new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxPreview, defaultPreview));

          // Set up token preview count change listener
          tokenPreviewCountSpinner
              .valueProperty()
              .addListener(
                  (obs, oldVal, newVal) -> {
                    if (newVal != null && newVal > 0) {
                      updateTokenPreview(newVal);
                      // Trigger validation refresh on final step when preview count changes
                      if (currentStep == 2 && analysisComplete) {
                        updateValidationModel();
                      }
                    }
                  });

          // Store the full analysis for preview filtering
          analysisCache.put(inputFolder, analysis);

          // Run initial preview with N=10 (default) - apply view filter immediately
          tokenPreviewCountSpinner.getValueFactory().setValue(defaultPreview);
          updateTokenPreview(
              defaultPreview); // This will update detectedTokens and TokenSelectionPane

          // Update validation status
          validationStatusLabel.setText(
              String.format(
                  messages.getString("simple.analysis.status.completed"), sampleFilenames.size()));

          // Task 15: Update validation model with sample filenames for automatic
          // validation
          validationModel.updateSampleFilenames(new ArrayList<>(sampleFilenames));

          // Only run validation if we're on the final step
          if (currentStep == 2) {
            updateValidationState();
            updateValidationModel();
          }

          // Log analysis completion
          ValidationLogger.logFileAnalysis(
              analysis.getFilenames().size(),
              analysis.getSuggestions().size(),
              0 // Duration not tracked here
              );
        });
  }

  /** Reanalyzes files by clearing cache and performing fresh analysis. */
  private void reanalyzeFiles() {
    if (isAnalyzing) {
      return; // Already analyzing
    }

    // Clear cache for current directory
    analysisCache.remove(inputFolder);

    // Clear current results and reset analysis state
    sampleFilenames.clear();
    detectedTokens.clear();
    selectedGroupId.set(null);
    roleRules.clear();
    analysisComplete = false; // Reset analysis completion flag

    // Perform fresh analysis
    performFileAnalysis(inputFolder);

    ValidationLogger.logUserAction("Reanalyze requested", "Clearing cache and re-running analysis");
  }

  /** Updates the step indicator label with proper 1-based numbering. */
  private void updateStepIndicator() {
    String stepText =
        String.format(
            messages.getString("pattern.builder.simple.step.indicator"),
            currentStep + 1,
            totalSteps);
    stepIndicatorLabel.setText(stepText);
  }

  /**
   * Updates the token preview display to show tokens from the first N files.
   *
   * @param fileCount the number of files to use for token preview
   */
  private void updateTokenPreview(int fileCount) {
    if (!analysisComplete || sampleFilenames.isEmpty()) {
      return;
    }

    // Get the cached analysis result
    TokenAnalysis analysis = analysisCache.get(inputFolder);
    if (analysis == null) {
      return;
    }

    // Check if we have existing configuration that should be preserved
    boolean hadConfiguration =
        !detectedTokens.isEmpty() && selectedGroupId.get() != null && !roleRules.isEmpty();

    // Create a filtered analysis with only the first N files
    List<String> previewFiles =
        sampleFilenames.subList(0, Math.min(fileCount, sampleFilenames.size()));

    // If we had configuration loaded from preset, don't re-analyze - just keep it
    if (hadConfiguration) {
      // Don't touch detectedTokens - keep the preset configuration
      // Update UI components with preserved configuration
      if (groupIdSelector != null && !detectedTokens.isEmpty()) {
        groupIdSelector.setAvailableTokens(detectedTokens);
      }
      if (roleRulesPane != null && !roleRules.isEmpty()) {
        roleRulesPane.setRoleRules(roleRules);
      }
    } else {
      // No previous configuration - perform fresh token extraction
      detectedTokens.clear();

      // Collect all unique tokens from all preview files
      java.util.Set<String> seenTokenValues = new java.util.HashSet<>();
      for (String filename : previewFiles) {
        List<FilenameToken> tokens = analysis.getTokensForFilename(filename);
        if (tokens != null) {
          for (FilenameToken token : tokens) {
            // Only add if we haven't seen this token value yet
            if (seenTokenValues.add(token.getValue())) {
              detectedTokens.add(token);
            }
          }
        }
      }

      // If no tokens found after collecting from all files, create basic fallback
      if (detectedTokens.isEmpty() && !previewFiles.isEmpty()) {
        String firstFile = previewFiles.get(0);
        String[] parts = firstFile.replaceAll("\\.[^.]+$", "").split("[_\\-\\s]+");
        for (int i = 0; i < parts.length; i++) {
          if (!parts[i].isEmpty()) {
            TokenType type =
                i == 0
                    ? TokenType.PREFIX
                    : (i == parts.length - 1 ? TokenType.CAMERA_SIDE : TokenType.UNKNOWN);
            detectedTokens.add(new FilenameToken(parts[i], i, type, 0.8));
          }
        }
      }
      System.out.println(
          "DETECTED TOKENS (" + detectedTokens.size() + " unique): " + detectedTokens);
    }

    // Update the TokenSelectionPane with filtered analysis
    // Create a filtered TokenAnalysis for display
    java.util.Map<String, List<FilenameToken>> filteredTokens = new java.util.HashMap<>();
    for (String filename : previewFiles) {
      List<FilenameToken> tokens = analysis.getTokensForFilename(filename);
      if (tokens != null) {
        filteredTokens.put(filename, tokens);
      }
    }

    TokenAnalysis filteredAnalysis =
        new TokenAnalysis(
            previewFiles,
            filteredTokens,
            analysis.getSuggestions(),
            analysis.getConfidenceScores());
    tokenSelectionPane.setTokenAnalysis(filteredAnalysis);

    updateNavigationState();

    ValidationLogger.logUserAction(
        "Token preview updated", "Showing tokens from " + fileCount + " files");
  }

  /**
   * Updates the validation state based on current configuration. Runs validation on all steps when
   * analysis is complete to keep status label current.
   */
  private void updateValidationState() {
    // Always run validation and update status when analysis is complete
    if (!analysisComplete) {
      // Clear validation state if no analysis yet
      validationBlocker.clearValidationState();
      updateValidationStatusDisplay();
      return;
    }

    // Run validation on all steps to keep global status current
    try {
      PatternConfiguration config = generateConfiguration();
      ValidationResult result = patternGenerator.validatePatterns(config);

      validationBlocker.updateValidationState(result);

      // Log validation results only on final step
      if (currentStep == 2) {
        for (ValidationError error : result.getErrors()) {
          ValidationLogger.logValidationError(error, "Step " + (currentStep + 1));
        }

        for (ValidationWarning warning : result.getWarnings()) {
          ValidationLogger.logValidationWarning(warning, "Step " + (currentStep + 1));
        }
      }

    } catch (Exception e) {
      ValidationLogger.logException(e, "Validation update failed");
      validationBlocker.clearValidationState();
    }

    // Always update the status display after validation
    updateValidationStatusDisplay();
  }

  /** Generates the final configuration and signals completion. */
  private void generateFinalConfiguration() {
    // Activate validation message box on first Generate button press
    if (!validationMessageBox.isActivated()) {
      validationMessageBox.activate();
    }

    PatternConfiguration config = generateConfiguration();

    // Signal that configuration is complete
    ValidationLogger.logUserAction("Pattern generation completed", "Final configuration generated");

    // Task 15: Ensure success banner is consistent with validation state
    if (validationModel.shouldShowSuccessBanner()) {
      validationStatusLabel.setText("✓ " + messages.getString("status.pattern.generated"));
      validationStatusLabel
          .getStyleClass()
          .removeAll("validation-success", "validation-warning", "validation-error");
      validationStatusLabel.getStyleClass().add("validation-success");
    }

    // Notify parent dialog that configuration is ready
    if (onConfigurationReady != null) {
      onConfigurationReady.accept(config);
    }
  }

  /**
   * Task 15: Updates the ValidationModel with current configuration. This triggers debounced
   * validation refresh automatically. Only runs if analysis is complete and we're on the final
   * step.
   */
  private void updateValidationModel() {
    // Only run validation model updates if analysis is complete and we're on the
    // final step
    if (!analysisComplete || currentStep != 2) {
      updateNavigationState();
      return;
    }

    try {
      PatternConfiguration config = generateConfiguration();
      // Task 15: Update configuration triggers automatic debounced validation
      validationModel.updateConfiguration(config);
      previewPane.updatePreview(config, sampleFilenames);
    } catch (Exception e) {
      ValidationLogger.logException(e, "Failed to update validation model");
      validationModel.updateConfiguration(null);
    }
  }

  /**
   * Updates the validation status display in the UI. Task 15: Ensures consistent validation display
   * with ValidationMessageBox.
   */
  private void updateValidationStatusDisplay() {
    String status = validationBlocker.getValidationSummary();
    validationStatusLabel.setText(status);

    // Update style based on validation state
    validationStatusLabel
        .getStyleClass()
        .removeAll("validation-success", "validation-warning", "validation-error");

    if (validationBlocker.isBlocked()) {
      validationStatusLabel.getStyleClass().add("validation-error");
    } else if (!validationBlocker.getActiveWarnings().isEmpty()) {
      validationStatusLabel.getStyleClass().add("validation-warning");
    } else {
      validationStatusLabel.getStyleClass().add("validation-success");
    }
  }

  /**
   * Generates the complete pattern configuration from current settings.
   *
   * @return the generated pattern configuration
   */
  public PatternConfiguration generateConfiguration() {
    PatternConfiguration config = new PatternConfiguration();

    // Set tokens and group ID
    config.setTokens(List.copyOf(detectedTokens));
    config.setGroupIdToken(selectedGroupId.get());
    config.setRoleRules(List.copyOf(roleRules));

    // Generate regex patterns
    if (selectedGroupId.get() != null) {
      // Get all optional types including mapped types from custom tokens
      java.util.Set<TokenType> allOptionalTypes = getAllOptionalTypes();

      String groupPattern =
          patternGenerator.generateGroupPattern(
              detectedTokens, selectedGroupId.get(), allOptionalTypes);

      // Apply extension matching if enabled
      if (useFlexibleExtensionMatching.get()) {
        groupPattern = ExtensionMatcher.applyExtensionMatching(groupPattern, true);
      }

      config.setGroupPattern(groupPattern);
      ValidationLogger.logPatternGeneration("Group Pattern", groupPattern, true);
    }

    // Generate role patterns
    for (ImageRole role : ImageRole.values()) {
      List<RoleRule> rulesForRole =
          roleRules.stream().filter(rule -> rule.getTargetRole() == role).toList();

      if (!rulesForRole.isEmpty()) {
        String rolePattern = patternGenerator.generateRolePattern(rulesForRole, role);

        // Apply extension matching if enabled
        if (useFlexibleExtensionMatching.get()) {
          rolePattern = ExtensionMatcher.applyExtensionMatching(rolePattern, true);
        }

        switch (role) {
          case FRONT -> {
            config.setFrontPattern(rolePattern);
            ValidationLogger.logPatternGeneration("Front Pattern", rolePattern, true);
          }
          case REAR -> {
            config.setRearPattern(rolePattern);
            ValidationLogger.logPatternGeneration("Rear Pattern", rolePattern, true);
          }
          case OVERVIEW -> {
            config.setOverviewPattern(rolePattern);
            ValidationLogger.logPatternGeneration("Overview Pattern", rolePattern, true);
          }
        }
      }
    }

    return config;
  }

  /**
   * Task 15: Demonstrates that pattern generation succeeds when success banner is shown.
   *
   * @return true if patterns can be generated successfully
   */
  public boolean canGeneratePatterns() {
    return validationModel.canGeneratePatterns();
  }

  /**
   * Task 15: Checks if validation message box should be hidden (empty state).
   *
   * @return true if no validation issues exist
   */
  public boolean shouldHideValidationDisplay() {
    return validationModel.shouldHideValidationDisplay();
  }

  // Getters for data binding and external access

  /**
   * @return the observable list of sample filenames
   */
  public ObservableList<String> getSampleFilenames() {
    return sampleFilenames;
  }

  /**
   * @return the observable list of detected tokens
   */
  public ObservableList<FilenameToken> getDetectedTokens() {
    return detectedTokens;
  }

  /**
   * @return the selected group ID token type property
   */
  public ObjectProperty<TokenType> selectedGroupIdProperty() {
    return selectedGroupId;
  }

  /**
   * @return the observable list of role rules
   */
  public ObservableList<RoleRule> getRoleRules() {
    return roleRules;
  }

  /**
   * @return the validation blocker instance
   */
  public ValidationBlocker getValidationBlocker() {
    return validationBlocker;
  }

  /**
   * Task 15: Gets the ValidationModel for external access.
   *
   * @return the validation model instance
   */
  public ValidationModel getValidationModel() {
    return validationModel;
  }

  /**
   * @return the flexible extension matching property
   */
  public BooleanProperty useFlexibleExtensionMatchingProperty() {
    return useFlexibleExtensionMatching;
  }

  /**
   * Gets the custom token manager.
   *
   * @return the custom token manager
   */
  public CustomTokenManager getCustomTokenManager() {
    return customTokenManager;
  }

  /**
   * Gets the current analysis directory.
   *
   * @return the analysis directory path, or null if none set
   */
  public String getAnalysisDirectory() {
    return inputFolder;
  }

  /**
   * Clears the analysis cache for a specific directory. Should be called when the input folder
   * changes outside the dialog.
   *
   * @param directoryPath the directory path to clear from cache
   */
  public static void clearAnalysisCache(String directoryPath) {
    if (directoryPath != null) {
      analysisCache.remove(directoryPath);
    }
  }

  /** Clears the entire analysis cache. Should be called when the session ends. */
  public static void clearAllAnalysisCache() {
    analysisCache.clear();
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
   * Loads configuration into the builder, restoring tokens, group ID selection, and role rules.
   *
   * @param config the configuration to load
   */
  public void setConfiguration(PatternConfiguration config) {
    if (config != null) {
      // Restore tokens - the tokens will be refreshed when analysis completes
      if (config.getTokens() != null && !config.getTokens().isEmpty()) {
        detectedTokens.setAll(config.getTokens());

        // Update TokenSelectionPane with loaded tokens
        if (tokenSelectionPane != null && analysisCache.get(inputFolder) != null) {
          // Create a TokenAnalysis with the loaded tokens for display
          TokenAnalysis loadedAnalysis = createTokenAnalysisFromTokens(config.getTokens());
          tokenSelectionPane.setTokenAnalysis(loadedAnalysis);
        }
      }

      // Restore group ID selection
      if (config.getGroupIdToken() != null) {
        selectedGroupId.set(config.getGroupIdToken());

        // Update GroupIdSelector if tokens are available
        if (groupIdSelector != null && !detectedTokens.isEmpty()) {
          groupIdSelector.setAvailableTokens(detectedTokens);
        }
      }

      // Restore role rules
      if (config.getRoleRules() != null && !config.getRoleRules().isEmpty()) {
        roleRules.setAll(config.getRoleRules());

        // Update RoleRulesPane
        if (roleRulesPane != null) {
          roleRulesPane.setRoleRules(roleRules);
        }
      }
    }
  }

  /**
   * Creates a TokenAnalysis from a list of tokens for UI display.
   *
   * @param tokens the tokens to create analysis from
   * @return the token analysis
   */
  private TokenAnalysis createTokenAnalysisFromTokens(List<FilenameToken> tokens) {
    // Create a map of filename -> tokens
    java.util.Map<String, List<FilenameToken>> tokensByFile = new java.util.HashMap<>();

    // If we have sample filenames, distribute tokens across them
    if (!sampleFilenames.isEmpty()) {
      // Use the first file as representative
      String firstFile = sampleFilenames.get(0);
      tokensByFile.put(firstFile, new ArrayList<>(tokens));
    } else {
      // Create a dummy filename
      tokensByFile.put("sample.jpg", new ArrayList<>(tokens));
    }

    // Create empty suggestions and confidence scores
    List<TokenSuggestion> suggestions = new ArrayList<>();
    java.util.Map<TokenType, Double> confidenceScores = new java.util.HashMap<>();

    return new TokenAnalysis(
        new ArrayList<>(tokensByFile.keySet()), tokensByFile, suggestions, confidenceScores);
  }

  /**
   * Sets the directory for analysis and triggers file loading. Task 15: Triggers validation refresh
   * when folder changes.
   *
   * @param directoryPath the directory path to analyze
   */
  public void setAnalysisDirectory(String directoryPath) {
    if (directoryPath != null && !directoryPath.trim().isEmpty()) {
      try {
        java.nio.file.Path directory = java.nio.file.Path.of(directoryPath);
        if (java.nio.file.Files.exists(directory) && java.nio.file.Files.isDirectory(directory)) {
          analyzeSampleFiles(directory);
        }
      } catch (Exception e) {
        ValidationLogger.logException(e, "Failed to set analysis directory");
        // Fallback to empty state
        sampleFilenames.clear();
        detectedTokens.clear();
      }
    }
  }

  /** Shows the custom token dialog if it hasn't been shown yet. */
  public void showCustomTokenDialogIfNeeded() {
    if (!customTokenManager.hasCustomTokenDialogBeenShown()) {
      // Show custom token dialog
      CustomTokenDialog dialog = new CustomTokenDialog(customTokenManager, messages);
      dialog.setOnTokenUpdated(
          () -> {
            refreshTokenizationAfterCustomTokenUpdate();
            // Refresh token display to show custom tokens
            if (tokenSelectionPane != null && analysisCache.get(inputFolder) != null) {
              tokenSelectionPane.setTokenAnalysis(analysisCache.get(inputFolder));
            }
          });
      dialog.showAndWait();

      customTokenManager.markCustomTokenDialogShown();
    }
  }

  /** Refreshes tokenization after custom token updates. */
  private void refreshTokenizationAfterCustomTokenUpdate() {
    if (analysisComplete && inputFolder != null) {
      // Clear cache to force re-analysis with new custom tokens
      analysisCache.remove(inputFolder);

      // Re-run analysis
      performFileAnalysis(inputFolder);

      ValidationLogger.logUserAction(
          "Tokenization refreshed", "Custom tokens updated, re-analyzing files");
    }
  }

  /**
   * Updates detected tokens from analysis results.
   *
   * @param analysis the token analysis results
   */
  private void updateTokensFromAnalysis(TokenAnalysis analysis) {
    detectedTokens.clear();

    // Get representative tokens from the first filename
    if (!analysis.getFilenames().isEmpty()) {
      String firstFilename = analysis.getFilenames().get(0);
      List<FilenameToken> tokens = analysis.getTokensForFilename(firstFilename);
      detectedTokens.addAll(tokens);
    }

    // If no tokens found, create some basic ones for demonstration
    if (detectedTokens.isEmpty() && !sampleFilenames.isEmpty()) {
      String firstFile = sampleFilenames.get(0);
      // Create basic tokens by splitting on common delimiters
      String[] parts = firstFile.replaceAll("\\.[^.]+$", "").split("[_\\-\\s]+");
      for (int i = 0; i < parts.length; i++) {
        if (!parts[i].isEmpty()) {
          TokenType type =
              i == 0
                  ? TokenType.PREFIX
                  : (i == parts.length - 1 ? TokenType.CAMERA_SIDE : TokenType.UNKNOWN);
          detectedTokens.add(new FilenameToken(parts[i], i, type, 0.8));
        }
      }
    }
  }

  /**
   * Checks if a filename represents an image file.
   *
   * @param filename the filename to check
   * @return true if the filename represents an image file
   */
  private boolean isImageFile(String filename) {
    String lower = filename.toLowerCase();
    return lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".png")
        || lower.endsWith(".bmp")
        || lower.endsWith(".tiff")
        || lower.endsWith(".gif")
        || lower.endsWith(".webp");
  }

  /**
   * Analyzes sample files from the specified directory. Task 15: Triggers validation refresh when
   * files are analyzed.
   *
   * @param sampleDirectory the directory containing sample files
   */
  public void analyzeSampleFiles(java.nio.file.Path sampleDirectory) {
    try {
      // Check if we have existing configuration that should be preserved
      boolean hadConfiguration =
          !detectedTokens.isEmpty() && selectedGroupId.get() != null && !roleRules.isEmpty();
      List<FilenameToken> previousTokens = new ArrayList<>(detectedTokens);
      TokenType previousGroupId = selectedGroupId.get();
      List<RoleRule> previousRules = new ArrayList<>(roleRules);

      sampleFilenames.clear();

      // Load actual image files from the directory
      try (java.util.stream.Stream<java.nio.file.Path> files =
          java.nio.file.Files.list(sampleDirectory)) {
        List<String> imageFiles =
            files
                .filter(java.nio.file.Files::isRegularFile)
                .map(java.nio.file.Path::getFileName)
                .map(java.nio.file.Path::toString)
                .filter(this::isImageFile)
                .limit(500) // Limit for performance
                .toList();

        sampleFilenames.addAll(imageFiles);
      }

      // If we had a configuration loaded from preset, don't re-analyze - just restore
      // it
      if (hadConfiguration) {
        // Restore the saved configuration without running new analysis
        detectedTokens.setAll(previousTokens);
        selectedGroupId.set(previousGroupId);
        roleRules.setAll(previousRules);

        ValidationLogger.logFileAnalysis(sampleFilenames.size(), previousTokens.size(), 0);
      } else {
        // No previous configuration - perform fresh tokenization
        detectedTokens.clear();

        if (!sampleFilenames.isEmpty()) {
          TokenAnalysis analysis = tokenizer.analyzeFilenames(sampleFilenames);

          // Enhance with custom tokens
          TokenAnalysis enhancedAnalysis = customTokenManager.enhanceWithCustomTokens(analysis);

          // Extract representative tokens from the analysis
          updateTokensFromAnalysis(enhancedAnalysis);

          ValidationLogger.logFileAnalysis(
              enhancedAnalysis.getFilenames().size(),
              enhancedAnalysis.getSuggestions().size(),
              0 // Duration not tracked here
              );
        }
      }

      // Task 15: File analysis triggers automatic validation refresh
      ValidationLogger.logUserAction(
          "Sample files analyzed",
          sampleDirectory.toString() + " - " + sampleFilenames.size() + " files");

    } catch (Exception e) {
      ValidationLogger.logException(e, "Failed to analyze sample files");
      // Ensure we have some fallback data for testing
      if (sampleFilenames.isEmpty()) {
        sampleFilenames.addAll(
            List.of("vehicle_001_front.jpg", "vehicle_001_rear.jpg", "vehicle_002_front.jpg"));
      }
    }
  }

  /**
   * Gets all optional token types, including mapped types from optional custom tokens.
   *
   * @return set of all optional token types for pattern generation
   */
  private java.util.Set<TokenType> getAllOptionalTypes() {
    java.util.Set<TokenType> allOptional = new java.util.HashSet<>();

    // Add optional token types from TokenSelectionPane
    if (tokenSelectionPane != null) {
      allOptional.addAll(tokenSelectionPane.getOptionalTokenTypes());
    }

    // Add mapped types from optional custom tokens
    if (tokenSelectionPane != null) {
      java.util.Set<String> optionalCustomNames = tokenSelectionPane.getOptionalCustomTokenNames();
      for (String customName : optionalCustomNames) {
        // Find the custom token by name and add its mapped type
        for (CustomTokenManager.CustomToken customToken : customTokenManager.getAllCustomTokens()) {
          if (customToken.getName().equals(customName)) {
            allOptional.add(customToken.getMappedType());
            break;
          }
        }
      }
    }

    return allOptional;
  }

  /**
   * Saves the optional token state to the given PatternBuilderConfig.
   *
   * @param config the configuration to save to
   */
  public void saveOptionalStateToConfig(PatternBuilderConfig config) {
    if (config != null && tokenSelectionPane != null) {
      // Convert optionalTokenTypes Set to List of Strings - READ FROM
      // TOKENSELECTIONPANE!
      List<String> optionalTypeNames =
          tokenSelectionPane.getOptionalTokenTypes().stream().map(TokenType::name).toList();
      config.setOptionalTokenTypes(optionalTypeNames);

      // Save optional custom token names
      List<String> optionalCustomNames =
          new ArrayList<>(tokenSelectionPane.getOptionalCustomTokenNames());
      config.setOptionalCustomTokenNames(optionalCustomNames);

      System.out.println(
          "DEBUG saveOptionalStateToConfig: optionalTokenTypes size = "
              + tokenSelectionPane.getOptionalTokenTypes().size());
      System.out.println(
          "DEBUG saveOptionalStateToConfig: optionalTokenTypes = "
              + tokenSelectionPane.getOptionalTokenTypes());
      System.out.println(
          "DEBUG saveOptionalStateToConfig: Set identity = "
              + System.identityHashCode(tokenSelectionPane.getOptionalTokenTypes()));
      System.out.println(
          "DEBUG saveOptionalStateToConfig: optionalCustomNames size = "
              + optionalCustomNames.size());
      System.out.println(
          "DEBUG saveOptionalStateToConfig: Config now has "
              + (config.getOptionalTokenTypes() != null ? config.getOptionalTokenTypes().size() : 0)
              + " optional types");
    }
  }

  /**
   * Loads the optional token state from the given PatternBuilderConfig.
   *
   * @param config the configuration to load from
   */
  public void loadOptionalStateFromConfig(PatternBuilderConfig config) {
    if (config != null && tokenSelectionPane != null) {
      // Load optional token types - WRITE TO TOKENSELECTIONPANE!
      java.util.Set<TokenType> tokenTypes = tokenSelectionPane.getOptionalTokenTypes();
      tokenTypes.clear();
      if (config.getOptionalTokenTypes() != null) {
        for (String typeName : config.getOptionalTokenTypes()) {
          try {
            TokenType type = TokenType.valueOf(typeName);
            tokenTypes.add(type);
          } catch (IllegalArgumentException e) {
            // Ignore invalid token type names
          }
        }
      }

      // Load optional custom token names
      java.util.Set<String> customTokenNames = tokenSelectionPane.getOptionalCustomTokenNames();
      customTokenNames.clear();
      if (config.getOptionalCustomTokenNames() != null) {
        customTokenNames.addAll(config.getOptionalCustomTokenNames());
      }

      // Refresh the UI to show the optional token type flags
      if (analysisCache.get(inputFolder) != null && !detectedTokens.isEmpty()) {
        TokenAnalysis loadedAnalysis = createTokenAnalysisFromTokens(detectedTokens);
        tokenSelectionPane.setTokenAnalysis(loadedAnalysis);
      }
    }
  }

  /**
   * Cleans up resources when the pattern builder is no longer needed. Task 12: Properly manages
   * background processing and cache resources.
   */
  public void cleanup() {
    // Cancel any running background tasks and cleanup resources
    if (fileAnalysisPane != null) {
      fileAnalysisPane.cleanup();
    }

    if (previewPane != null) {
      previewPane.shutdown();
    }

    // Clear analysis cache to free memory
    analysisCache.clear();

    ValidationLogger.logUserAction("Cleanup", "SimplePatternBuilder resources cleaned up");
  }
}
