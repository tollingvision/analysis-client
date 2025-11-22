package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * UI component for visual token display and drag-and-drop interaction. Allows users to see detected
 * tokens and their suggested types, with the ability to modify token types through drag-and-drop or
 * selection.
 */
public class TokenSelectionPane extends VBox {

  private TokenAnalysis tokenAnalysis;
  private FlowPane tokenDisplay;
  private FlowPane tokenTypesPanel;
  private TextArea explanationArea;
  private CustomTokenManager customTokenManager;
  private FilenameTokenizer tokenizer;
  private java.util.Set<TokenType> optionalTokenTypes;
  private java.util.Set<String> optionalCustomTokenNames;

  /** Creates a new TokenSelectionPane with visual token display. */
  public TokenSelectionPane() {
    this.customTokenManager = new CustomTokenManager();
    this.tokenizer = new FilenameTokenizer();
    this.optionalTokenTypes = new java.util.HashSet<>();
    this.optionalCustomTokenNames = new java.util.HashSet<>();

    initializeComponents();
    setupLayout();
    setupDragAndDrop();
  }

  /**
   * Creates a new TokenSelectionPane with custom token manager.
   *
   * @param customTokenManager the custom token manager to use
   */
  public TokenSelectionPane(CustomTokenManager customTokenManager) {
    this.customTokenManager =
        customTokenManager != null ? customTokenManager : new CustomTokenManager();
    this.tokenizer = new FilenameTokenizer();
    this.optionalTokenTypes = new java.util.HashSet<>();
    this.optionalCustomTokenNames = new java.util.HashSet<>();

    initializeComponents();
    setupLayout();
    setupDragAndDrop();
  }

  /**
   * Sets the optional token types.
   *
   * @param optionalTokenTypes the set of token types that are optional
   */
  public void setOptionalTokenTypes(java.util.Set<TokenType> optionalTokenTypes) {
    this.optionalTokenTypes = optionalTokenTypes;
  }

  /**
   * Gets the optional token types.
   *
   * @return the set of token types that are optional
   */
  public java.util.Set<TokenType> getOptionalTokenTypes() {
    return optionalTokenTypes;
  }

  /**
   * Sets the optional custom token names.
   *
   * @param optionalCustomTokenNames the set of custom token names that are optional
   */
  public void setOptionalCustomTokenNames(java.util.Set<String> optionalCustomTokenNames) {
    this.optionalCustomTokenNames = optionalCustomTokenNames;
  }

  /**
   * Gets the optional custom token names.
   *
   * @return the set of custom token names that are optional
   */
  public java.util.Set<String> getOptionalCustomTokenNames() {
    return optionalCustomTokenNames;
  }

