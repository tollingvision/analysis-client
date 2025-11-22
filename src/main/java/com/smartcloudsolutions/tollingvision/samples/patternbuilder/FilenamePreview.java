package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a filename preview entry showing how a filename would be processed by the current
 * pattern configuration. Used in the PatternPreviewPane to display real-time feedback on pattern
 * matching results.
 */
public class FilenamePreview {
  private final StringProperty filename;
  private final StringProperty groupId;
  private final ObjectProperty<ImageRole> role;
  private final BooleanProperty matched;
  private final StringProperty errorMessage;

  /**
   * Creates a new filename preview with the specified values.
   *
   * @param filename the original filename
   * @param groupId the extracted group ID, or null if extraction failed
   * @param role the classified image role, or null if classification failed
   * @param matched true if the filename was successfully processed
   * @param errorMessage error message if processing failed, or null if successful
   */
  public FilenamePreview(
      String filename, String groupId, ImageRole role, boolean matched, String errorMessage) {
    this.filename = new SimpleStringProperty(filename);
    this.groupId = new SimpleStringProperty(groupId);
    this.role = new SimpleObjectProperty<>(role);
    this.matched = new SimpleBooleanProperty(matched);
    this.errorMessage = new SimpleStringProperty(errorMessage);
  }

  /**
   * Creates a new filename preview with default values.
   *
   * @param filename the original filename
   */
  public FilenamePreview(String filename) {
    this(filename, null, null, false, null);
  }

  // Filename property
  public String getFilename() {
    return filename.get();
  }

  public void setFilename(String filename) {
    this.filename.set(filename);
  }

  public StringProperty filenameProperty() {
    return filename;
  }

  // Group ID property
  public String getGroupId() {
    return groupId.get();
  }

  public void setGroupId(String groupId) {
    this.groupId.set(groupId);
  }

  public StringProperty groupIdProperty() {
    return groupId;
  }

  // Role property
  public ImageRole getRole() {
    return role.get();
  }

  public void setRole(ImageRole role) {
    this.role.set(role);
  }

  public ObjectProperty<ImageRole> roleProperty() {
    return role;
  }

  // Matched property
  public boolean isMatched() {
    return matched.get();
  }

  public void setMatched(boolean matched) {
    this.matched.set(matched);
  }

  public BooleanProperty matchedProperty() {
    return matched;
  }

  // Error message property
  public String getErrorMessage() {
    return errorMessage.get();
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage.set(errorMessage);
  }

  public StringProperty errorMessageProperty() {
    return errorMessage;
  }

  /**
   * Updates this preview with new processing results.
   *
   * @param groupId the extracted group ID
   * @param role the classified image role
   * @param matched true if processing was successful
   * @param errorMessage error message if processing failed
   */
  public void updateResults(String groupId, ImageRole role, boolean matched, String errorMessage) {
    setGroupId(groupId);
    setRole(role);
    setMatched(matched);
    setErrorMessage(errorMessage);
  }

  /**
   * Marks this preview as having an error.
   *
   * @param errorMessage the error message to display
   */
  public void setError(String errorMessage) {
    setGroupId(null);
    setRole(null);
    setMatched(false);
    setErrorMessage(errorMessage);
  }

  /**
   * Marks this preview as successfully processed.
   *
   * @param groupId the extracted group ID
   * @param role the classified image role
   */
  public void setSuccess(String groupId, ImageRole role) {
    setGroupId(groupId);
    setRole(role);
    setMatched(true);
    setErrorMessage(null);
  }

  /**
   * @return true if this preview has an error message
   */
  public boolean hasError() {
    return errorMessage.get() != null && !errorMessage.get().trim().isEmpty();
  }

  /**
   * @return true if this preview represents a successfully processed filename
   */
  public boolean isSuccessful() {
    return matched.get() && !hasError();
  }

  @Override
  public String toString() {
    return String.format(
        "FilenamePreview{filename='%s', groupId='%s', role=%s, matched=%s, error='%s'}",
        getFilename(), getGroupId(), getRole(), isMatched(), getErrorMessage());
  }
}
