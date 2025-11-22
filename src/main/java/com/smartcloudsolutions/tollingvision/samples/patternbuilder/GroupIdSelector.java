package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * UI component for Group ID selection with required selection validation and blocking logic.
 * Provides a clear interface for users to select which token should be used as the vehicle group
 * identifier, with validation to ensure exactly one token is selected.
 */
public class GroupIdSelector extends VBox {

  private final ResourceBundle messages;
  private final ObjectProperty<TokenType> selectedGroupId = new SimpleObjectProperty<>();
  private final ObservableList<FilenameToken> availableTokens = FXCollections.observableArrayList();
  private final ToggleGroup tokenSelectionGroup = new ToggleGroup();
  private CustomTokenManager customTokenManager;

  // UI Components
  private VBox tokenGroupsContainer;
  private ScrollPane tokenScrollPane;
  private TextArea previewArea;
  private Label validationLabel;
  private Button clearSelectionButton;

  /** Creates a new GroupIdSelector with token selection and validation. */
  public GroupIdSelector() {
    this(java.util.ResourceBundle.getBundle("messages"));
  }

  public GroupIdSelector(ResourceBundle messages) {
    this.messages = messages;
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setupValidation();
  }

  /** Initializes all UI components. */
  private void initializeComponents() {
    tokenGroupsContainer = new VBox(15);
    tokenGroupsContainer.setPadding(new Insets(10));

    tokenScrollPane = new ScrollPane(tokenGroupsContainer);
    tokenScrollPane.setFitToWidth(true);
    tokenScrollPane.setPrefHeight(300);
    tokenScrollPane.setStyle("-fx-background-color: #f8f9fa;");
    Tooltip tokenScrollTooltip =
        new Tooltip(
            "Select the token that uniquely identifies each vehicle.\n"
                + "Good choices: license plate, vehicle ID, unique identifier\n"
                + "Avoid: camera side, date, or tokens that vary within same vehicle");
    Tooltip.install(tokenScrollPane, tokenScrollTooltip);

    previewArea = new TextArea();
    previewArea.setEditable(false);
    previewArea.setPrefRowCount(6);
    previewArea.setWrapText(true);
    previewArea.setPromptText(messages.getString("group.id.preview.prompt"));
    previewArea.setTooltip(
        new Tooltip(
            "Preview shows the generated regex pattern and how it will extract the Group ID\n"
                + "from your filenames for grouping images of the same vehicle together"));

    validationLabel = new Label();
    validationLabel.getStyleClass().add("validation-label");
    validationLabel.setWrapText(true);

    clearSelectionButton = new Button(messages.getString("button.clear.selection"));
    clearSelectionButton.setDisable(true);
    clearSelectionButton.setTooltip(
        new Tooltip("Clear the currently selected Group ID token to choose a different one"));
  }

  /** Sets up the layout structure. */
  private void setupLayout() {
    setSpacing(20);
    setPadding(new Insets(20));

    // Title
    Label title = new Label(messages.getString("group.id.title"));
    title.getStyleClass().add("step-title");

    // Description
    Label description = new Label(messages.getString("group.id.description"));
    description.setWrapText(true);
    description.getStyleClass().add("step-description");

    // Requirements box
    VBox requirementsBox = new VBox(5);
    requirementsBox.setPadding(new Insets(15));
    requirementsBox.setStyle(
        "-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; -fx-border-radius: 5;");

    Label requirementsTitle = new Label(messages.getString("group.id.requirements.title"));
    requirementsTitle.setStyle("-fx-font-weight: bold;");

    Label requirement1 = new Label(messages.getString("group.id.requirement.1"));
    Label requirement2 = new Label(messages.getString("group.id.requirement.2"));
    Label requirement3 = new Label(messages.getString("group.id.requirement.3"));

    requirementsBox
        .getChildren()
        .addAll(requirementsTitle, requirement1, requirement2, requirement3);

    // Token selection section
    Label tokensLabel = new Label(messages.getString("group.id.tokens.label"));
    tokensLabel.getStyleClass().add("section-label");

    HBox selectionControls = new HBox(10);
    selectionControls.setAlignment(Pos.CENTER_LEFT);
    selectionControls.getChildren().add(clearSelectionButton);

    // Preview section
    Label previewLabel = new Label(messages.getString("group.id.preview.label"));
    previewLabel.getStyleClass().add("section-label");

    getChildren()
        .addAll(
            title,
            description,
            requirementsBox,
            tokensLabel,
            tokenScrollPane,
            selectionControls,
            validationLabel,
            previewLabel,
            previewArea);
  }

