package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ResourceBundle;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Reusable progress indicator component for long-running operations. Provides progress feedback,
 * status messages, and cancellation support.
 */
public class ProgressIndicatorPane extends VBox {

  private final ResourceBundle messages;
  private final ProgressBar progressBar;
  private final ProgressIndicator progressIndicator;
  private final Label statusLabel;
  private final Label detailLabel;
  private final Button cancelButton;

  private Task<?> currentTask;

  /** Creates a new progress indicator pane. */
  public ProgressIndicatorPane() {
    this(false);
  }

  /**
   * Creates a new progress indicator pane.
   *
   * @param showCancelButton whether to show a cancel button
   */
  public ProgressIndicatorPane(boolean showCancelButton) {
    this(java.util.ResourceBundle.getBundle("messages"), showCancelButton);
  }

  /**
   * Creates a new progress indicator pane with i18n support.
   *
   * @param messages resource bundle for i18n
   * @param showCancelButton whether to show a cancel button
   */
  public ProgressIndicatorPane(ResourceBundle messages, boolean showCancelButton) {
    this.messages = messages;
    setSpacing(10);
    setPadding(new Insets(15));
    setAlignment(Pos.CENTER);
    getStyleClass().add("progress-pane");

    // Progress indicators
    progressBar = new ProgressBar();
    progressBar.setPrefWidth(300);
    progressBar.setVisible(false);

    progressIndicator = new ProgressIndicator();
    progressIndicator.setPrefSize(40, 40);
    progressIndicator.setVisible(false);

    // Status labels
    statusLabel = new Label(messages.getString("status.ready"));
    statusLabel.getStyleClass().add("progress-status");

    detailLabel = new Label();
    detailLabel.getStyleClass().add("progress-detail");
    detailLabel.setVisible(false);
    detailLabel.setWrapText(true);

    // Cancel button
    cancelButton = new Button(messages.getString("button.cancel"));
    cancelButton.setVisible(showCancelButton);
    cancelButton.setOnAction(e -> cancelCurrentTask());

    // Layout
    HBox progressBox = new HBox(10);
    progressBox.setAlignment(Pos.CENTER);
    progressBox.getChildren().addAll(progressIndicator, progressBar);

    HBox controlsBox = new HBox(10);
    controlsBox.setAlignment(Pos.CENTER);
    controlsBox.getChildren().add(cancelButton);

    getChildren().addAll(statusLabel, progressBox, detailLabel, controlsBox);
  }

  /**
   * Binds the progress pane to a task.
   *
   * @param task the task to bind to
   */
  public void bindToTask(Task<?> task) {
    if (currentTask != null) {
      unbindFromCurrentTask();
    }

    currentTask = task;

    // Bind progress
    progressBar.progressProperty().bind(task.progressProperty());
    progressIndicator.progressProperty().bind(task.progressProperty());

    // Bind status
    statusLabel.textProperty().bind(task.messageProperty());

    // Show progress indicators
    setProgressVisible(true);

    // Handle task completion
    task.setOnSucceeded(
        e -> {
          setProgressVisible(false);
          statusLabel.textProperty().unbind();
          statusLabel.setText(messages.getString("progress.completed"));
          statusLabel.getStyleClass().removeAll("progress-error", "progress-warning");
          statusLabel.getStyleClass().add("progress-success");
        });

    task.setOnFailed(
        e -> {
          setProgressVisible(false);
          statusLabel.textProperty().unbind();
          statusLabel.setText(
              String.format(
                  messages.getString("progress.failed"), task.getException().getMessage()));
          statusLabel.getStyleClass().removeAll("progress-success", "progress-warning");
          statusLabel.getStyleClass().add("progress-error");
        });

    task.setOnCancelled(
        e -> {
          setProgressVisible(false);
          statusLabel.textProperty().unbind();
          statusLabel.setText(messages.getString("progress.cancelled"));
          statusLabel.getStyleClass().removeAll("progress-success", "progress-error");
          statusLabel.getStyleClass().add("progress-warning");
        });
  }

  /** Unbinds from the current task. */
  private void unbindFromCurrentTask() {
    if (currentTask != null) {
      progressBar.progressProperty().unbind();
      progressIndicator.progressProperty().unbind();
      statusLabel.textProperty().unbind();
      currentTask = null;
    }
  }

  /**
   * Sets the visibility of progress indicators.
   *
   * @param visible whether progress indicators should be visible
   */
  private void setProgressVisible(boolean visible) {
    progressBar.setVisible(visible);
    progressIndicator.setVisible(visible);
    cancelButton.setVisible(visible && cancelButton.isVisible());
  }

  /** Cancels the current task if one is running. */
  private void cancelCurrentTask() {
    if (currentTask != null && !currentTask.isDone()) {
      currentTask.cancel(true);
    }
  }

  /**
   * Sets a detail message.
   *
   * @param detail the detail message, or null to hide
   */
  public void setDetail(String detail) {
    if (detail != null && !detail.trim().isEmpty()) {
      detailLabel.setText(detail);
      detailLabel.setVisible(true);
    } else {
      detailLabel.setVisible(false);
    }
  }

  /**
   * Sets the status message manually (when not bound to a task).
   *
   * @param status the status message
   */
  public void setStatus(String status) {
    if (currentTask == null) {
      statusLabel.setText(status);
    }
  }

  /**
   * Sets the progress manually (when not bound to a task).
   *
   * @param progress the progress value (0.0 to 1.0)
   */
  public void setProgress(double progress) {
    if (currentTask == null) {
      progressBar.setProgress(progress);
      progressIndicator.setProgress(progress);
    }
  }

  /** Shows indeterminate progress. */
  public void showIndeterminateProgress() {
    progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    setProgressVisible(true);
  }

  /** Hides all progress indicators. */
  public void hideProgress() {
    setProgressVisible(false);
  }

  /** Resets the progress pane to initial state. */
  public void reset() {
    unbindFromCurrentTask();
    hideProgress();
    statusLabel.setText(messages.getString("status.ready"));
    statusLabel.getStyleClass().removeAll("progress-success", "progress-error", "progress-warning");
    detailLabel.setVisible(false);
  }

  /**
   * Sets whether the cancel button is visible.
   *
   * @param visible whether the cancel button should be visible
   */
  public void setCancelButtonVisible(boolean visible) {
    cancelButton.setVisible(visible);
  }

  /**
   * Gets the current task being tracked.
   *
   * @return the current task, or null if none
   */
  public Task<?> getCurrentTask() {
    return currentTask;
  }
}
