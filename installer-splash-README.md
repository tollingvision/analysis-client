# Windows Installer Splash Screen

## Current Status
This is a PLACEHOLDER file. Replace with your actual branded splash screen.

## Requirements

### Size and Format
- **Filename**: `installer-splash.png`
- **Recommended size**: 493×312 pixels (WiX toolset standard)
- **Alternative size**: 500×300 pixels (also commonly used)
- **Format**: PNG or BMP (PNG recommended for transparency and quality)
- **Color depth**: 24-bit RGB or 32-bit RGBA (with alpha channel)

### Design Guidelines
- This image appears **during Windows MSI installation only**
- It's shown in the installer wizard while files are being copied
- Should include your branding (logo, product name, tagline)
- Background should complement the Windows installer theme
- Avoid too much text (installation status text appears over/near it)
- Test on both light and dark Windows themes if possible

### What This Is NOT
This is **not** an application splash screen (shown when launching the app).
This is the **installer splash** shown during the installation process.

## How to Create

1. **Design the image** in your preferred tool (Photoshop, GIMP, Figma, etc.)
   - Canvas: 493×312 pixels
   - Include your logo and branding
   - Keep important content away from edges (leave ~20px margin)

2. **Export as PNG**:
   - File → Export → PNG
   - 493×312 pixels
   - Save as `installer-splash.png` in project root

3. **Replace this file**:
   - Delete or overwrite the existing `installer-splash.png`
   - Copy your new splash screen to project root

4. **Rebuild installer**:
   ```bash
   ./gradlew jpackage
   ```

## Example Design Layout

```
┌─────────────────────────────────────────┐
│                                         │
│         [Your Company Logo]             │
│                                         │
│     TollingVision Analysis Sample       │
│        Professional Image Analysis      │
│                                         │
│              Version 1.0.0              │
│                                         │
└─────────────────────────────────────────┘
```

## Technical Details

The splash is configured in `build.gradle.kts`:
- Windows MSI installer uses `--resource-dir` option
- jpackage looks for the file in the project root
- File is automatically included if it exists and has content
- No splash is shown if file is missing or empty

## After Replacing

Once you replace the placeholder with your actual splash screen:
1. Test the installer on Windows to verify it appears correctly
2. Check that colors/branding look good during installation
3. Ensure text is readable and properly positioned
