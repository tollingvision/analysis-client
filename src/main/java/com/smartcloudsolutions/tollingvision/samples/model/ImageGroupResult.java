package com.smartcloudsolutions.tollingvision.samples.model;

import com.smartcloudsolutions.tollingvision.EventResult;
import com.smartcloudsolutions.tollingvision.SearchResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents the analysis results for a group of images. Contains both the aggregated EventResult
 * and individual SearchResponse data per image.
 */
public class ImageGroupResult {
  private final String bucket;
  private final String frontNames;
  private final String rearNames;
  private final String overNames;
  private final String frontPlate;
  private final String frontAlt;
  private final String rearPlate;
  private final String rearAlt;
  private final String frontJurisdiction;
  private final String frontJurisdictionAlt;
  private final String rearJurisdiction;
  private final String rearJurisdictionAlt;
  private final String mmr;
  private final String mmrAlt;
  private final List<Path> allImagePaths;
  private final EventResult eventResult;
  private final Map<Path, SearchResponse> imageAnalysisData;

  /**
   * Constructor for real analysis result.
   *
   * @param bucket bucket identifier
   * @param eventResult the analysis result
   * @param imagePaths list of image paths
   * @param frontPattern pattern for front images
   * @param rearPattern pattern for rear images
   * @param overviewPattern pattern for overview images
   */
  public ImageGroupResult(
      String bucket,
      EventResult eventResult,
      List<Path> imagePaths,
      String frontPattern,
      String rearPattern,
      String overviewPattern) {
    this.bucket = bucket;
    this.eventResult = eventResult;
    this.allImagePaths = imagePaths;

    Pattern fp = Pattern.compile(frontPattern);
    Pattern rp = Pattern.compile(rearPattern);
    Pattern op = Pattern.compile(overviewPattern);

    this.frontNames = filterNames(imagePaths, fp);
    this.rearNames = filterNames(imagePaths, rp);
    this.overNames = filterNames(imagePaths, op);

    // Format plate and MMR data
    this.frontPlate = eventResult.hasFrontPlate() ? eventResult.getFrontPlate().getText() : "";
    this.frontAlt =
        eventResult.getFrontPlateAlternativeList().stream()
            .map(plate -> plate.getText())
            .collect(Collectors.joining("|"));
    this.rearPlate = eventResult.hasRearPlate() ? eventResult.getRearPlate().getText() : "";
    this.rearAlt =
        eventResult.getRearPlateAlternativeList().stream()
            .map(plate -> plate.getText())
            .collect(Collectors.joining("|"));

    // Format jurisdiction data
    this.frontJurisdiction =
        eventResult.hasFrontPlate() ? eventResult.getFrontPlate().getState() : "";
    this.frontJurisdictionAlt =
        eventResult.getFrontPlateAlternativeList().stream()
            .map(plate -> plate.getState())
            .collect(Collectors.joining("|"));
    this.rearJurisdiction = eventResult.hasRearPlate() ? eventResult.getRearPlate().getState() : "";
    this.rearJurisdictionAlt =
        eventResult.getRearPlateAlternativeList().stream()
            .map(plate -> plate.getState())
            .collect(Collectors.joining("|"));

    this.mmr = eventResult.hasMmr() ? formatMmr(eventResult.getMmr()) : "";
    this.mmrAlt =
        eventResult.getMmrAlternativeList().stream()
            .map(ImageGroupResult::formatMmr)
            .collect(Collectors.joining("|"));

    // Initialize map for individual image analysis data
    this.imageAnalysisData = new HashMap<>();
  }

  /**
   * Constructor for log entries.
   *
   * @param bucket bucket identifier
   * @param message log message
   */
  public ImageGroupResult(String bucket, String message) {
    this.bucket = bucket + " - " + message;
    this.frontNames = "";
    this.rearNames = "";
    this.overNames = "";
    this.frontPlate = "";
    this.frontAlt = "";
    this.rearPlate = "";
    this.rearAlt = "";
    this.frontJurisdiction = "";
    this.frontJurisdictionAlt = "";
    this.rearJurisdiction = "";
    this.rearJurisdictionAlt = "";
    this.mmr = "";
    this.mmrAlt = "";
    this.allImagePaths = Collections.emptyList();
    this.eventResult = null;
    this.imageAnalysisData = new HashMap<>();
  }

  // Getters
  /**
   * Gets the bucket identifier.
   *
   * @return the bucket identifier
   */
  public String getBucket() {
    return bucket;
  }

  /**
   * Gets the front image names.
   *
   * @return the front image names
   */
  public String getFrontNames() {
    return frontNames;
  }

  /**
   * Gets the rear image names.
   *
   * @return the rear image names
   */
  public String getRearNames() {
    return rearNames;
  }

  /**
   * Gets the overview image names.
   *
   * @return the overview image names
   */
  public String getOverNames() {
    return overNames;
  }

  /**
   * Gets the front plate information.
   *
   * @return the front plate information
   */
  public String getFrontPlate() {
    return frontPlate;
  }

  /**
   * Gets the front plate alternatives.
   *
   * @return the front plate alternatives
   */
  public String getFrontAlt() {
    return frontAlt;
  }

  /**
   * Gets the rear plate information.
   *
   * @return the rear plate information
   */
  public String getRearPlate() {
    return rearPlate;
  }

  /**
   * Gets the rear plate alternatives.
   *
   * @return the rear plate alternatives
   */
  public String getRearAlt() {
    return rearAlt;
  }

  /**
   * Gets the MMR (Make, Model, Recognition) information.
   *
   * @return the MMR information
   */
  public String getMmr() {
    return mmr;
  }