  /** Initializes all UI components. */
  private void initializeComponents() {
    tokenDisplay = new FlowPane();
    tokenDisplay.setHgap(10);
    tokenDisplay.setVgap(10);
    tokenDisplay.setPadding(new Insets(15));
    tokenDisplay.setPrefWrapLength(400); // Enable proper wrapping
    tokenDisplay.setStyle(
        "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
    Tooltip tokenDisplayTooltip =
        new Tooltip(
            "Detected filename tokens - drag these chips to different categories below to"
                + " reclassify them");
    Tooltip.install(tokenDisplay, tokenDisplayTooltip);

    tokenTypesPanel = new FlowPane();
    tokenTypesPanel.setHgap(15);
    tokenTypesPanel.setVgap(15);
    tokenTypesPanel.setPadding(new Insets(15));
    tokenTypesPanel.setStyle(
        "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5;");

    explanationArea = new TextArea();
    explanationArea.setEditable(false);
    explanationArea.setPrefRowCount(4);
    explanationArea.setWrapText(true);
    explanationArea.setText(
        "Drag tokens to different type categories to adjust their classification. The system has"
            + " automatically detected token types based on common patterns. You can override these"
            + " suggestions by dragging tokens to the appropriate categories.");
  }

  /** Sets up the layout structure. */
  private void setupLayout() {
    setSpacing(20);
    setPadding(new Insets(20));

    // Title
    Label title = new Label("Step 1: Token Selection");
    title.getStyleClass().add("step-title");

    // Description (includes instructions text)
    Label description =
        new Label(
            "Drag and drop detected tokens between categories or use the custom token dialog to"
                + " define new tokens. Review the detected tokens and their suggested types. You"
                + " can drag tokens between categories to correct any misclassifications.");
    description.setWrapText(true);
    description.getStyleClass().add("step-description");

    // Token display section
    Label tokensLabel = new Label("Detected Tokens:");
    tokensLabel.getStyleClass().add("section-label");

    // Token types panel
    Label typesLabel = new Label("Token Types:");
    typesLabel.getStyleClass().add("section-label");

    // Create vertical split layout: tokens on top, types on bottom
    VBox mainContent = new VBox(20);
    mainContent.setFillWidth(true);
    VBox.setVgrow(mainContent, Priority.ALWAYS);

    // Top section: Detected tokens (fit to content height)
    VBox topSection = new VBox(10);
    topSection.getChildren().add(tokensLabel);

    ScrollPane topScrollPane = new ScrollPane(tokenDisplay);
    topScrollPane.setFitToWidth(true);
    topScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    topScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    topScrollPane.setPrefViewportHeight(-1); // Auto-size to content
    topScrollPane.setMinHeight(50);
    topScrollPane.setMaxHeight(150);

    topSection.getChildren().add(topScrollPane);

    // Bottom section: Token types (fit to content height, 2 rows)
    VBox bottomSection = new VBox(10);
    bottomSection.getChildren().add(typesLabel);

    // Wrap tokenTypesPanel in ScrollPane - fit to content
    ScrollPane bottomScrollPane = new ScrollPane(tokenTypesPanel);
    bottomScrollPane.setFitToWidth(true);
    bottomScrollPane.setFitToHeight(true); // Fit to content
    bottomScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    bottomScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    bottomScrollPane.setPrefHeight(-1); // Auto-size to content
    bottomScrollPane.setMinHeight(180);
    bottomScrollPane.setMaxHeight(500);

    bottomSection.getChildren().add(bottomScrollPane);

    mainContent.getChildren().addAll(topSection, bottomSection);

    // Instructions moved to description, explanationArea removed
    getChildren().addAll(title, description, mainContent);
  }

  /** Sets up drag and drop functionality for token classification. */
  private void setupDragAndDrop() {
    // Token types will accept drops - initial setup
    // Actual drop zones are created dynamically in displayTokens()
    // to include both standard and custom token types
  }

  /** Creates a drop zone for a specific token type. */
  private void createTokenTypeDropZone(TokenType tokenType) {
    VBox dropZone = new VBox(5);
    dropZone.setPadding(new Insets(10));
    dropZone.setPrefWidth(250);
    dropZone.setMinWidth(200);
    dropZone.setMaxWidth(300);
    dropZone.setMinHeight(120);

    // Update style based on optional status
    updateDropZoneStyle(dropZone, tokenType, false);
    dropZone.setAlignment(Pos.TOP_LEFT);

    // Add optional indicator to label if type is optional
    String labelText = formatTokenTypeName(tokenType);
    if (optionalTokenTypes.contains(tokenType)) {
      labelText += " (?)";
    }

    Label typeLabel = new Label(labelText);
    typeLabel.getStyleClass().add("token-type-label");
    typeLabel.setStyle(
        "-fx-font-weight: bold; -fx-text-fill: " + getTokenTypeColor(tokenType) + ";");

    Label descriptionLabel = new Label(getTokenTypeDescription(tokenType));
    descriptionLabel.getStyleClass().add("token-type-description");
    descriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
    descriptionLabel.setWrapText(true);

    FlowPane tokenContainer = new FlowPane(5, 5);

    dropZone.getChildren().addAll(typeLabel, descriptionLabel, tokenContainer);

    // Set up drag over and drop handlers
    dropZone.setOnDragOver(
        event -> {
          if (event.getGestureSource() != dropZone && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
            updateDropZoneStyle(dropZone, tokenType, true);
          }
          event.consume();
        });

    dropZone.setOnDragExited(
        event -> {
          updateDropZoneStyle(dropZone, tokenType, false);
          event.consume();
        });

    dropZone.setOnDragDropped(
        event -> {
          Dragboard db = event.getDragboard();
          boolean success = false;

          if (db.hasString()) {
            String tokenValue = db.getString();
            moveTokenToType(tokenValue, tokenType, tokenContainer);
            success = true;
          }

          event.setDropCompleted(success);
          event.consume();
        });

    // Add context menu for optional toggle - create on-demand to ensure current
    // state
    dropZone.setOnContextMenuRequested(
        event -> {
          javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
          javafx.scene.control.MenuItem toggleOptionalItem =
              new javafx.scene.control.MenuItem(
                  optionalTokenTypes.contains(tokenType)
                      ? "✓ Mark Type as Required"
                      : "Mark Type as Optional (?)");
          toggleOptionalItem.setOnAction(
              e -> {
                if (optionalTokenTypes.contains(tokenType)) {
                  optionalTokenTypes.remove(tokenType);
                  System.out.println(
                      "DEBUG TokenSelectionPane: Removed " + tokenType + " from optional types");
                } else {
                  optionalTokenTypes.add(tokenType);
                  System.out.println(
                      "DEBUG TokenSelectionPane: Added "
                          + tokenType
                          + " to optional types. Set now contains: "
                          + optionalTokenTypes);
                  System.out.println(
                      "DEBUG TokenSelectionPane: Set identity = "
                          + System.identityHashCode(optionalTokenTypes));
                }
                // Refresh the display to show updated label
                displayTokens();
              });

          javafx.scene.control.SeparatorMenuItem separator =
              new javafx.scene.control.SeparatorMenuItem();
          javafx.scene.control.MenuItem infoItem =
              new javafx.scene.control.MenuItem("ℹ Optional types may not appear in all filenames");
          infoItem.setDisable(true);

          contextMenu.getItems().addAll(toggleOptionalItem, separator, infoItem);
          contextMenu.show(dropZone, event.getScreenX(), event.getScreenY());
          event.consume();
        });

    tokenTypesPanel.getChildren().add(dropZone);
  }

  /** Updates drop zone style based on optional status. */
  private void updateDropZoneStyle(VBox dropZone, TokenType tokenType, boolean isHover) {
    String baseColor = optionalTokenTypes.contains(tokenType) ? "#fff3cd" : "#ffffff";
    String borderColor = isHover ? "#2196f3" : "#ced4da";

    dropZone.setStyle(
        String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 3;",
            baseColor, borderColor));
  }

