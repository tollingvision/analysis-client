package com.smartcloudsolutions.tollingvision.samples.util;

import com.smartcloudsolutions.tollingvision.samples.model.UserConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages user configuration persistence to ~/.tollingvision-client/config.json. Pattern builder
 * state is managed separately in pattern presets.
 */
public class ConfigurationManager {

  private static final String CONFIG_DIR = ".tollingvision-client";
  private static final String CONFIG_FILE = "config.json";

  private final Path configPath;

  /** Creates a new ConfigurationManager instance. */
  public ConfigurationManager() {
    Path homeDir = Paths.get(System.getProperty("user.home"));
    Path configDir = homeDir.resolve(CONFIG_DIR);
    this.configPath = configDir.resolve(CONFIG_FILE);
  }

  /**
   * Loads user configuration from the config file. Returns default configuration if file doesn't
   * exist or can't be read.
   *
   * @return the loaded or default configuration
   */
  public UserConfiguration loadConfiguration() {
    try {
      if (!Files.exists(configPath)) {
        return createDefaultConfiguration();
      }

      String jsonContent = Files.readString(configPath);
      return parseJsonConfiguration(jsonContent);

    } catch (Exception e) {
      System.err.println("Failed to load configuration: " + e.getMessage());
      return createDefaultConfiguration();
    }
  }

  /**
   * Saves user configuration to the config file. Creates the config directory if it doesn't exist.
   *
   * @param config the configuration to save
   * @throws IOException if the configuration cannot be saved
   */
  public void saveConfiguration(UserConfiguration config) throws IOException {
    // Ensure config directory exists
    Files.createDirectories(configPath.getParent());

    // Convert to JSON and save
    String jsonContent = toJson(config);
    Files.writeString(configPath, jsonContent);
  }

  /**
   * Creates a default configuration with sensible defaults.
   *
   * @return the default configuration
   */
  private UserConfiguration createDefaultConfiguration() {
    return UserConfiguration.newBuilder()
        .setInputFolder("")
        .setServiceUrl("localhost:50051")
        .setTlsEnabled(true)
        .setInsecureAllowed(false)
        .setCsvDirectory("")
        .setMaxParallel(4)
        .setGroupPattern("")
        .setFrontPattern("")
        .setRearPattern("")
        .setOverviewPattern("")
        .build();
  }

  /**
   * Parses JSON configuration content into a UserConfiguration object.
   *
   * @param jsonContent the JSON content to parse
   * @return the parsed configuration
   */
  protected UserConfiguration parseJsonConfiguration(String jsonContent) {
    UserConfiguration.Builder builder = UserConfiguration.newBuilder();

    // Extract values using simple string parsing
    builder.setInputFolder(extractStringValue(jsonContent, "inputFolder", ""));
    builder.setServiceUrl(extractStringValue(jsonContent, "serviceUrl", "localhost:50051"));
    builder.setTlsEnabled(extractBooleanValue(jsonContent, "tlsEnabled", true));
    builder.setInsecureAllowed(extractBooleanValue(jsonContent, "insecureAllowed", false));

    // Handle backward compatibility: try csvDirectory first, then fall back to
    // csvOutput
    String csvDir = extractStringValue(jsonContent, "csvDirectory", "");
    if (csvDir.isEmpty()) {
      // Backward compatibility: extract directory from old csvOutput field
      String oldCsvOutput = extractStringValue(jsonContent, "csvOutput", "");
      if (!oldCsvOutput.isEmpty()) {
        Path oldPath = Paths.get(oldCsvOutput);
        csvDir = oldPath.getParent() != null ? oldPath.getParent().toString() : "";
      }
    }
    builder.setCsvDirectory(csvDir);

    builder.setMaxParallel(extractIntValue(jsonContent, "maxParallel", 4));
    builder.setGroupPattern(extractStringValue(jsonContent, "groupPattern", "^.{7}"));
    builder.setFrontPattern(extractStringValue(jsonContent, "frontPattern", ".*front.*"));
    builder.setRearPattern(extractStringValue(jsonContent, "rearPattern", ".*rear.*"));
    builder.setOverviewPattern(extractStringValue(jsonContent, "overviewPattern", ".*scene.*"));

    return builder.build();
  }