  /** Sets up event handlers for user interactions. */
  private void setupEventHandlers() {
    // Handle clear selection
    clearSelectionButton.setOnAction(
        e -> {
          tokenSelectionGroup.selectToggle(null);
          selectedGroupId.set(null);
          clearSelectionButton.setDisable(true);
          updatePreview();
          updateValidation();
        });

    // Listen to toggle group changes
    tokenSelectionGroup
        .selectedToggleProperty()
        .addListener(
            (obs, oldToggle, newToggle) -> {
              if (newToggle != null) {
                TokenType tokenType = (TokenType) newToggle.getUserData();
                selectedGroupId.set(tokenType);
                clearSelectionButton.setDisable(false);
              } else {
                selectedGroupId.set(null);
                clearSelectionButton.setDisable(true);
              }
              updatePreview();
              updateValidation();
            });
  }

  /** Sets up validation logic and feedback. */
  private void setupValidation() {
    selectedGroupId.addListener((obs, oldVal, newVal) -> updateValidation());
    updateValidation(); // Initial validation
  }

  /** Updates validation status and feedback. */
  private void updateValidation() {
    if (selectedGroupId.get() == null) {
      validationLabel.setText(messages.getString("validation.error.no.group.id"));
      validationLabel.setStyle(
          "-fx-text-fill: #856404; -fx-background-color: #fff3cd; "
              + "-fx-padding: 8; -fx-background-radius: 3;");
    } else {
      TokenType tokenType = selectedGroupId.get();
      if (tokenType == TokenType.GROUP_ID) {
        validationLabel.setText(messages.getString("group.id.validation.good"));
        validationLabel.setStyle(
            "-fx-text-fill: #155724; -fx-background-color: #d4edda; "
                + "-fx-padding: 8; -fx-background-radius: 3;");
      } else if (tokenType == TokenType.INDEX || tokenType == TokenType.DATE) {
        validationLabel.setText(messages.getString("group.id.validation.maybe"));
        validationLabel.setStyle(
            "-fx-text-fill: #856404; -fx-background-color: #fff3cd; "
                + "-fx-padding: 8; -fx-background-radius: 3;");
      } else {
        validationLabel.setText(messages.getString("group.id.validation.bad"));
        validationLabel.setStyle(
            "-fx-text-fill: #721c24; -fx-background-color: #f8d7da; "
                + "-fx-padding: 8; -fx-background-radius: 3;");
      }
    }
  }

