# TollingVision Analysis Client

A powerful desktop application for batch processing vehicle images using [TollingVision](https://tollingvision.com)'s AI-powered license plate and vehicle recognition. Built with modern Java technologies for efficient, high-volume image analysis.

## Overview

TollingVision Analysis Client processes batches of vehicle images by automatically grouping them (front, rear, overview shots) and sending them to a TollingVision server via gRPC. The application features real-time progress tracking, interactive result galleries, and an intuitive visual pattern builder that simplifies filename configuration.

## Key Features

- **Batch Image Processing**: Automated grouping and processing of vehicle images with high-performance parallel execution
- **Real-time Monitoring**: Live progress tracking, status counters, and comprehensive event logging
- **Interactive Gallery**: AI overlay visualization with bounding boxes, zoom/pan, and thumbnail navigation
- **Visual Pattern Builder**: Intuitive dialog for configuring filename patterns without regex expertise
  - Simple mode: drag-and-drop configuration with live preview
  - Advanced mode: direct regex editing with validation
  - Smart pattern generation from sample files
- **Smart Export**: CSV export with automatic log filtering and configurable output
- **Cross-platform**: Native installers for Windows, Linux, and macOS
- **Security**: TLS 1.2+ support with configurable certificate handling
- **Modern Architecture**: Java 21 LTS with virtual threads for lightweight, scalable concurrency

## Technology Stack

### Build System
- **Build Tool**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **Java Version**: Java 21 LTS with virtual threads
- **Module System**: Fully modular Java application with JPMS

### Core Technologies
- **UI Framework**: JavaFX 23 (modular)
- **RPC Protocol**: gRPC with Netty transport
- **Security**: TLS 1.2+ support with optional insecure certificates
- **Concurrency**: Virtual threads (Java 21) with semaphore-based parallelism control

### Key Dependencies
- `io.grpc:grpc-netty` - gRPC networking
- `com.smart-cloud-solutions:tollingvision:2.6.2` - TollingVision API client
- `io.netty:netty-bom` - Network transport layer
- `com.google.protobuf:protobuf-java` - Protocol buffer serialization
- `com.google.protobuf:protobuf-java-util` - Protobuf JSON formatting utilities

### Architecture Highlights
- **Protobuf-First**: All JSON serialization uses Protobuf JsonFormat
- **Pure Java Implementation**: All overlay rendering done with JavaFX Canvas
- **Minimal Dependencies**: Lightweight deployment with only essential libraries
- **Memory Efficient**: 512 MB heap default with minimal footprint

## Quick Start

### Prerequisites
- **Java 21 LTS or later**
  - For development and running: OpenJDK 21 JRE is sufficient
  - For building native installers (jlink/jpackage): Full JDK with jmods required
- **Gradle 8.5+** (or use included wrapper)

#### Installing Full JDK for Native Installers

**Ubuntu/Debian:**
```bash
# Install full JDK with jmods (required for jlink/jpackage)
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# Verify jmods are available
ls /usr/lib/jvm/java-21-openjdk-amd64/jmods/
```

**macOS (via Homebrew):**
```bash
brew install openjdk@21
```

**Windows:**
- Download and install [Eclipse Temurin JDK 21](https://adoptium.net/) or [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
- Ensure `JAVA_HOME` points to the JDK installation

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Clean build artifacts
./gradlew clean
```

### Native Installer Packaging
Create platform-specific installers with custom JRE (requires full JDK with jmods):

```bash
# Step 1: Create optimized runtime image with jlink
./gradlew jlink
# Output: build/analysis-sample-runtime/

# Step 2: Create native installer with jpackage
./gradlew jpackage
# Output (Linux): build/jpackage/AnalysisSample_*.deb
# Output (Windows): build/jpackage/AnalysisSample-*.exe
# Output (macOS): build/jpackage/AnalysisSample-*.dmg
```

**Requirements:**
- Full JDK installation with `jmods` directory (not JRE)
- Platform-specific packaging tools:
  - **Linux**: `dpkg-deb` (usually pre-installed on Debian/Ubuntu)
  - **Windows**: [WiX Toolset](https://wixtoolset.org/) for MSI or native tools for EXE
  - **macOS**: Xcode command line tools

**Troubleshooting:**
If you get "java.base module not found" error, verify your JDK installation:
```bash
# Check Java home
java -XshowSettings:properties -version 2>&1 | grep "java.home"

# Verify jmods exist
ls $JAVA_HOME/jmods/java.base.jmod
```

## Project Structure

```
analysis-sample/
├── src/main/java/
│   ├── module-info.java                                    # Java module descriptor
│   └── com/smartcloudsolutions/tollingvision/samples/
│       ├── AnalysisSampleApp.java                         # Main JavaFX application
│       ├── model/
│       │   ├── ImageGroupResult.java                      # Result data model
│       │   ├── UserConfiguration.java                     # User settings model
│       │   └── ImageRole.java                            # Image role enumeration
│       ├── ui/
│       │   ├── MainScreen.java                           # Main application UI
│       │   └── GalleryWindow.java                        # Interactive gallery viewer
│       ├── patternbuilder/                               # ⭐ Pattern Builder System
│       │   ├── PatternBuilderDialog.java                 # Main pattern builder dialog
│       │   ├── SimplePatternBuilder.java                 # Visual drag-and-drop builder
│       │   ├── AdvancedPatternBuilder.java               # Direct regex editor
│       │   ├── PatternGenerator.java                     # Smart pattern generation
│       │   ├── PatternPreviewPane.java                   # Live preview with real files
│       │   ├── FilenameTokenizer.java                    # Intelligent filename analysis
│       │   ├── GroupingEngine.java                       # Image grouping logic
│       │   ├── PresetManager.java                        # Save/load configurations
│       │   └── ValidationModel.java                      # Pattern validation
│       └── util/
│           ├── OverlayUtils.java                         # Lightweight overlay rendering
│           └── ConfigurationManager.java                 # Settings persistence
├── src/main/resources/
│   ├── assets/logo.png                                   # Application logo
│   ├── messages.properties                               # I18n resource bundle
│   ├── tollingvision-theme.css                          # Application theme
│   └── pattern-builder-validation.css                   # Pattern builder styling
├── build.gradle.kts                                      # Gradle build configuration
└── settings.gradle.kts                                   # Project settings
```

## User Interface

### Main Application Window
- **Intuitive Two-Column Layout**: All essential controls at your fingertips
- **Pattern Builder Button**: One-click access to the visual pattern configurator
- **Real-time Status Counters**: Live progress tracking during batch processing
- **Expandable Event Log**: Detailed logging with color-coded status indicators
- **Comprehensive Tooltips**: Hover help for every control and feature
- **Modern Design**: Professional CSS theming with clean, accessible interface

### 🎨 Pattern Builder Dialog

#### Simple Mode (Visual Builder)
Perfect for users without regex experience:

1. **File Selection**: Browse and select sample files from your folder
2. **Smart Tokenization**: Application automatically breaks down filenames into parts
3. **Drag & Drop Configuration**:
   - Identify which token represents the group ID (vehicle identifier)
   - Mark optional tokens (parts that may or may not appear)
   - Define custom token types if needed
4. **Role Rules**: Simple point-and-click rules to identify:
   - Front images (e.g., contains "front" or "f")
   - Rear images (e.g., contains "rear" or "back")
   - Overview images (e.g., contains "scene" or "overview")
5. **Live Preview**: See exactly how your files will be grouped
6. **One-Click Generate**: Pattern generated automatically!

**Example**: Given files like `vehicle_001_front.jpg`, `vehicle_001_rear.jpg`:
- Select "001" as Group ID
- Set rule: "front" → Front Image
- Set rule: "rear" → Rear Image
- Click Generate → Done!

#### Advanced Mode (Regex Editor)
For power users who want full control:

- **Direct Regex Input**: Enter patterns manually with syntax highlighting
- **Live Validation**: Instant feedback on pattern correctness
- **Real-time Preview**: Test patterns against your actual files
- **Pattern Explanation**: Click "Explain" for human-readable description
- **Quick Copy**: One-click copy to clipboard
- **Flexible Extension Matching**: Toggle between specific (.jpg) or any image extension

#### Pattern Preview
Both modes include a live preview pane that shows:
- How many files match your pattern
- Which files match and which don't
- What group each file belongs to
- Unmatched files with reasons why they don't match

### Interactive Gallery Window
- **Auto-rendering**: Analysis results displayed immediately on open
- **Thumbnail Navigation**: Click any thumbnail to jump to that image
- **Zoom/Pan Controls**: Smooth zooming with proper viewport clamping
- **AI Overlay Visualization**:
  - License plate bounding boxes
  - Make/model recognition regions
  - Color-coded detection results
- **Keyboard Shortcuts**:
  - Arrow keys: Navigate between images
  - ESC: Close gallery
  - +/-: Zoom in/out
- **Detailed Results Display**:
  - ANPR data with confidence scores
  - Make/model/recognition details
  - Alternative readings
  - Per-image analysis metadata

## API Integration

### gRPC Services

This application uses **TollingVisionService.Analyze** for multi-view event analysis:

| Interface                               | Streaming     | Port(s)  | Request            | Response            | Purpose                                     |
|-----------------------------------------|--------------|----------|--------------------|---------------------|---------------------------------------------|
| TollingVisionService.Analyze            | Server-stream | 80 / 443 | EventRequest       | stream EventResponse | Multi-view event analysis (used by this app) |
| grpc.health.v1.Health/Check              | Unary         | 80 / 443 | HealthCheckRequest | HealthCheckResponse  | Service liveness probe                       |

**Note**: The TollingVision API also provides `TollingVisionService.Search` for single-image analysis, but this application uses `Analyze` for grouped multi-view processing.

All RPCs run over HTTP/2 with TLS 1.2+ support when secured mode is enabled.

## Typical Workflow

### Getting Started (First Time Users)

1. **Set Input Folder**: Click Browse and select your folder containing vehicle images

2. **Configure Patterns**: Click "Pattern Builder" to set up filename grouping rules
   - Simple mode: drag-and-drop configuration (no regex needed)
   - Advanced mode: direct regex editing
   - Use live preview to validate your patterns

3. **Configure Service**:
   - Enter your TollingVision server URL
   - Configure TLS if needed
   - Set parallel processing threads (4-16 recommended)

4. **Start Processing**: Click "Start Processing"

5. **Monitor Progress**: Watch real-time counters and event log

6. **View Results**: Double-click any result to open the interactive gallery

7. **Export Data**: Click "Save As..." to export results to CSV

### Advanced Users

For direct regex control:
- Open Pattern Builder → Switch to Advanced Mode
- Enter patterns manually with live validation
- Test against your actual files
- Save as presets for reuse

## Features in Detail

### Intelligent Image Processing

#### Smart Filename Analysis
The application's **FilenameTokenizer** intelligently breaks down your filenames:
- Detects common patterns: dates, IDs, sequences, camera positions
- Identifies token types automatically (numeric, alphabetic, mixed)
- Calculates confidence scores for each detection
- Groups similar tokens across multiple files

#### Dynamic Pattern Generation
The **PatternGenerator** creates optimal regex patterns by:
- Analyzing all sample values for each token type
- Detecting character types (digits-only, letters-only, alphanumeric)
- Measuring min/max lengths to generate precise quantifiers
- Including special characters only when actually present in your data
- Avoiding over-generic patterns that might cause false matches

**Example**: For group IDs like `"5789"`, `"11707"`, `"15864"`:
- Detects: All numeric, length 4-5
- Generates: `\d{4,5}` (not the generic `\w+` that could match too much)

#### Image Grouping Engine
- Groups files by extracting group IDs using generated patterns
- Assigns roles (front/rear/overview) within each group using simple rules
- Handles edge cases: optional tokens, missing files, mixed formats
- Reports unmatched files with clear explanations

#### Batch Processing
- **Recursive Folder Scanning**: Finds all files matching the configured patterns
- **Parallel Processing**: Configurable concurrency (1-64 parallel groups via semaphore)
- **Format Support**: Any image format supported by the TollingVision API (typically JPEG, PNG)
- **Smart Batching**: Groups sent as complete events to the API

### Real-time Monitoring & User Experience
- **Live Status Counters**:
  - Groups discovered
  - Requests sent
  - Successful responses
  - Errors (if any)
- **Comprehensive Event Log**:
  - Timestamped entries
  - Color-coded status indicators
  - Expandable for detailed view
- **Progress Tracking**:
  - Real-time progress bar
  - Visual progress percentage
  - Current operation status via event log
- **Rich Tooltips**: Every button, field, and control has helpful hover text
- **Keyboard Shortcuts**: Gallery navigation (arrows, ESC), Enter to open results
- **Error Handling**: Graceful error recovery with clear messages

### Gallery Features
- **Auto-rendering**: Immediate display of analysis results on gallery open
- **Enhanced Navigation**: Clickable thumbnail strip with stable sizing
- **Zoom/Pan Controls**: Enhanced image viewer with proper viewport clamping
- **Overlay Visualization**: Individual bounding boxes using Quadrilateral data
- **Keyboard Shortcuts**: Arrow keys for navigation, ESC to close, Enter to open
- **Data Binding**: Per-image results with direct path-based mapping
- **MMR Formatting**: Structured display of Make/Model/Recognition data

### Configuration & Persistence

#### Pattern Presets
- **Save/Load Presets**: Save your pattern configurations for reuse
- **Export/Import**: Share presets with team members
- **Default Preset**: Automatically loads last used configuration
- **Preset Management**: Create, rename, delete presets via UI

#### User Settings
All preferences automatically saved to `~/.tollingvision-client/`:
- `config.json`: Main application settings
- `pattern-presets.json`: Saved pattern configurations
- `custom-tokens.json`: User-defined token types

Settings include:
- Input folder paths
- Service connection parameters (URL, TLS settings)
- Processing patterns (group, front, rear, overview)
- Thread pool configuration
- CSV output preferences
- Pattern Builder state (last used mode, presets)

### Data Export
- **Smart CSV Export**:
  - Comprehensive results with all analysis data
  - Intelligent log filtering (excludes log entries automatically)
  - Configurable output location
  - Timestamped filenames
- **Analysis Data**:
  - License plate readings with confidence scores
  - Make/model/recognition details
  - Alternative readings
  - Image paths and classifications
- **Custom Export**: Export individual gallery views as JSON

### Security & Networking
- **TLS Support**: TLS 1.2+ with configurable certificate handling
- **gRPC Integration**: Efficient binary protocol with streaming support
- **Connection Management**: Automatic retry logic and graceful error handling
- **Resource Management**: Configurable thread pools and memory limits

## Technical Implementation

### Protocol Buffers
The application uses Protocol Buffers for all data serialization.

**EventRequest Structure**:
```proto
message EventRequest {
  bool sign_recognition = 1;              // Enable ADR sign recognition
  bool international_recognition = 2;     // Enable international ANPR
  optional bool resampling = 3;           // Enable full HD resampling
  bool results_without_plate_type = 4;    // Return results without plate type
  string location = 5;                    // Location for improved accuracy
  repeated Image front_image = 6;         // Front view images ✓ USED
  repeated Image rear_image = 7;          // Rear view images ✓ USED
  repeated Image overview_image = 8;      // Overview images ✓ USED
  uint32 max_search = 9;                  // Max vehicles per image (1-5)
  uint32 max_rotation = 10;               // Max plate rotation (deg)
  sint32 max_character_size = 11;         // Max plate char height (px)
  repeated Region region = 12;            // Regions of interest
}

message Image {
  bytes data = 1;   // Image binary data
  string name = 2;  // Filename
}
```

**Usage**: This application only populates the **image fields** (`front_image`, `rear_image`, `overview_image`) with raw image bytes and filenames. All other configuration fields use server defaults. Images are grouped by vehicle/event ID and sent together in a single `EventRequest`. The server performs ANPR on front/rear images and ANPR+MMR on overview images, returning results via server-streaming `EventResponse`.

### Modular Architecture
- **Module Name**: `analysis.sample`
- **Main Class**: `com.smartcloudsolutions.tollingvision.samples.AnalysisSampleApp`
- **Clean Separation**: UI, data models, and utilities in separate packages
- **JPMS Integration**: Fully modular with proper module descriptor

## Development Commands

### Build Operations
```bash
# Compile only
./gradlew compileJava

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

### Platform-Specific Packaging
- **Windows**: Generates `.exe` installer
- **macOS**: Generates `.dmg` installer
- **Linux**: Generates `.deb` installer (default)

## Performance & Requirements

| Category      | Specification |
|---------------|---------------|
| **Performance**   | Low UI latency; throughput depends on server and network |
| **Scalability**   | Handles large image sets; lightweight concurrency with virtual threads |
| **Memory**        | 512 MB heap default; virtual threads reduce overhead |
| **Portability**   | Native installers (~50MB with bundled JRE runtime) for Windows, macOS, and Linux |
| **Security**      | TLS 1.2+ support, no plaintext credentials |
| **Usability**     | Gallery keyboard shortcuts, comprehensive tooltips, accessible UI |

## Who Should Use This?

### Perfect For:
- **Traffic Enforcement Agencies**: Process large batches of violation images
- **Tolling Operators**: Analyze vehicle images from toll plazas
- **Parking Management**: Automated license plate recognition for lots/garages
- **Security Operations**: Vehicle monitoring and access control
- **Fleet Management**: Vehicle identification and tracking
- **Research Teams**: Analyzing vehicle image datasets

### Key Advantages:
- **No Programming Required**: The Pattern Builder means anyone can configure it
- **Handles Complex Naming**: Works with any filename convention
- **Batch Processing**: Process thousands of images efficiently
- **Visual Feedback**: See results immediately in the interactive gallery
- **Production Ready**: TLS security, error handling, and robust architecture
- **Cross-Platform**: Works on Windows, Linux, and macOS



## Configuration

User configuration is automatically saved to `~/.tollingvision-client/config.json` and includes:
- Input folder paths and processing patterns
- Service connection parameters
- Thread pool configuration
- UI preferences and themes

## Dependencies

The application maintains a minimal dependency footprint:
- **Core**: gRPC, Netty, Protocol Buffers
- **UI**: JavaFX 23 (modular)
- **BOM Management**: Uses Bill of Materials for consistent versioning

## License

This project is part of the TollingVision ecosystem. Please refer to your TollingVision license agreement for usage terms and conditions.

## Support

For technical support and documentation, please refer to the TollingVision API documentation.
