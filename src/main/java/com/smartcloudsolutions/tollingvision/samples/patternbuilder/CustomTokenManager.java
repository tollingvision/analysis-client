package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages custom tokens that users have defined for their specific filename patterns. Custom tokens
 * are user-defined token types that don't fit into the standard categories but are important for
 * their specific use case.
 */
public class CustomTokenManager {

  /** Represents a custom token definition. */
  public static class CustomToken {
    private final String name;
    private final String description;
    private final Set<String> examples;
    private final TokenType mappedType;

    public CustomToken(
        String name, String description, Set<String> examples, TokenType mappedType) {
      this.name = name;
      this.description = description;
      this.examples = Set.copyOf(examples);
      this.mappedType = mappedType;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public Set<String> getExamples() {
      return examples;
    }

    public TokenType getMappedType() {
      return mappedType;
    }

    @Override
    public String toString() {
      return name + " (" + description + ")";
    }
  }

  // Storage for custom tokens
  private final Map<String, CustomToken> customTokens = new HashMap<>();

  // Flag to track if custom token dialog has been shown
  private boolean customTokenDialogShown = false;

  /**
   * Adds a custom token definition.
   *
   * @param customToken the custom token to add
   */
  public void addCustomToken(CustomToken customToken) {
    customTokens.put(customToken.getName().toLowerCase(), customToken);
  }

  /**
   * Removes a custom token definition.
   *
   * @param tokenName the name of the token to remove
   */
  public void removeCustomToken(String tokenName) {
    customTokens.remove(tokenName.toLowerCase());
  }

  /**
   * Gets a custom token by name.
   *
   * @param tokenName the name of the token
   * @return the custom token, or null if not found
   */
  public CustomToken getCustomToken(String tokenName) {
    return customTokens.get(tokenName.toLowerCase());
  }

  /**
   * Gets all custom tokens.
   *
   * @return collection of all custom tokens
   */
  public Collection<CustomToken> getAllCustomTokens() {
    return customTokens.values();
  }

  /**
   * Adds a token value to a custom token's examples. Creates a new CustomToken with updated
   * examples.
   *
   * @param customTokenName the name of the custom token
   * @param tokenValue the token value to add (will be converted to lowercase)
   */
  public void addTokenToCustomToken(String customTokenName, String tokenValue) {
    CustomToken existingToken = customTokens.get(customTokenName.toLowerCase());
    if (existingToken == null) {
      return;
    }

    Set<String> newExamples = new java.util.HashSet<>(existingToken.getExamples());
    newExamples.add(tokenValue.toLowerCase());

    CustomToken updatedToken =
        new CustomToken(
            existingToken.getName(),
            existingToken.getDescription(),
            newExamples,
            existingToken.getMappedType());

    customTokens.put(customTokenName.toLowerCase(), updatedToken);
  }

  /**
   * Removes a token value from all custom token examples. Creates new CustomToken instances with
   * updated examples.
   *
   * @param tokenValue the token value to remove (will be converted to lowercase)
   */
  public void removeTokenFromAllCustomTokens(String tokenValue) {
    String tokenValueLower = tokenValue.toLowerCase();

    for (Map.Entry<String, CustomToken> entry : new java.util.HashMap<>(customTokens).entrySet()) {
      CustomToken existingToken = entry.getValue();
      if (existingToken.getExamples().contains(tokenValueLower)) {
        Set<String> newExamples = new java.util.HashSet<>(existingToken.getExamples());
        newExamples.remove(tokenValueLower);

        CustomToken updatedToken =
            new CustomToken(
                existingToken.getName(),
                existingToken.getDescription(),
                newExamples,
                existingToken.getMappedType());

        customTokens.put(entry.getKey(), updatedToken);
      }
    }
  }

  /**
   * Checks if a token value matches any custom token examples.
   *
   * @param tokenValue the token value to check
   * @return the matching custom token, or null if no match
   */
  public CustomToken findMatchingCustomToken(String tokenValue) {
    String lowerValue = tokenValue.toLowerCase();
    return customTokens.values().stream()
        .filter(
            token ->
                token.getExamples().stream()
                    .anyMatch(example -> example.toLowerCase().equals(lowerValue)))
        .findFirst()
        .orElse(null);
  }

  /**
   * Pre-populates tokens in the analysis based on custom token definitions.
   *
   * @param analysis the token analysis to enhance
   * @return enhanced token analysis with custom tokens applied
   */
  public TokenAnalysis enhanceWithCustomTokens(TokenAnalysis analysis) {
    if (customTokens.isEmpty()) {
      return analysis;
    }

    Map<String, List<FilenameToken>> enhancedTokens = new HashMap<>();

    for (Map.Entry<String, List<FilenameToken>> entry :
        analysis.getTokenizedFilenames().entrySet()) {
      String filename = entry.getKey();
      List<FilenameToken> tokens = entry.getValue();

      List<FilenameToken> enhancedTokenList = new ArrayList<>();
      for (FilenameToken token : tokens) {
        CustomToken customToken = findMatchingCustomToken(token.getValue());
        if (customToken != null) {
          // Apply custom token type
          enhancedTokenList.add(
              new FilenameToken(
                  token.getValue(),
                  token.getPosition(),
                  customToken.getMappedType(),
                  0.9 // High confidence for user-defined tokens
                  ));
        } else {
          enhancedTokenList.add(token);
        }
      }

      enhancedTokens.put(filename, enhancedTokenList);
    }

    return new TokenAnalysis(
        analysis.getFilenames(),
        enhancedTokens,
        analysis.getSuggestions(),
        analysis.getConfidenceScores());
  }