  /** Creates a drop zone for a custom token type. */
  private void createCustomTokenDropZone(CustomTokenManager.CustomToken customToken) {
    VBox dropZone = new VBox(5);
    dropZone.setPadding(new Insets(10));
    dropZone.setPrefWidth(250);
    dropZone.setMinWidth(200);
    dropZone.setMaxWidth(300);
    dropZone.setMinHeight(120);

    // Check if this custom token is optional
    String tokenName = customToken.getName();
    boolean isOptional = optionalCustomTokenNames.contains(tokenName);

    String backgroundColor = isOptional ? "#fff3cd" : "#f0f8ff";
    dropZone.setStyle(
        "-fx-background-color: "
            + backgroundColor
            + "; -fx-border-color: #4169e1; "
            + "-fx-border-radius: 3; -fx-border-width: 2;");
    dropZone.setAlignment(Pos.TOP_LEFT);

    // Add optional indicator to label if type is optional
    String labelText = customToken.getName();
    if (isOptional) {
      labelText += " (?)";
    }

    Label typeLabel = new Label(labelText);
    typeLabel.getStyleClass().add("token-type-label");
    typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4169e1;");

    Label descriptionLabel = new Label(customToken.getDescription());
    descriptionLabel.getStyleClass().add("token-type-description");
    descriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
    descriptionLabel.setWrapText(true);

    FlowPane tokenContainer = new FlowPane(5, 5);
    tokenContainer.setUserData(customToken); // Store custom token reference

    dropZone.getChildren().addAll(typeLabel, descriptionLabel, tokenContainer);

    // Set up drag over and drop handlers
    dropZone.setOnDragOver(
        event -> {
          if (event.getGestureSource() != dropZone && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
            dropZone.setStyle(
                "-fx-background-color: #e6f2ff; -fx-border-color: #1e90ff; "
                    + "-fx-border-radius: 3; -fx-border-width: 2; -fx-min-height: 60;");
          }
          event.consume();
        });

    dropZone.setOnDragExited(
        event -> {
          String bgColor = optionalCustomTokenNames.contains(tokenName) ? "#fff3cd" : "#f0f8ff";
          dropZone.setStyle(
              "-fx-background-color: "
                  + bgColor
                  + "; -fx-border-color: #4169e1; "
                  + "-fx-border-radius: 3; -fx-border-width: 2; -fx-min-height: 60;");
          event.consume();
        });

    dropZone.setOnDragDropped(
        event -> {
          Dragboard db = event.getDragboard();
          boolean success = false;

          if (db.hasString()) {
            String tokenValue = db.getString();
            // Map custom token to its underlying TokenType
            moveTokenToType(tokenValue, customToken.getMappedType(), tokenContainer);
            success = true;
          }

          event.setDropCompleted(success);
          event.consume();
        });

    // Add context menu for optional toggle - create on-demand to ensure current
    // state
    dropZone.setOnContextMenuRequested(
        event -> {
          javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
          javafx.scene.control.MenuItem toggleOptionalItem =
              new javafx.scene.control.MenuItem(
                  optionalCustomTokenNames.contains(tokenName)
                      ? "✓ Mark Type as Required"
                      : "Mark Type as Optional (?)");
          toggleOptionalItem.setOnAction(
              e -> {
                if (optionalCustomTokenNames.contains(tokenName)) {
                  optionalCustomTokenNames.remove(tokenName);
                } else {
                  optionalCustomTokenNames.add(tokenName);
                }
                // Refresh the display to show updated label
                displayTokens();
              });

          javafx.scene.control.SeparatorMenuItem separator =
              new javafx.scene.control.SeparatorMenuItem();
          javafx.scene.control.MenuItem infoItem =
              new javafx.scene.control.MenuItem("ℹ Optional types may not appear in all filenames");
          infoItem.setDisable(true);

          contextMenu.getItems().addAll(toggleOptionalItem, separator, infoItem);
          contextMenu.show(dropZone, event.getScreenX(), event.getScreenY());
          event.consume();
        });

    tokenTypesPanel.getChildren().add(dropZone);
  }