  /** Updates the group pattern preview based on selected token type. */
  private void updatePreview() {
    if (selectedGroupId.get() == null || availableTokens.isEmpty()) {
      previewArea.setText(messages.getString("group.id.preview.prompt"));
      return;
    }

    TokenType selectedType = selectedGroupId.get();

    // Find all tokens of the selected type
    List<FilenameToken> tokensOfType =
        availableTokens.stream().filter(token -> token.getSuggestedType() == selectedType).toList();

    if (tokensOfType.isEmpty()) {
      previewArea.setText(messages.getString("group.id.preview.prompt"));
      return;
    }

    // Get representative token (first one) for preview
    FilenameToken representativeToken = tokensOfType.get(0);

    StringBuilder preview = new StringBuilder();
    preview.append(messages.getString("group.id.preview.selected.title")).append('\n');
    preview.append("=======================\n\n");

    preview
        .append(
            String.format(
                messages.getString("group.id.preview.type"), formatTokenTypeName(selectedType)))
        .append('\n');
    preview.append(String.format("Token Group: %d tokens\n", tokensOfType.size()));
    preview.append(
        String.format(
            "Examples: %s\n",
            tokensOfType.stream()
                .limit(5)
                .map(FilenameToken::getValue)
                .collect(java.util.stream.Collectors.joining(", "))));
    preview.append('\n');

    preview.append(messages.getString("group.id.preview.generated.title")).append('\n');
    preview.append("=======================\n\n");

    // Generate a simple pattern preview using representative token
    String pattern = generateGroupPatternPreview(representativeToken);
    preview
        .append(String.format(messages.getString("group.id.preview.regex"), pattern))
        .append('\n')
        .append('\n');

    preview.append(messages.getString("group.id.preview.explanation.title")).append('\n');
    preview.append("===================\n\n");

    preview.append(messages.getString("group.id.preview.explanation.intro")).append('\n');
    preview
        .append(messages.getString("group.id.preview.explanation.bullet.structure"))
        .append('\n');
    preview
        .append(
            String.format(
                messages.getString("group.id.preview.explanation.bullet.extract"),
                representativeToken.getPosition()))
        .append('\n');
    preview.append(messages.getString("group.id.preview.explanation.bullet.capture")).append('\n');
    preview
        .append(messages.getString("group.id.preview.explanation.bullet.grouping"))
        .append('\n')
        .append('\n');

    preview.append(messages.getString("group.id.preview.examples.title")).append('\n');
    preview.append("===============\n\n");

    // Show example matches
    preview.append(messages.getString("group.id.preview.examples.intro")).append('\n');
    preview.append(messages.getString("group.id.preview.examples.line1")).append('\n');
    preview.append(messages.getString("group.id.preview.examples.line2")).append('\n');
    preview.append(messages.getString("group.id.preview.examples.line3")).append('\n').append('\n');

    preview.append(messages.getString("group.id.preview.examples.summary.line1")).append('\n');
    preview.append(messages.getString("group.id.preview.examples.summary.line2"));

    previewArea.setText(preview.toString());
  }

