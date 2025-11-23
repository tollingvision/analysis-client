package com.smartcloudsolutions.tollingvision.samples.ui;

import com.smartcloudsolutions.tollingvision.samples.model.ImageGroupResult;
import com.smartcloudsolutions.tollingvision.samples.model.UserConfiguration;
import com.smartcloudsolutions.tollingvision.samples.patternbuilder.PatternBuilderDialog;
import com.smartcloudsolutions.tollingvision.samples.patternbuilder.PatternConfiguration;
import com.smartcloudsolutions.tollingvision.samples.patternbuilder.PresetConfiguration;
import com.smartcloudsolutions.tollingvision.samples.patternbuilder.PresetManager;
import com.smartcloudsolutions.tollingvision.samples.util.ConfigurationManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main screen UI for the TollingVision application. Contains the configuration form, status
 * counters, and event log.
 */
public class MainScreen {

  private final ResourceBundle messages;
  private final Stage primaryStage;
  private final ConfigurationManager configManager;

  // In-memory configuration state
  private UserConfiguration currentUserConfig;

  // Configuration fields
  private final TextField dirField = new TextField();
  private final TextField urlField = new TextField("localhost:50051");
  private final CheckBox tlsCheck = new CheckBox();
  private final CheckBox insecureCheck = new CheckBox();
  private final TextField csvField = new TextField();
  private final Button csvBtn = new Button();
  private final Spinner<Integer> maxParSpin = new Spinner<>(1, 64, 4);

  // Current session CSV file path (not persisted in config)
  private String currentCsvFilePath = "";

  private final TextField groupPatternField = new TextField();
  private final TextField frontPatternField = new TextField();
  private final TextField rearPatternField = new TextField();
  private final TextField overviewPatternField = new TextField();

  // Control fields
  private final ProgressBar progressBar = new ProgressBar(0);
  private final Button startBtn = new Button();
  private final Button stopBtn = new Button();

  // Status counters
  private final IntegerProperty groupsDiscovered = new SimpleIntegerProperty(0);
  private final IntegerProperty requestsSent = new SimpleIntegerProperty(0);
  private final IntegerProperty responsesOk = new SimpleIntegerProperty(0);
  private final IntegerProperty responsesError = new SimpleIntegerProperty(0);

  private final BooleanProperty processing = new SimpleBooleanProperty(false);

  // Event log
  private final ObservableList<ImageGroupResult> logItems = FXCollections.observableArrayList();
  private final ListView<ImageGroupResult> logList = new ListView<>(logItems);

  // Callbacks for processing
  private Runnable onStartProcessing;
  private Runnable onStopProcessing;

  public static void addIcon(Stage stage) {
    stage
        .getIcons()
        .addAll(
            new Image(MainScreen.class.getResourceAsStream("/assets/logo_16_16.png")),
            new Image(MainScreen.class.getResourceAsStream("/assets/logo_32_32.png")),
            new Image(MainScreen.class.getResourceAsStream("/assets/logo_48_48.png")),
            new Image(MainScreen.class.getResourceAsStream("/assets/logo_128_128.png")));
  }

  /**
   * Creates a new MainScreen with the specified stage and resource bundle.
   *
   * @param primaryStage the primary stage for the application
   * @param messages resource bundle for internationalization
   */
  public MainScreen(Stage primaryStage, ResourceBundle messages) {
    this.primaryStage = primaryStage;
    this.messages = messages;
    this.configManager = new ConfigurationManager();

    initializeUI();
    setupEventHandlers();
    loadConfiguration();
    setupConfigurationAutoSave();
  }

  private void initializeUI() {
    MainScreen.addIcon(primaryStage);
    primaryStage.setTitle(messages.getString("app.title"));
    primaryStage.setMinWidth(1280);
    primaryStage.setMinHeight(800);

    // Create main layout
    VBox root = new VBox();
    root.getStyleClass().add("root");

    // Header with logo and branding
    HBox header = createHeader();

    // Main content area with two-column layout
    HBox mainContent = createMainContent();

    // Status and event log section
    VBox statusSection = createStatusSection();

    root.getChildren().addAll(header, mainContent, statusSection);
    HBox.setHgrow(mainContent, Priority.ALWAYS);
    VBox.setVgrow(statusSection, Priority.ALWAYS); // Make event log expand

    // Apply theme and create scene
    Scene scene = new Scene(root, 1280, 800);
    scene.getStylesheets().add(getClass().getResource("/tollingvision-theme.css").toExternalForm());

    primaryStage.setScene(scene);
  }