  /** Creates a draggable token chip. */
  private Label createTokenChip(FilenameToken token) {
    Label chip = new Label(token.getValue());
    chip.getStyleClass().add("token-chip"); // Check if this is a front token and highlight it
    boolean isFrontToken = tokenizer.isFrontToken(token.getValue());
    String backgroundColor = getTokenTypeColor(token.getSuggestedType());

    if (isFrontToken) {
      // Add special styling for front tokens
      chip.setStyle(
          String.format(
              "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 5 10; "
                  + "-fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 12px; "
                  + "-fx-border-color: #ff6b35; -fx-border-width: 2px; -fx-border-radius: 15;",
              backgroundColor));
    } else {
      chip.setStyle(
          String.format(
              "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 5 10; "
                  + "-fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 12px;",
              backgroundColor));
    }

    // Enhanced tooltip with additional information
    StringBuilder tooltipText = new StringBuilder();
    tooltipText.append(
        String.format(
            "Type: %s\nConfidence: %.1f%%\nPosition: %d",
            formatTokenTypeName(token.getSuggestedType()),
            token.getConfidence() * 100,
            token.getPosition()));

    if (isFrontToken) {
      tooltipText.append("\n\n✓ Front camera token detected");
      ImageRole role = tokenizer.getImageRoleForToken(token.getValue());
      if (role != null) {
        tooltipText.append("\nRole: ").append(role.name());
      }
    }

    // Check if this matches a custom token
    CustomTokenManager.CustomToken customToken =
        customTokenManager.findMatchingCustomToken(token.getValue());
    if (customToken != null) {
      tooltipText.append("\n\n✓ Custom token: ").append(customToken.getName());
      tooltipText.append("\nDescription: ").append(customToken.getDescription());
    }

    Tooltip tooltip = new Tooltip(tooltipText.toString());
    Tooltip.install(chip, tooltip);

    // Set up drag detection
    chip.setOnDragDetected(
        event -> {
          Dragboard db = chip.startDragAndDrop(TransferMode.MOVE);
          ClipboardContent content = new ClipboardContent();
          content.putString(token.getValue());
          db.setContent(content);
          event.consume();
        });

    return chip;
  }