  /**
   * Gets the MMR alternatives.
   *
   * @return the MMR alternatives
   */
  public String getMmrAlt() {
    return mmrAlt;
  }

  /**
   * Gets the front jurisdiction information.
   *
   * @return the front jurisdiction information
   */
  public String getFrontJurisdiction() {
    return frontJurisdiction;
  }

  /**
   * Gets the front jurisdiction alternatives.
   *
   * @return the front jurisdiction alternatives
   */
  public String getFrontJurisdictionAlt() {
    return frontJurisdictionAlt;
  }

  /**
   * Gets the rear jurisdiction information.
   *
   * @return the rear jurisdiction information
   */
  public String getRearJurisdiction() {
    return rearJurisdiction;
  }

  /**
   * Gets the rear jurisdiction alternatives.
   *
   * @return the rear jurisdiction alternatives
   */
  public String getRearJurisdictionAlt() {
    return rearJurisdictionAlt;
  }

  /**
   * Gets all image paths.
   *
   * @return the list of all image paths
   */
  public List<Path> getAllImagePaths() {
    return allImagePaths;
  }

  /**
   * Gets the event result.
   *
   * @return the event result
   */
  public EventResult getEventResult() {
    return eventResult;
  }

  /**
   * Gets the image analysis data map.
   *
   * @return the map of image paths to SearchResponse data
   */
  public Map<Path, SearchResponse> getImageAnalysisData() {
    return imageAnalysisData;
  }

  /**
   * Adds individual image analysis data.
   *
   * @param imagePath the image path
   * @param searchResponse the SearchResponse data for this image
   */
  public void addImageAnalysis(Path imagePath, SearchResponse searchResponse) {
    imageAnalysisData.put(imagePath, searchResponse);
  }

  /**
   * Gets the formatted display text for the event log. Format: <group> | front plate: <front plate>
   * (<front jurisdiction>) | rear plate: <rear plate> (<rear jurisdiction>) | mmr: <make> <model>
   * (<variation> <generation>) <colorName> <category>
   *
   * @return the formatted display text
   */
  public String getDisplayText() {
    // For log entries, return the bucket as-is
    if (bucket.startsWith("[LOG]")) {
      return bucket;
    }

    StringBuilder display = new StringBuilder();
    display.append(bucket);

    // Front plate section
    display.append(" | front plate: ");
    if (!frontPlate.isEmpty()) {
      display.append(frontPlate);
      if (!frontJurisdiction.isEmpty()) {
        display.append(" (").append(frontJurisdiction).append(")");
      }
    } else {
      display.append("-");
    }

    // Rear plate section
    display.append(" | rear plate: ");
    if (!rearPlate.isEmpty()) {
      display.append(rearPlate);
      if (!rearJurisdiction.isEmpty()) {
        display.append(" (").append(rearJurisdiction).append(")");
      }
    } else {
      display.append("-");
    }

    // MMR section
    display.append(" | mmr: ");
    if (eventResult != null && eventResult.hasMmr()) {
      com.smartcloudsolutions.tollingvision.Mmr mmrData = eventResult.getMmr();
      display.append(formatMmrForDisplay(mmrData));
    } else {
      display.append("-");
    }

    return display.toString();
  }

  // Helper methods
  private static String filterNames(List<Path> list, Pattern pattern) {
    return list.stream()
        .filter(f -> pattern.matcher(f.getFileName().toString()).find())
        .map(Path::getFileName)
        .map(Path::toString)
        .collect(Collectors.joining("|"));
  }

  private static String formatMmr(com.smartcloudsolutions.tollingvision.Mmr mmr) {
    return String.format(
        "%s %s (generation: %s|category: %s|body type: %s|view point: %s|color: %s|standard color:"
            + " %s|dimensions: %s)",
        mmr.getMake(),
        mmr.getModel(),
        mmr.getGeneration(),
        mmr.getCategory(),
        mmr.getBodyType(),
        mmr.getViewPoint(),
        mmr.getColorName(),
        mmr.getStandardColorName(),
        mmr.getDimensions() != null
            ? mmr.getDimensions().getWidth()
                + "x"
                + mmr.getDimensions().getHeight()
                + "x"
                + mmr.getDimensions().getLength()
            : "N/A");
  }

  /**
   * Formats MMR data for display in the event log. Format: <make> <model> (<generation>)
   * <colorName> <category> Omits empty tokens and surrounding spaces/parentheses.
   */
  private static String formatMmrForDisplay(com.smartcloudsolutions.tollingvision.Mmr mmr) {
    StringBuilder mmrDisplay = new StringBuilder();

    // Make and Model (required)
    if (!mmr.getMake().isEmpty()) {
      mmrDisplay.append(mmr.getMake());
    }
    if (!mmr.getModel().isEmpty()) {
      if (mmrDisplay.length() > 0) mmrDisplay.append(" ");
      mmrDisplay.append(mmr.getModel());
    }

    // Generation (optional, in parentheses)
    if (!mmr.getGeneration().isEmpty()) {
      if (mmrDisplay.length() > 0) mmrDisplay.append(" ");
      mmrDisplay.append("(").append(mmr.getGeneration()).append(")");
    }

    // Color Name
    if (!mmr.getColorName().isEmpty()) {
      if (mmrDisplay.length() > 0) mmrDisplay.append(" ");
      mmrDisplay.append(mmr.getColorName());
    }

    // Category
    if (!mmr.getCategory().isEmpty()) {
      if (mmrDisplay.length() > 0) mmrDisplay.append(" ");
      mmrDisplay.append(mmr.getCategory());
    }

    return mmrDisplay.toString();
  }
}