  /**
   * Converts a UserConfiguration to JSON format.
   *
   * @param config the configuration to convert
   * @return the JSON representation
   */
  protected String toJson(UserConfiguration config) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"inputFolder\": \"")
        .append(escapeJson(config.getInputFolder()))
        .append("\",\n");
    json.append("  \"serviceUrl\": \"").append(escapeJson(config.getServiceUrl())).append("\",\n");
    json.append("  \"tlsEnabled\": ").append(config.isTlsEnabled()).append(",\n");
    json.append("  \"insecureAllowed\": ").append(config.isInsecureAllowed()).append(",\n");
    json.append("  \"csvDirectory\": \"")
        .append(escapeJson(config.getCsvDirectory()))
        .append("\",\n");
    json.append("  \"maxParallel\": ").append(config.getMaxParallel()).append(",\n");
    json.append("  \"groupPattern\": \"")
        .append(escapeJson(config.getGroupPattern()))
        .append("\",\n");
    json.append("  \"frontPattern\": \"")
        .append(escapeJson(config.getFrontPattern()))
        .append("\",\n");
    json.append("  \"rearPattern\": \"")
        .append(escapeJson(config.getRearPattern()))
        .append("\",\n");
    json.append("  \"overviewPattern\": \"")
        .append(escapeJson(config.getOverviewPattern()))
        .append("\"\n");
    json.append("}");
    return json.toString();
  }

  /** Extracts a string value from JSON content. */
  protected String extractStringValue(String json, String key, String defaultValue) {
    String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher m = p.matcher(json);
    return m.find() ? unescapeJson(m.group(1)) : defaultValue;
  }

  /** Extracts a boolean value from JSON content. */
  protected boolean extractBooleanValue(String json, String key, boolean defaultValue) {
    String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher m = p.matcher(json);
    return m.find() ? Boolean.parseBoolean(m.group(1)) : defaultValue;
  }

  /** Extracts an integer value from JSON content. */
  protected int extractIntValue(String json, String key, int defaultValue) {
    String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher m = p.matcher(json);
    return m.find() ? Integer.parseInt(m.group(1)) : defaultValue;
  }

  /** Escapes special characters in JSON strings. */
  protected String escapeJson(String value) {
    if (value == null) return "";

    StringBuilder sb = new StringBuilder(value.length() + 16);
    final int len = value.length();
    for (int i = 0; i < len; i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 0x20 || c == '\u2028' || c == '\u2029') {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }

  /** Unescapes special JSON characters. */
  protected String unescapeJson(String s) {
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ) {
      char c = s.charAt(i++);
      if (c == '\\') {
        if (i >= s.length()) {
          out.append('\\');
          break;
        }
        char e = s.charAt(i++);
        switch (e) {
          case '"':
            out.append('"');
            break;
          case '\\':
            out.append('\\');
            break;
          case '/':
            out.append('/');
            break;
          case 'b':
            out.append('\b');
            break;
          case 'f':
            out.append('\f');
            break;
          case 'n':
            out.append('\n');
            break;
          case 'r':
            out.append('\r');
            break;
          case 't':
            out.append('\t');
            break;
          case 'u':
            if (i + 4 <= s.length()) {
              String hex = s.substring(i, i + 4);
              out.append((char) Integer.parseInt(hex, 16));
              i += 4;
            } else {
              out.append('\uFFFD');
            }
            break;
          default:
            out.append(e);
        }
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /**
   * Gets the path to the configuration file.
   *
   * @return the configuration file path
   */
  public Path getConfigPath() {
    return configPath;
  }
}