  /** Moves a token to a new type category. */
  private void moveTokenToType(String tokenValue, TokenType newType, FlowPane targetContainer) {
    if (tokenAnalysis == null) return;

    // Check if target is a custom token drop zone
    Object userData = targetContainer.getUserData();
    CustomTokenManager.CustomToken targetCustomToken = null;
    if (userData instanceof CustomTokenManager.CustomToken) {
      targetCustomToken = (CustomTokenManager.CustomToken) userData;
    }

    // Remove token from ALL custom token examples first (using manager's method)
    customTokenManager.removeTokenFromAllCustomTokens(tokenValue);

    // If dropping into a custom token zone, add to that custom token's examples
    if (targetCustomToken != null) {
      customTokenManager.addTokenToCustomToken(targetCustomToken.getName(), tokenValue);
    }

    // Find and update the token's suggested type
    // Only update suggestedType if dropping into a standard zone (not custom)
    if (targetCustomToken == null) {
      for (List<FilenameToken> tokens : tokenAnalysis.getTokenizedFilenames().values()) {
        for (FilenameToken token : tokens) {
          if (token.getValue().equals(tokenValue)) {
            token.setSuggestedType(newType);
            token.setConfidence(0.9); // High confidence for manual assignment
          }
        }
      }
    }

    // Refresh display
    displayTokens();
  }

  /** Displays tokens organized by their suggested types. */
  private void displayTokens() {
    if (tokenAnalysis == null) return;

    // Clear existing displays
    tokenDisplay.getChildren().clear();
    tokenTypesPanel.getChildren().clear();

    // Recreate token type drop zones for standard types
    for (TokenType tokenType : TokenType.values()) {
      if (tokenType != TokenType.UNKNOWN) {
        createTokenTypeDropZone(tokenType);
      }
    }

    // Add drop zones for custom tokens
    for (CustomTokenManager.CustomToken customToken : customTokenManager.getAllCustomTokens()) {
      createCustomTokenDropZone(customToken);
    }

    // Set<TokenType> seen = EnumSet.noneOf(TokenType.class);

    // Get representative tokens from first filename
    // if (!tokenAnalysis.getFilenames().isEmpty()) {
    // String firstFilename = tokenAnalysis.getFilenames().get(0);
    // List<FilenameToken> tokens =
    // tokenAnalysis.getTokensForFilename(firstFilename);

    List<FilenameToken> tokens =
        tokenAnalysis.getTokenizedFilenames().values().stream()
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .distinct()
            /*
             * .filter(t -> t.getSuggestedType() != null)
             * .filter(t -> {
             * return t.getSuggestedType() == TokenType.CAMERA_SIDE ? true :
             * seen.add(t.getSuggestedType());
             * })
             */
            .collect(Collectors.toList());

    // Display tokens in main area
    for (FilenameToken token : tokens) {
      Label chip = createTokenChip(token);
      tokenDisplay.getChildren().add(chip);
    }

    // Organize tokens by type in drop zones with deduplication
    // Custom tokens have priority - track already placed tokens
    java.util.Set<String> placedTokens = new java.util.HashSet<>();

    // First pass: Place custom tokens
    for (int i = 0; i < tokenTypesPanel.getChildren().size(); i++) {
      VBox dropZone = (VBox) tokenTypesPanel.getChildren().get(i);
      FlowPane tokenContainer = (FlowPane) dropZone.getChildren().get(2);
      Object userData = tokenContainer.getUserData();

      if (userData instanceof CustomTokenManager.CustomToken customToken) {
        // Match tokens against custom token examples
        for (FilenameToken token : tokens) {
          String tokenValue = token.getValue().toLowerCase();
          if (customToken.getExamples().contains(tokenValue)
              && !placedTokens.contains(token.getValue())) {
            Label chip = createTokenChip(token);
            tokenContainer.getChildren().add(chip);
            placedTokens.add(token.getValue());
          }
        }
      }
    }

    // Second pass: Place standard tokens (only if not already placed)
    int standardTypeIndex = 0;
    for (int i = 0; i < tokenTypesPanel.getChildren().size(); i++) {
      VBox dropZone = (VBox) tokenTypesPanel.getChildren().get(i);
      FlowPane tokenContainer = (FlowPane) dropZone.getChildren().get(2);
      Object userData = tokenContainer.getUserData();

      if (!(userData instanceof CustomTokenManager.CustomToken)) {
        // Standard token type drop zone
        TokenType tokenType = TokenType.values()[standardTypeIndex];
        standardTypeIndex++;

        for (FilenameToken token : tokens) {
          if (token.getSuggestedType() == tokenType && !placedTokens.contains(token.getValue())) {
            Label chip = createTokenChip(token);
            tokenContainer.getChildren().add(chip);
            placedTokens.add(token.getValue());
          }
        }
      }
    }
    // }
  }

