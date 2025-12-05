# Application Icon Placeholder

## Current Status
This is a PLACEHOLDER file. Replace with your actual application icon.

## What to provide:

### Windows Icon (app-icon.ico)
- Multi-resolution ICO file containing:
  - 16×16, 32×32, 48×48, 256×256 pixels
- Use online converter or tool like GIMP to create
- Recommendation: Start with a 1024×1024 PNG, then convert to ICO

### macOS Icon (app-icon.icns)
- Multi-resolution ICNS file containing:
  - 16×16@1x/2x, 32×32@1x/2x, 128×128@1x/2x, 256×256@1x/2x, 512×512@1x/2x
- Use macOS 'iconutil' command or online converter
- Recommendation: Start with a 1024×1024 PNG, then convert to ICNS

### Linux Icon (app-icon.png)
- Single PNG file: 256×256 pixels (or 512×512 for HiDPI)
- PNG format with transparency support

## How to create from a single source image:

1. Create a high-resolution source (1024×1024 PNG recommended)
2. For Windows: Use https://convertio.co/png-ico/ or similar
3. For macOS: Use https://cloudconvert.com/png-to-icns or iconutil
4. For Linux: Resize to 256×256 PNG

## After replacing:
Run `./gradlew jpackage` to rebuild installers with new icons.