  /** Creates pre-configured custom tokens for common vehicle imaging scenarios. */
  public void loadPreconfiguredCustomTokens() {
    // Common vehicle imaging custom tokens
    addCustomToken(
        new CustomToken(
            "Lane",
            "Traffic lane identifier",
            Set.of("lane1", "lane2", "lane3", "l1", "l2", "l3"),
            TokenType.SUFFIX));

    addCustomToken(
        new CustomToken(
            "Direction",
            "Traffic direction indicator",
            Set.of("nb", "sb", "eb", "wb", "north", "south", "east", "west"),
            TokenType.SUFFIX));

    addCustomToken(
        new CustomToken(
            "Station",
            "Monitoring station identifier",
            Set.of("sta1", "sta2", "station1", "station2", "st1", "st2"),
            TokenType.PREFIX));

    addCustomToken(
        new CustomToken(
            "Violation",
            "Violation type indicator",
            Set.of("speed", "redlight", "toll", "hov", "violation"),
            TokenType.SUFFIX));
  }

  /**
   * Checks if the custom token dialog has been shown.
   *
   * @return true if the dialog has been shown
   */
  public boolean hasCustomTokenDialogBeenShown() {
    return customTokenDialogShown;
  }

  /** Marks the custom token dialog as shown. */
  public void markCustomTokenDialogShown() {
    this.customTokenDialogShown = true;
  }

  /** Resets the custom token dialog shown flag. */
  public void resetCustomTokenDialogFlag() {
    this.customTokenDialogShown = false;
  }

  /** Clears all custom tokens. */
  public void clearAllCustomTokens() {
    customTokens.clear();
  }

  /**
   * Gets the count of defined custom tokens.
   *
   * @return the number of custom tokens
   */
  public int getCustomTokenCount() {
    return customTokens.size();
  }

  /** Saves custom tokens to persistent storage using simple text format. */
  public void saveCustomTokens() {
    try {
      Path configDir = getConfigDirectory();
      Files.createDirectories(configDir);

      Path customTokensFile = configDir.resolve("custom-tokens.txt");

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(customTokensFile.toFile()))) {
        for (CustomToken token : customTokens.values()) {
          // Format: name|description|mappedType|example1,example2,example3
          String examples = String.join(",", token.getExamples());
          writer.write(
              String.format(
                  "%s|%s|%s|%s%n",
                  token.getName(), token.getDescription(), token.getMappedType().name(), examples));
        }
      }

      ValidationLogger.logUserAction(
          "Custom tokens saved", "Saved " + customTokens.size() + " custom tokens");
    } catch (IOException e) {
      ValidationLogger.logException(e, "Failed to save custom tokens");
    }
  }

  /** Loads custom tokens from persistent storage using simple text format. */
  public void loadCustomTokens() {
    try {
      Path customTokensFile = getConfigDirectory().resolve("custom-tokens.txt");

      if (!Files.exists(customTokensFile)) {
        // Load preconfigured tokens if no saved tokens exist
        loadPreconfiguredCustomTokens();
        return;
      }

      customTokens.clear();

      try (BufferedReader reader = new BufferedReader(new FileReader(customTokensFile.toFile()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.trim().isEmpty()) continue;

          String[] parts = line.split("\\|", 4);
          if (parts.length >= 4) {
            String name = parts[0];
            String description = parts[1];
            TokenType mappedType = TokenType.valueOf(parts[2]);
            Set<String> examples = parts[3].isEmpty() ? Set.of() : Set.of(parts[3].split(","));

            CustomToken token = new CustomToken(name, description, examples, mappedType);
            customTokens.put(name.toLowerCase(), token);
          }
        }
      }

      ValidationLogger.logUserAction(
          "Custom tokens loaded", "Loaded " + customTokens.size() + " custom tokens");
    } catch (Exception e) {
      ValidationLogger.logException(e, "Failed to load custom tokens, using defaults");
      // Fallback to preconfigured tokens
      loadPreconfiguredCustomTokens();
    }
  }

  /** Gets the configuration directory for storing custom tokens. */
  private Path getConfigDirectory() {
    String userHome = System.getProperty("user.home");
    return Paths.get(userHome, ".tollingvision-client");
  }

  /**
   * Updates a custom token and immediately refreshes tokenization.
   *
   * @param oldName the old token name (for updates)
   * @param customToken the updated custom token
   * @param refreshCallback callback to trigger tokenization refresh
   */
  public void updateCustomToken(String oldName, CustomToken customToken, Runnable refreshCallback) {
    // Remove old token if name changed
    if (oldName != null && !oldName.equals(customToken.getName())) {
      removeCustomToken(oldName);
    }

    // Add/update token
    addCustomToken(customToken);

    // Save to persistence
    saveCustomTokens();

    // Trigger refresh
    if (refreshCallback != null) {
      refreshCallback.run();
    }

    ValidationLogger.logUserAction(
        "Custom token updated", "Updated token: " + customToken.getName());
  }
}
