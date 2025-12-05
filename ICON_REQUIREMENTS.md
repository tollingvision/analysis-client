# Icon and Splash Screen Requirements

## Application Icon Requirements

The application icon appears when the installed application is launched (desktop shortcut, Start menu, Dock).

### Windows (.ico format)
- **Filename**: `app-icon.ico`
- **Required sizes** (all in one .ico file):
  - 16×16 pixels
  - 32×32 pixels
  - 48×48 pixels
  - 256×256 pixels
- **Format**: ICO with multiple resolutions
- **Tool recommendation**: Use an online ICO converter or tool like GIMP, Photoshop

### macOS (.icns format)
- **Filename**: `app-icon.icns`
- **Required sizes** (all in one .icns file):
  - 16×16 pixels (@1x and @2x = 32×32)
  - 32×32 pixels (@1x and @2x = 64×64)
  - 128×128 pixels (@1x and @2x = 256×256)
  - 256×256 pixels (@1x and @2x = 512×512)
  - 512×512 pixels (@1x and @2x = 1024×1024)
- **Format**: ICNS (Apple Icon Image)
- **Tool recommendation**: Use `iconutil` (macOS command-line) or online ICNS converter

### Linux (.png format)
- **Filename**: `app-icon.png`
- **Size**: 256×256 pixels (or 512×512 for HiDPI)
- **Format**: PNG with transparency
- **Location**: Will be installed to system icon directories

## Installer Splash Screen (Windows MSI only)

### Windows Installer Splash
- **Filename**: `installer-splash.png`
- **Size**: 493×312 pixels (WiX toolset standard)
- **Format**: PNG or BMP
- **Usage**: Displayed during Windows MSI installation process
- **Note**: This is the installer splash, not the application splash screen

**Current file**: `installer-splash.png` already exists but may be placeholder

## How to Replace Placeholders

1. **Create your icons** using your design tool (Illustrator, Photoshop, etc.)
2. **Convert to required formats**:
   - Windows: Convert to .ico with multiple sizes
   - macOS: Convert to .icns with multiple sizes
   - Linux: Export as 256×256 PNG
3. **Replace the placeholder files** in the project root:
   - `app-icon.ico`
   - `app-icon.icns`
   - `app-icon.png`
   - `installer-splash.png`
4. **Rebuild installers**: Run `./gradlew jpackage`

## Design Tips

- Use **simple, recognizable designs** that work at small sizes (16×16)
- Ensure **good contrast** for visibility on light and dark backgrounds
- Test icons at all sizes to ensure clarity
- Use **transparency** for non-rectangular shapes (PNG/ICO/ICNS support this)
- **Installer splash** can be more detailed since it's larger (493×312)