  /** Generates a preview of the group pattern regex. */
  private String generateGroupPatternPreview(FilenameToken groupToken) {
    StringBuilder pattern = new StringBuilder();

    // Build pattern based on token positions
    for (int i = 0; i < availableTokens.size(); i++) {
      if (i > 0) {
        pattern.append("[_\\-\\.\\s]+"); // Delimiter pattern
      }

      if (i == groupToken.getPosition()) {
        // This is the group ID token - add capturing group
        pattern.append("([^_\\-\\.\\s]+)");
      } else {
        // Other tokens - match but don't capture
        pattern.append("[^_\\-\\.\\s]+");
      }
    }

    return pattern.toString();
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

  /** Sets the available tokens for selection. */
  public void setAvailableTokens(List<FilenameToken> tokens) {
    // Preserve current selection
    TokenType currentSelection = selectedGroupId.get();

    availableTokens.clear();
    if (tokens != null) {
      availableTokens.addAll(tokens);
    }

    displayGroupedTokens();

    // Restore previous selection if available
    if (currentSelection != null) {
      tokenSelectionGroup.getToggles().stream()
          .filter(toggle -> toggle.getUserData() == currentSelection)
          .findFirst()
          .ifPresent(toggle -> tokenSelectionGroup.selectToggle(toggle));
    } else if (tokens != null) {
      // Auto-select GROUP_ID token type if available and nothing is selected
      boolean hasGroupIdType =
          tokens.stream().anyMatch(token -> token.getSuggestedType() == TokenType.GROUP_ID);

      if (hasGroupIdType) {
        tokenSelectionGroup.getToggles().stream()
            .filter(toggle -> toggle.getUserData() == TokenType.GROUP_ID)
            .findFirst()
            .ifPresent(toggle -> tokenSelectionGroup.selectToggle(toggle));
      }
    }
  }

  /** Displays tokens grouped by type. */
  private void displayGroupedTokens() {
    tokenGroupsContainer.getChildren().clear();
    tokenSelectionGroup.getToggles().clear();

    if (availableTokens.isEmpty()) {
      return;
    }

    // Group tokens by type
    Map<TokenType, List<FilenameToken>> tokensByType =
        availableTokens.stream().collect(Collectors.groupingBy(FilenameToken::getSuggestedType));

    // Get custom token information if available
    Map<String, String> customTokenNames = new java.util.HashMap<>();
    if (customTokenManager != null) {
      for (CustomTokenManager.CustomToken customToken : customTokenManager.getAllCustomTokens()) {
        // Map token values to custom token names
        for (String example : customToken.getExamples()) {
          customTokenNames.put(example.toLowerCase(), customToken.getName());
        }
      }
    }

    // Display each token type group
    for (TokenType tokenType : TokenType.values()) {
      if (tokenType == TokenType.UNKNOWN) {
        continue;
      }

      List<FilenameToken> tokensOfType = tokensByType.get(tokenType);
      if (tokensOfType != null && !tokensOfType.isEmpty()) {
        createTokenTypeGroup(tokenType, tokensOfType, customTokenNames);
      }
    }
  }

  /** Creates a group display for a specific token type. */
  private void createTokenTypeGroup(
      TokenType tokenType, List<FilenameToken> tokens, Map<String, String> customTokenNames) {
    VBox groupBox = new VBox(8);
    groupBox.setPadding(new Insets(10));
    groupBox.setStyle(
        "-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");

    // Create a single toggle button for the entire token type
    ToggleButton typeButton = createTokenTypeButton(tokenType, tokens, customTokenNames);
    typeButton.setToggleGroup(tokenSelectionGroup);
    typeButton.setUserData(tokenType);

    groupBox.getChildren().add(typeButton);
    tokenGroupsContainer.getChildren().add(groupBox);
  }

  /** Creates a toggle button for a token type group. */
  private ToggleButton createTokenTypeButton(
      TokenType tokenType, List<FilenameToken> tokens, Map<String, String> customTokenNames) {

    // Create display text showing token type and examples
    StringBuilder displayText = new StringBuilder();
    displayText.append(formatTokenTypeName(tokenType));
    displayText.append("\n");
    displayText.append(String.format("(%d tokens: ", tokens.size()));
    displayText.append(
        tokens.stream()
            .limit(5)
            .map(FilenameToken::getValue)
            .collect(java.util.stream.Collectors.joining(", ")));
    if (tokens.size() > 5) {
      displayText.append("...");
    }
    displayText.append(")");

    ToggleButton button = new ToggleButton(displayText.toString());
    button.setMaxWidth(Double.MAX_VALUE);
    button.setWrapText(true);
    button.setStyle(
        String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 12 16; "
                + "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; "
                + "-fx-alignment: center-left;",
            getTokenTypeColor(tokenType)));

    // Hover effect
    button.setOnMouseEntered(
        e -> {
          if (!button.isSelected()) {
            button.setStyle(
                String.format(
                    "-fx-background-color: derive(%s, -20%%); -fx-text-fill: white; "
                        + "-fx-padding: 12 16; -fx-background-radius: 8; -fx-font-size: 13px; "
                        + "-fx-cursor: hand; -fx-alignment: center-left;",
                    getTokenTypeColor(tokenType)));
          }
        });

    button.setOnMouseExited(
        e -> {
          if (!button.isSelected()) {
            button.setStyle(
                String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 12 16; "
                        + "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; "
                        + "-fx-alignment: center-left;",
                    getTokenTypeColor(tokenType)));
          }
        });

    // Selected style
    button
        .selectedProperty()
        .addListener(
            (obs, wasSelected, isSelected) -> {
              if (isSelected) {
                button.setStyle(
                    String.format(
                        "-fx-background-color: derive(%s, -30%%); -fx-text-fill: white; "
                            + "-fx-padding: 12 16; -fx-background-radius: 8; -fx-font-size: 13px; "
                            + "-fx-border-color: white; -fx-border-width: 3px; -fx-cursor: hand; "
                            + "-fx-alignment: center-left;",
                        getTokenTypeColor(tokenType)));
              } else {
                button.setStyle(
                    String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 12 16; "
                            + "-fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand; "
                            + "-fx-alignment: center-left;",
                        getTokenTypeColor(tokenType)));
              }
            });

    return button;
  }

  /**
   * Sets the custom token manager for displaying custom token information.
   *
   * @param customTokenManager the custom token manager
   */
  public void setCustomTokenManager(CustomTokenManager customTokenManager) {
    this.customTokenManager = customTokenManager;
  }

  /**
   * @return the selected group ID token type property
   */
  public ObjectProperty<TokenType> selectedGroupIdProperty() {
    return selectedGroupId;
  }

  /**
   * @return the currently selected group ID token type
   */
  public TokenType getSelectedGroupId() {
    return selectedGroupId.get();
  }
}
