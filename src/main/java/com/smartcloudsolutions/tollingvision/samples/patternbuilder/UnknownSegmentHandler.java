package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles unknown segments in filenames by allowing users to label them as ignored, custom tokens,
 * or free text to prevent files from falling out of matching.
 */
public class UnknownSegmentHandler {

  /** Actions that can be taken for unknown segments. */
  public enum SegmentAction {
    IGNORE("Ignore this segment"),
    CUSTOM_TOKEN("Treat as custom token"),
    FREE_TEXT("Free text (variable content)");

    private final String description;

    SegmentAction(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Represents a user's decision about how to handle an unknown segment. */
  public static class SegmentLabel {
    private final String segmentValue;
    private final SegmentAction action;
    private final String customLabel;

    public SegmentLabel(String segmentValue, SegmentAction action, String customLabel) {
      this.segmentValue = segmentValue;
      this.action = action;
      this.customLabel = customLabel;
    }

    public String getSegmentValue() {
      return segmentValue;
    }

    public SegmentAction getAction() {
      return action;
    }

    public String getCustomLabel() {
      return customLabel;
    }
  }

  // Storage for user-defined segment labels
  private final Map<String, SegmentLabel> segmentLabels = new HashMap<>();

  /**
   * Identifies unknown segments in a list of tokens.
   *
   * @param tokens the tokens to analyze
   * @return list of unknown segment values
   */
  public List<String> identifyUnknownSegments(List<FilenameToken> tokens) {
    List<String> unknownSegments = new ArrayList<>();

    for (FilenameToken token : tokens) {
      if (token.getSuggestedType() == TokenType.UNKNOWN
          && !segmentLabels.containsKey(token.getValue().toLowerCase())) {
        unknownSegments.add(token.getValue());
      }
    }

    return unknownSegments.stream().distinct().toList();
  }

  /**
   * Labels an unknown segment with a user-defined action.
   *
   * @param segmentValue the segment value to label
   * @param action the action to take for this segment
   * @param customLabel optional custom label (for CUSTOM_TOKEN action)
   */
  public void labelSegment(String segmentValue, SegmentAction action, String customLabel) {
    segmentLabels.put(
        segmentValue.toLowerCase(), new SegmentLabel(segmentValue, action, customLabel));
  }

  /**
   * Gets the label for a segment if it has been defined.
   *
   * @param segmentValue the segment value to check
   * @return the segment label, or null if not defined
   */
  public SegmentLabel getSegmentLabel(String segmentValue) {
    return segmentLabels.get(segmentValue.toLowerCase());
  }

  /**
   * Checks if a segment should be ignored based on user labeling.
   *
   * @param segmentValue the segment value to check
   * @return true if the segment should be ignored
   */
  public boolean shouldIgnoreSegment(String segmentValue) {
    SegmentLabel label = getSegmentLabel(segmentValue);
    return label != null && label.getAction() == SegmentAction.IGNORE;
  }

  /**
   * Applies user-defined segment labels to tokens.
   *
   * @param tokens the tokens to process
   * @return processed tokens with labels applied
   */
  public List<FilenameToken> applySegmentLabels(List<FilenameToken> tokens) {
    List<FilenameToken> processed = new ArrayList<>();

    for (FilenameToken token : tokens) {
      SegmentLabel label = getSegmentLabel(token.getValue());

      if (label != null) {
        switch (label.getAction()) {
          case IGNORE -> {
            // Skip ignored segments
            continue;
          }
          case CUSTOM_TOKEN -> {
            // Convert to custom token type
            processed.add(
                new FilenameToken(
                    token.getValue(),
                    token.getPosition(),
                    TokenType.SUFFIX, // Treat as suffix for now
                    0.8 // High confidence since user-defined
                    ));
          }
          case FREE_TEXT -> {
            // Keep as unknown but mark as handled
            processed.add(
                new FilenameToken(
                    token.getValue(),
                    token.getPosition(),
                    TokenType.UNKNOWN,
                    0.9 // High confidence since user-defined
                    ));
          }
        }
      } else {
        processed.add(token);
      }
    }

    // Reassign positions after filtering
    for (int i = 0; i < processed.size(); i++) {
      FilenameToken token = processed.get(i);
      processed.set(
          i,
          new FilenameToken(token.getValue(), i, token.getSuggestedType(), token.getConfidence()));
    }

    return processed;
  }

  /**
   * Gets all currently defined segment labels.
   *
   * @return map of segment values to their labels
   */
  public Map<String, SegmentLabel> getAllSegmentLabels() {
    return new HashMap<>(segmentLabels);
  }

  /** Clears all segment labels. */
  public void clearAllLabels() {
    segmentLabels.clear();
  }

  /**
   * Gets a summary of unknown segments and their handling status.
   *
   * @param allTokens all tokens from analyzed files
   * @return summary of unknown segments
   */
  public UnknownSegmentSummary getUnknownSegmentSummary(
      Map<String, List<FilenameToken>> allTokens) {
    Set<String> allUnknownSegments = new HashSet<>();
    Set<String> labeledSegments = new HashSet<>();
    Set<String> unlabeledSegments = new HashSet<>();

    for (List<FilenameToken> tokens : allTokens.values()) {
      for (FilenameToken token : tokens) {
        if (token.getSuggestedType() == TokenType.UNKNOWN) {
          String value = token.getValue().toLowerCase();
          allUnknownSegments.add(value);

          if (segmentLabels.containsKey(value)) {
            labeledSegments.add(value);
          } else {
            unlabeledSegments.add(value);
          }
        }
      }
    }

    return new UnknownSegmentSummary(allUnknownSegments, labeledSegments, unlabeledSegments);
  }

  /** Summary of unknown segments and their handling status. */
  public static class UnknownSegmentSummary {
    private final Set<String> allUnknownSegments;
    private final Set<String> labeledSegments;
    private final Set<String> unlabeledSegments;

    public UnknownSegmentSummary(
        Set<String> allUnknownSegments,
        Set<String> labeledSegments,
        Set<String> unlabeledSegments) {
      this.allUnknownSegments = Set.copyOf(allUnknownSegments);
      this.labeledSegments = Set.copyOf(labeledSegments);
      this.unlabeledSegments = Set.copyOf(unlabeledSegments);
    }

    public Set<String> getAllUnknownSegments() {
      return allUnknownSegments;
    }

    public Set<String> getLabeledSegments() {
      return labeledSegments;
    }

    public Set<String> getUnlabeledSegments() {
      return unlabeledSegments;
    }

    public boolean hasUnlabeledSegments() {
      return !unlabeledSegments.isEmpty();
    }

    public int getTotalUnknownCount() {
      return allUnknownSegments.size();
    }

    public int getLabeledCount() {
      return labeledSegments.size();
    }

    public int getUnlabeledCount() {
      return unlabeledSegments.size();
    }
  }
}