  /** Formats token type name for display. */
  private String formatTokenTypeName(TokenType tokenType) {
    return switch (tokenType) {
      case PREFIX -> "Prefix";
      case SUFFIX -> "Suffix";
      case GROUP_ID -> "Group ID";
      case CAMERA_SIDE -> "Camera/Side";
      case DATE -> "Date";
      case INDEX -> "Index";
      case EXTENSION -> "Extension";
      case UNKNOWN -> "Unknown";
    };
  }

  /** Gets description for token type. */
  private String getTokenTypeDescription(TokenType tokenType) {
    return switch (tokenType) {
      case PREFIX -> "Fixed text at the beginning";
      case SUFFIX -> "Fixed text at the end";
      case GROUP_ID -> "Unique identifier for each vehicle";
      case CAMERA_SIDE -> "Camera position or image side";
      case DATE -> "Date or timestamp";
      case INDEX -> "Numeric sequence or index";
      case EXTENSION -> "File extension";
      case UNKNOWN -> "Unclassified token";
    };
  }

  /** Gets color for token type visualization. */
  private String getTokenTypeColor(TokenType tokenType) {
    return switch (tokenType) {
      case PREFIX -> "#6c757d";
      case SUFFIX -> "#6c757d";
      case GROUP_ID -> "#dc3545";
      case CAMERA_SIDE -> "#fd7e14";
      case DATE -> "#20c997";
      case INDEX -> "#0d6efd";
      case EXTENSION -> "#6f42c1";
      case UNKNOWN -> "#adb5bd";
    };
  }

  /** Sets the token analysis results and updates the display. */
  public void setTokenAnalysis(TokenAnalysis analysis) {
    // Enhance analysis with custom tokens
    this.tokenAnalysis = analysis /* customTokenManager.enhanceWithCustomTokens(analysis) */;
    displayTokens();
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
   * Sets the custom token manager.
   *
   * @param customTokenManager the custom token manager to use
   */
  public void setCustomTokenManager(CustomTokenManager customTokenManager) {
    this.customTokenManager =
        customTokenManager != null ? customTokenManager : new CustomTokenManager();
  }

  /**
   * @return the current token analysis
   */
  public TokenAnalysis getTokenAnalysis() {
    return tokenAnalysis;
  }
}