  private void setupEventHandlers() {
    // Event log keyboard handling
    logList.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            ImageGroupResult selected = logList.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.getAllImagePaths().isEmpty()) {
              openGallery(selected);
            }
            event.consume();
          }
        });

    // Event log mouse handling
    logList.setOnMouseClicked(
        ev -> {
          if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
            ImageGroupResult selected = logList.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.getAllImagePaths().isEmpty()) {
              openGallery(selected);
            }
          }
        });
  }

  private HBox createHeader() {
    HBox header = new HBox();
    header.getStyleClass().add("header-section");
    header.setAlignment(Pos.CENTER_LEFT);

    // Create a professional logo programmatically
    javafx.scene.canvas.Canvas logoCanvas = new javafx.scene.canvas.Canvas(48, 48);
    drawPngOnCanvas(logoCanvas, "/assets/logo_48_48.png");

    VBox branding = new VBox(2);
    Label titleLabel = new Label(messages.getString("app.title"));
    titleLabel.getStyleClass().add("app-title");
    Label subtitleLabel = new Label(messages.getString("app.subtitle"));
    subtitleLabel.getStyleClass().add("app-subtitle");
    branding.getChildren().addAll(titleLabel, subtitleLabel);

    HBox logoContainer = new HBox(15);
    logoContainer.getStyleClass().add("logo-container");
    logoContainer.getChildren().addAll(logoCanvas, branding);

    header.getChildren().add(logoContainer);
    return header;
  }

  private void drawPngOnCanvas(Canvas canvas, String resource) {
    // 1) Betöltés resources-ből (szinkron, InputStreammel)
    try (InputStream is = getClass().getResourceAsStream(resource)) {
      if (is == null) throw new IllegalStateException("Resource not found: " + resource);
      Image img = new Image(is); // szinkron betölt

      // 2) Rajzolás
      GraphicsContext g = canvas.getGraphicsContext2D();
      double cw = canvas.getWidth(), ch = canvas.getHeight();
      g.clearRect(0, 0, cw, ch);

      // Opcionális: méretezés, hogy beférjen a canvasba arányosan
      double s = Math.min(cw / img.getWidth(), ch / img.getHeight());
      double w = img.getWidth() * s, h = img.getHeight() * s;
      double x = (cw - w) / 2.0, y = (ch - h) / 2.0;

      g.drawImage(img, x, y, w, h);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private HBox createMainContent() {
    HBox mainContent = new HBox(20);
    mainContent.setPadding(new Insets(20));

    // Left column - Input Configuration
    VBox leftColumn = createInputConfigSection();

    // Right column - Connection & Runtime
    VBox rightColumn = createConnectionSection();

    mainContent.getChildren().addAll(leftColumn, rightColumn);
    HBox.setHgrow(leftColumn, Priority.ALWAYS);
    HBox.setHgrow(rightColumn, Priority.ALWAYS);

    return mainContent;
  }

  private VBox createInputConfigSection() {
    VBox section = new VBox();
    section.getStyleClass().add("config-section");
    section.setMaxWidth(Double.MAX_VALUE);

    Label sectionTitle = new Label(messages.getString("config.input.title"));
    sectionTitle.getStyleClass().add("section-title");

    // Input folder
    dirField.getStyleClass().add("required-field");
    HBox folderRow =
        createFormRow(
            messages.getString("config.input.folder"),
            messages.getString("config.input.folder.tooltip"),
            dirField,
            createChooseButton(() -> chooseDir()));

    // Group pattern
    groupPatternField.setPromptText("^.{7}");
    HBox groupRow =
        createFormRow(
            messages.getString("config.input.group.pattern"),
            messages.getString("config.input.group.pattern.tooltip"),
            groupPatternField);

    // Front pattern
    frontPatternField.setPromptText(".*front.*");
    HBox frontRow =
        createFormRow(
            messages.getString("config.input.front.pattern"),
            messages.getString("config.input.front.pattern.tooltip"),
            frontPatternField);

    // Rear pattern
    rearPatternField.setPromptText(".*rear.*");
    HBox rearRow =
        createFormRow(
            messages.getString("config.input.rear.pattern"),
            messages.getString("config.input.rear.pattern.tooltip"),
            rearPatternField);

    // Overview pattern
    overviewPatternField.setPromptText(".*scene.*");
    HBox overviewRow =
        createFormRow(
            messages.getString("config.input.overview.pattern"),
            messages.getString("config.input.overview.pattern.tooltip"),
            overviewPatternField);

    // Pattern Builder button
    Button patternBuilderBtn = new Button(messages.getString("button.pattern.builder"));
    patternBuilderBtn.getStyleClass().add("pattern-builder-button");
    patternBuilderBtn.setOnAction(e -> openPatternBuilder());

    // Update button state based on input folder validity
    updatePatternBuilderButtonState(patternBuilderBtn);

    // Listen for changes to input folder to update button state
    dirField
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              updatePatternBuilderButtonState(patternBuilderBtn);
            });

    HBox patternBuilderRow = new HBox(10);
    patternBuilderRow
        .getChildren()
        .addAll(new Label(messages.getString("label.visual.builder")), patternBuilderBtn);
    patternBuilderRow.setAlignment(Pos.CENTER_LEFT);

    section
        .getChildren()
        .addAll(
            sectionTitle, folderRow, groupRow, frontRow, rearRow, overviewRow, patternBuilderRow);
    return section;
  }

  private VBox createConnectionSection() {
    VBox section = new VBox();
    section.getStyleClass().add("config-section");
    section.setMaxWidth(Double.MAX_VALUE);

    Label sectionTitle = new Label(messages.getString("config.connection.title"));
    sectionTitle.getStyleClass().add("section-title");

    // Service URL
    HBox urlRow =
        createFormRow(
            messages.getString("config.connection.url"),
            messages.getString("config.connection.url.tooltip"),
            urlField);

    // Security options
    tlsCheck.setText(messages.getString("config.connection.secured"));
    tlsCheck.setTooltip(new Tooltip(messages.getString("config.connection.secured.tooltip")));
    tlsCheck.setSelected(true);

    insecureCheck.setText(messages.getString("config.connection.insecure"));
    insecureCheck.setTooltip(new Tooltip(messages.getString("config.connection.insecure.tooltip")));

    VBox securityBox = new VBox(8, tlsCheck, insecureCheck);

    // Parallel threads
    maxParSpin.setEditable(true);
    HBox threadsRow =
        createFormRow(
            messages.getString("config.connection.threads"),
            messages.getString("config.connection.threads.tooltip"),
            maxParSpin);

    // Export CSS
    csvBtn.setText(messages.getString("button.choose"));
    csvBtn.setOnAction(e -> chooseCsvDirectory());
    HBox exportRow =
        createFormRow(
            messages.getString("config.connection.export"),
            messages.getString("config.connection.export.tooltip"),
            csvField,
            csvBtn);

    // Control buttons
    HBox controlButtons = createControlButtons();

    section
        .getChildren()
        .addAll(sectionTitle, urlRow, securityBox, threadsRow, exportRow, controlButtons);
    return section;
  }

  private HBox createControlButtons() {
    startBtn.setText(messages.getString("button.start"));
    startBtn.getStyleClass().addAll("button", "button-primary");
    startBtn.disableProperty().bind(dirField.textProperty().isEmpty().or(processing));

    // Dynamic tooltip based on button state
    updateStartButtonTooltip();
    startBtn.setOnAction(
        e -> {
          if (onStartProcessing != null) {
            onStartProcessing.run();
          }
        });

    stopBtn.setText(messages.getString("button.stop"));
    stopBtn.setTooltip(new Tooltip(messages.getString("button.stop.tooltip")));
    stopBtn.getStyleClass().addAll("button", "button-secondary");
    stopBtn.disableProperty().bind(processing.not());
    stopBtn.setOnAction(
        e -> {
          if (onStopProcessing != null) {
            onStopProcessing.run();
          }
        });

    progressBar.setPrefWidth(200);
    progressBar.setVisible(false);

    HBox controlBox = new HBox(15);
    controlBox.setAlignment(Pos.CENTER_LEFT);
    controlBox.setPadding(new Insets(20, 0, 0, 0));
    controlBox.getChildren().addAll(startBtn, stopBtn, progressBar);

    return controlBox;
  }

  private VBox createStatusSection() {
    VBox statusSection = new VBox(15);
    statusSection.setPadding(new Insets(0, 20, 20, 20));

    // Status counters
    HBox counters = createStatusCounters();

    // Event log
    VBox logSection = createEventLogSection();

    statusSection.getChildren().addAll(counters, logSection);
    VBox.setVgrow(logSection, Priority.ALWAYS); // Make log section expand
    return statusSection;
  }

  private HBox createStatusCounters() {
    HBox counters = new HBox();
    counters.getStyleClass().add("status-counters");
    counters.setAlignment(Pos.CENTER);

    VBox groupsCounter =
        createCounterItem(groupsDiscovered, messages.getString("status.groups.discovered"));
    VBox requestsCounter =
        createCounterItem(requestsSent, messages.getString("status.requests.sent"));
    VBox okCounter = createCounterItem(responsesOk, messages.getString("status.responses.ok"));
    VBox errorCounter =
        createCounterItem(responsesError, messages.getString("status.responses.error"));

    counters.getChildren().addAll(groupsCounter, requestsCounter, okCounter, errorCounter);
    return counters;
  }

  private VBox createCounterItem(IntegerProperty property, String labelText) {
    VBox counter = new VBox();
    counter.getStyleClass().add("counter-item");
    counter.setAlignment(Pos.CENTER);

    Label valueLabel = new Label("0");
    valueLabel.getStyleClass().add("counter-value");
    valueLabel.textProperty().bind(property.asString());

    Label textLabel = new Label(labelText);
    textLabel.getStyleClass().add("counter-label");

    counter.getChildren().addAll(valueLabel, textLabel);
    return counter;
  }

  private VBox createEventLogSection() {
    VBox logSection = new VBox(10);

    Label logTitle = new Label(messages.getString("status.event.log"));
    logTitle.getStyleClass().add("section-title");

    logList.getStyleClass().add("event-log");
    logList.setPrefHeight(200);
    logList.setCellFactory(
        list ->
            new ListCell<>() {
              @Override
              protected void updateItem(ImageGroupResult item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayText());
              }
            });

    logSection.getChildren().addAll(logTitle, logList);
    VBox.setVgrow(logList, Priority.ALWAYS); // Make list expand
    return logSection;
  }

  private HBox createFormRow(String labelText, String tooltipText, Region... controls) {
    HBox row = new HBox();
    row.getStyleClass().add("form-row");
    row.setAlignment(Pos.CENTER_LEFT);

    Label label = new Label(labelText);
    label.getStyleClass().add("form-label");
    if (tooltipText != null) {
      label.setTooltip(new Tooltip(tooltipText));
    }

    row.getChildren().add(label);
    for (Region control : controls) {
      row.getChildren().add(control);
      if (control instanceof TextField) {
        HBox.setHgrow(control, Priority.ALWAYS);
      }
    }

    return row;
  }

  private Button createChooseButton(Runnable action) {
    Button button = new Button(messages.getString("button.choose"));
    button.getStyleClass().add("button");
    button.setOnAction(e -> action.run());
    return button;
  }

  private void chooseDir() {
    DirectoryChooser dc = new DirectoryChooser();
    File f = dc.showDialog(primaryStage);
    if (f != null) {
      dirField.setText(f.getAbsolutePath());
    }
  }

  private void chooseCsvDirectory() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle(messages.getString("dialog.csv.select.title"));

    // Set initial directory to current CSV directory if set
    String csvDir = csvField.getText().trim();
    if (!csvDir.isEmpty()) {
      File dir = new File(csvDir);
      if (dir.exists() && dir.isDirectory()) {
        dc.setInitialDirectory(dir);
      }
    }

    File selectedDir = dc.showDialog(primaryStage);
    if (selectedDir != null) {
      csvField.setText(selectedDir.getAbsolutePath());
      // Reset current session file path when directory changes
      currentCsvFilePath = "";
    }
  }

  /** Updates the Pattern Builder button state based on input folder validity. */
  private void updatePatternBuilderButtonState(Button patternBuilderBtn) {
    String inputFolder = dirField.getText().trim();
    boolean isValidFolder = isValidInputFolder(inputFolder);

    patternBuilderBtn.setDisable(!isValidFolder);

    if (isValidFolder) {
      patternBuilderBtn.setTooltip(
          new Tooltip(messages.getString("tooltip.pattern.builder.enabled")));
    } else {
      patternBuilderBtn.setTooltip(
          new Tooltip(messages.getString("tooltip.pattern.builder.disabled")));
    }
  }

  /** Validates if the input folder is valid and accessible. */
  private boolean isValidInputFolder(String folderPath) {
    if (folderPath == null || folderPath.trim().isEmpty()) {
      return false;
    }

    try {
      Path path = Paths.get(folderPath);
      return java.nio.file.Files.exists(path) && java.nio.file.Files.isDirectory(path);
    } catch (Exception e) {
      return false;
    }
  }

  /** Opens the Pattern Builder dialog for visual pattern configuration. */
  private void openPatternBuilder() {
    String inputFolder = dirField.getText().trim();

    // Validate input folder before opening
    if (!isValidInputFolder(inputFolder)) {
      javafx.scene.control.Alert alert =
          new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
      alert.setTitle(messages.getString("error.title"));
      alert.setHeaderText(messages.getString("error.input.folder.required"));
      alert.setContentText(messages.getString("error.pattern.builder.disabled"));
      alert.showAndWait();
      return;
    }

    PatternBuilderDialog dialog = new PatternBuilderDialog(inputFolder, messages);
    MainScreen.addIcon(dialog);

    // Set callback to update pattern fields when configuration is complete
    dialog.setOnConfigurationComplete(
        config -> {
          // Update existing pattern fields with generated patterns
          if (config.getGroupPattern() != null && !config.getGroupPattern().trim().isEmpty()) {
            groupPatternField.setText(config.getGroupPattern());
          }
          if (config.getFrontPattern() != null && !config.getFrontPattern().trim().isEmpty()) {
            frontPatternField.setText(config.getFrontPattern());
          }
          if (config.getRearPattern() != null && !config.getRearPattern().trim().isEmpty()) {
            rearPatternField.setText(config.getRearPattern());
          }
          if (config.getOverviewPattern() != null
              && !config.getOverviewPattern().trim().isEmpty()) {
            overviewPatternField.setText(config.getOverviewPattern());
          }

          // Update pattern builder config in user configuration
          updatePatternBuilderConfig(config);

          // Configuration will be automatically saved by existing auto-save mechanism
          saveConfiguration();
        });

    // Initialize with current patterns and pattern builder config
    PatternConfiguration currentConfig = getCurrentPatternConfiguration();
    dialog.showDialog(currentConfig);
  }

  /**
   * Creates a PatternConfiguration from current pattern field values and Default preset builder
   * state.
   */
  private PatternConfiguration getCurrentPatternConfiguration() {
    // Load builder state from Default preset
    PresetManager presetManager = new PresetManager();
    PresetConfiguration defaultPreset = presetManager.getDefaultPreset();

    // Create new config combining builder state from preset with UI patterns
    PatternConfiguration config = new PatternConfiguration();

    // Copy builder state from Default preset (if available)
    if (defaultPreset != null && defaultPreset.getPatternConfig() != null) {
      PatternConfiguration savedState = defaultPreset.getPatternConfig();
      config.setTokens(savedState.getTokens());
      config.setRoleRules(savedState.getRoleRules());
      config.setGroupIdToken(savedState.getGroupIdToken());
      config.setOptionalTokenTypes(savedState.getOptionalTokenTypes());
      config.setOptionalCustomTokenNames(savedState.getOptionalCustomTokenNames());
    }

    // Set current generated patterns from UI (these go to config.json, not preset)
    // Use null for empty fields instead of empty strings
    String groupPattern = groupPatternField.getText().trim();
    config.setGroupPattern(groupPattern.isEmpty() ? null : groupPattern);

    String frontPattern = frontPatternField.getText().trim();
    config.setFrontPattern(frontPattern.isEmpty() ? null : frontPattern);

    String rearPattern = rearPatternField.getText().trim();
    config.setRearPattern(rearPattern.isEmpty() ? null : rearPattern);

    String overviewPattern = overviewPatternField.getText().trim();
    config.setOverviewPattern(overviewPattern.isEmpty() ? null : overviewPattern);

    return config;
  }

  /**
   * Updates the pattern builder state and generated patterns after Pattern Builder dialog closes.
   */
  private void updatePatternBuilderConfig(PatternConfiguration config) {
    try {
      // Save builder state (tokens, rules) to Default preset
      updateDefaultPreset(config);

      // Save generated patterns to config.json
      UserConfiguration userConfig =
          (currentUserConfig != null) ? currentUserConfig : configManager.loadConfiguration();

      UserConfiguration updatedConfig =
          UserConfiguration.newBuilder()
              .setInputFolder(userConfig.getInputFolder())
              .setServiceUrl(userConfig.getServiceUrl())
              .setTlsEnabled(userConfig.isTlsEnabled())
              .setInsecureAllowed(userConfig.isInsecureAllowed())
              .setCsvDirectory(userConfig.getCsvDirectory())
              .setMaxParallel(userConfig.getMaxParallel())
              .setGroupPattern(config.getGroupPattern())
              .setFrontPattern(config.getFrontPattern())
              .setRearPattern(config.getRearPattern())
              .setOverviewPattern(config.getOverviewPattern())
              .build();

      this.currentUserConfig = updatedConfig;
      configManager.saveConfiguration(updatedConfig);
    } catch (Exception e) {
      System.err.println("Failed to update pattern builder config: " + e.getMessage());
    }
  }

  /**
   * Updates the Default preset with the current builder state. This is where we save tokens, role
   * rules, and other builder-specific settings WITHOUT the generated patterns.
   */
  private void updateDefaultPreset(PatternConfiguration config) {
    try {
      PresetManager presetManager = new PresetManager();

      // Get the Default preset
      PresetConfiguration defaultPreset = presetManager.getDefaultPreset();

      if (defaultPreset != null) {
        // Create a copy with ONLY builder state (no generated patterns)
        PatternConfiguration builderStateOnly = new PatternConfiguration();
        builderStateOnly.setTokens(config.getTokens());
        builderStateOnly.setRoleRules(config.getRoleRules());
        builderStateOnly.setGroupIdToken(config.getGroupIdToken());
        builderStateOnly.setOptionalTokenTypes(config.getOptionalTokenTypes());
        builderStateOnly.setOptionalCustomTokenNames(config.getOptionalCustomTokenNames());
        // Do NOT set generated patterns (groupPattern, frontPattern, etc.)

        // Update the Default preset's configuration with builder state only
        defaultPreset.setPatternConfig(builderStateOnly);
        defaultPreset.updateLastUsed();

        // Save the updated preset
        presetManager.savePreset(defaultPreset);
      }
    } catch (Exception e) {
      System.err.println("Failed to update Default preset: " + e.getMessage());
    }
  }

  /**
   * Validates the current pattern configuration. This integrates with existing pattern validation
   * logic.
   *
   * @return true if patterns are valid, false otherwise
   */
  public boolean validatePatterns() {
    String groupPattern = groupPatternField.getText().trim();
    String frontPattern = frontPatternField.getText().trim();
    String rearPattern = rearPatternField.getText().trim();
    String overviewPattern = overviewPatternField.getText().trim();

    // Basic validation - group pattern is required
    if (groupPattern.isEmpty()) {
      System.err.println("Group pattern is required");
      return false;
    }

    // At least one role pattern should be defined
    if (frontPattern.isEmpty() && rearPattern.isEmpty() && overviewPattern.isEmpty()) {
      System.err.println("At least one role pattern (front, rear, or overview) must be defined");
      return false;
    }

    // Validate regex syntax
    try {
      java.util.regex.Pattern.compile(groupPattern);
      if (!frontPattern.isEmpty()) {
        java.util.regex.Pattern.compile(frontPattern);
      }
      if (!rearPattern.isEmpty()) {
        java.util.regex.Pattern.compile(rearPattern);
      }
      if (!overviewPattern.isEmpty()) {
        java.util.regex.Pattern.compile(overviewPattern);
      }
    } catch (java.util.regex.PatternSyntaxException e) {
      System.err.println("Invalid regex pattern: " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Shows Save As dialog for CSV file selection. This is used when user wants to override the
   * default filename for current session.
   */
  public void showSaveAsDialog() {
    FileChooser fc = new FileChooser();
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    fc.setTitle(messages.getString("dialog.csv.saveas.title"));

    // Set initial directory to the stored CSV directory
    String csvDir = csvField.getText().trim();
    if (!csvDir.isEmpty()) {
      File dir = new File(csvDir);
      if (dir.exists() && dir.isDirectory()) {
        fc.setInitialDirectory(dir);
      }
    }

    // Set default filename
    fc.setInitialFileName(getDefaultCsvFileName());

    File f = fc.showSaveDialog(primaryStage);
    if (f != null) {
      // Store the full path for current session
      currentCsvFilePath = f.getAbsolutePath();

      // Update the directory field (only directory is persisted)
      String parentDir = f.getParent();
      if (parentDir != null) {
        csvField.setText(parentDir);
      }
    }
  }

  private void openGallery(ImageGroupResult result) {
    GalleryWindow gallery = new GalleryWindow(result, messages);
    gallery.show();
  }

  /**
   * Generates the default CSV filename using current date in Budapest time. Format:
   * results-YYYY-MM-DD.csv
   *
   * @return the default CSV filename
   */
  private String getDefaultCsvFileName() {
    LocalDate today = LocalDate.now(ZoneId.of("Europe/Budapest"));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return "results-" + today.format(formatter) + ".csv";
  }

  // Getters for configuration values
  /**
   * Gets the input folder path.
   *
   * @return the input folder path
   */
  public String getInputFolder() {
    return dirField.getText();
  }

  /**
   * Gets the service URL.
   *
   * @return the service URL
   */
  public String getServiceUrl() {
    return urlField.getText().trim();
  }

  /**
   * Checks if TLS is enabled.
   *
   * @return true if TLS is enabled
   */
  public boolean isTlsEnabled() {
    return tlsCheck.isSelected();
  }

  /**
   * Checks if insecure connections are allowed.
   *
   * @return true if insecure connections are allowed
   */
  public boolean isInsecureAllowed() {
    return insecureCheck.isSelected();
  }

  /**
   * Gets the CSV output file path. Returns the current session override if set, otherwise
   * constructs default path.
   *
   * @return the CSV output file path
   */
  public String getCsvOutput() {
    // If user has chosen a specific file for this session, use it
    if (!currentCsvFilePath.isEmpty()) {
      return currentCsvFilePath;
    }

    // Otherwise, construct default path from directory + default filename
    String csvDir = csvField.getText().trim();
    if (csvDir.isEmpty()) {
      // If no directory set, use user's home directory (writable on all platforms)
      csvDir = System.getProperty("user.home");
    }

    Path csvPath = Paths.get(csvDir, getDefaultCsvFileName());
    return csvPath.toString();
  }

  /**
   * Gets the maximum parallel processing count.
   *
   * @return the maximum parallel processing count
   */
  public int getMaxParallel() {
    return maxParSpin.getValue();
  }

  /**
   * Gets the group pattern for image grouping.
   *
   * @return the group pattern
   */
  public String getGroupPattern() {
    return groupPatternField.getText();
  }

  /**
   * Gets the front image pattern.
   *
   * @return the front image pattern
   */
  public String getFrontPattern() {
    return frontPatternField.getText();
  }

  /**
   * Gets the rear image pattern.
   *
   * @return the rear image pattern
   */
  public String getRearPattern() {
    return rearPatternField.getText();
  }

  /**
   * Gets the overview image pattern.
   *
   * @return the overview image pattern
   */
  public String getOverviewPattern() {
    return overviewPatternField.getText();
  }

  // Pattern field getters for testing
  /**
   * Gets the group pattern field for testing purposes.
   *
   * @return the group pattern text field
   */
  TextField getGroupPatternField() {
    return groupPatternField;
  }

  /**
   * Gets the front pattern field for testing purposes.
   *
   * @return the front pattern text field
   */
  TextField getFrontPatternField() {
    return frontPatternField;
  }

  /**
   * Gets the rear pattern field for testing purposes.
   *
   * @return the rear pattern text field
   */
  TextField getRearPatternField() {
    return rearPatternField;
  }

  /**
   * Gets the overview pattern field for testing purposes.
   *
   * @return the overview pattern text field
   */
  TextField getOverviewPatternField() {
    return overviewPatternField;
  }

  /**
   * Gets the current pattern configuration for testing purposes.
   *
   * @return the current pattern configuration
   */
  PatternConfiguration getPatternConfigurationForTesting() {
    return getCurrentPatternConfiguration();
  }

  // Property getters
  /**
   * Gets the processing property for binding.
   *
   * @return the processing boolean property
   */
  public BooleanProperty processingProperty() {
    return processing;
  }

  /**
   * Gets the groups discovered property for binding.
   *
   * @return the groups discovered integer property
   */
  public IntegerProperty groupsDiscoveredProperty() {
    return groupsDiscovered;
  }

  /**
   * Gets the requests sent property for binding.
   *
   * @return the requests sent integer property
   */
  public IntegerProperty requestsSentProperty() {
    return requestsSent;
  }

  /**
   * Gets the successful responses property for binding.
   *
   * @return the successful responses integer property
   */
  public IntegerProperty responsesOkProperty() {
    return responsesOk;
  }

  /**
   * Gets the error responses property for binding.
   *
   * @return the error responses integer property
   */
  public IntegerProperty responsesErrorProperty() {
    return responsesError;
  }

  /**
   * Gets the progress bar component.
   *
   * @return the progress bar
   */
  public ProgressBar getProgressBar() {
    return progressBar;
  }

  /**
   * Gets the observable list of log items.
   *
   * @return the log items list
   */
  public ObservableList<ImageGroupResult> getLogItems() {
    return logItems;
  }

  // Event handlers
  /**
   * Sets the handler for start processing events.
   *
   * @param handler the handler to execute when processing starts
   */
  public void setOnStartProcessing(Runnable handler) {
    this.onStartProcessing = handler;
  }

  /**
   * Sets the handler for stop processing events.
   *
   * @param handler the handler to execute when processing stops
   */
  public void setOnStopProcessing(Runnable handler) {
    this.onStopProcessing = handler;
  }

  /** Shows the main screen window. */
  public void show() {
    primaryStage.show();
  }

  /** Loads user configuration from the persistent storage and applies it to the UI. */
  private void loadConfiguration() {
    try {
      UserConfiguration config = configManager.loadConfiguration();
      this.currentUserConfig = config;
      applyConfiguration(config);
    } catch (Exception e) {
      System.err.println("Failed to load configuration: " + e.getMessage());
      this.currentUserConfig = UserConfiguration.newBuilder().build(); // Use default
      // Continue with default values already set in UI
    }
  }

  /** Saves the current UI configuration to persistent storage. */
  public void saveConfiguration() {
    try {
      UserConfiguration config = getCurrentConfiguration();
      this.currentUserConfig = config; // Update in-memory state
      configManager.saveConfiguration(config);
    } catch (Exception e) {
      System.err.println("Failed to save configuration: " + e.getMessage());
    }
  }

  /**
   * Applies a configuration to the UI fields.
   *
   * @param config the configuration to apply
   */
  private void applyConfiguration(UserConfiguration config) {
    dirField.setText(config.getInputFolder());
    urlField.setText(config.getServiceUrl());
    tlsCheck.setSelected(config.isTlsEnabled());
    insecureCheck.setSelected(config.isInsecureAllowed());
    csvField.setText(config.getCsvDirectory());
    maxParSpin.getValueFactory().setValue(config.getMaxParallel());

    // Only set pattern fields if they have non-empty values, otherwise leave empty
    // to show placeholder
    if (config.getGroupPattern() != null && !config.getGroupPattern().trim().isEmpty()) {
      groupPatternField.setText(config.getGroupPattern());
    } else {
      groupPatternField.clear();
    }

    if (config.getFrontPattern() != null && !config.getFrontPattern().trim().isEmpty()) {
      frontPatternField.setText(config.getFrontPattern());
    } else {
      frontPatternField.clear();
    }

    if (config.getRearPattern() != null && !config.getRearPattern().trim().isEmpty()) {
      rearPatternField.setText(config.getRearPattern());
    } else {
      rearPatternField.clear();
    }

    if (config.getOverviewPattern() != null && !config.getOverviewPattern().trim().isEmpty()) {
      overviewPatternField.setText(config.getOverviewPattern());
    } else {
      overviewPatternField.clear();
    }

    // Pattern builder config is loaded on-demand in
    // getCurrentPatternConfiguration()
    // to avoid circular dependencies during initialization

    // Reset current session CSV file path when loading config
    currentCsvFilePath = "";
  }

  /**
   * Creates a configuration object from the current UI field values.
   *
   * @return the current configuration
   */
  private UserConfiguration getCurrentConfiguration() {
    return UserConfiguration.newBuilder()
        .setInputFolder(dirField.getText())
        .setServiceUrl(urlField.getText())
        .setTlsEnabled(tlsCheck.isSelected())
        .setInsecureAllowed(insecureCheck.isSelected())
        .setCsvDirectory(csvField.getText())
        .setMaxParallel(maxParSpin.getValue())
        .setGroupPattern(groupPatternField.getText())
        .setFrontPattern(frontPatternField.getText())
        .setRearPattern(rearPatternField.getText())
        .setOverviewPattern(overviewPatternField.getText())
        .build();
  }

  /** Sets up automatic configuration saving when fields change. */
  private void setupConfigurationAutoSave() {
    // Add listeners to save configuration when fields change
    dirField
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              saveConfiguration();
              updateStartButtonTooltip();
            });
    urlField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    tlsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    insecureCheck.selectedProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    csvField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    maxParSpin.valueProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    groupPatternField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    frontPatternField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    rearPatternField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());
    overviewPatternField.textProperty().addListener((obs, oldVal, newVal) -> saveConfiguration());

    // Also listen to processing state changes
    processing.addListener((obs, oldVal, newVal) -> updateStartButtonTooltip());
  }

  /** Updates the start button tooltip based on current state. */
  private void updateStartButtonTooltip() {
    String tooltipText;
    if (processing.get()) {
      tooltipText = messages.getString("tooltip.processing.running");
    } else if (dirField.getText().trim().isEmpty()) {
      tooltipText = messages.getString("tooltip.input.folder.required");
    } else {
      tooltipText = messages.getString("button.start.tooltip");
    }
    startBtn.setTooltip(new Tooltip(tooltipText));
  }
}
