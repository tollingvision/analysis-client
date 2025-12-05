import org.gradle.internal.os.OperatingSystem

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink")          version "3.1.3"
    id("com.diffplug.spotless") version "8.0.0"
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.76.0"))

    implementation(platform("io.netty:netty-bom:4.2.7.Final"))
    implementation(platform("com.google.cloud:libraries-bom:26.70.0"))

    implementation("io.grpc:grpc-netty")
    implementation("com.smart-cloud-solutions:tollingvision:2.6.2")
    implementation("com.google.code.gson:gson:2.12.1")

    runtimeOnly("com.google.protobuf:protobuf-java:4.33.0")
    implementation("com.google.protobuf:protobuf-java-util:4.33.0")
    runtimeOnly("com.google.errorprone:error_prone_annotations:2.43.0")
    
    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    modularity.inferModulePath.set(true)
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.smartcloudsolutions.tollingvision.samples.AnalysisSampleApp")
    mainModule.set("analysis.sample")
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.swing")
}

jlink {
    forceMerge(".*")
    mergedModule { 
        additive = true
        uses("io.grpc.NameResolverProvider")
        uses("io.grpc.LoadBalancerProvider")
    }

    imageName.set("TollingVision Analysis Sample")
    launcher {
        name = "TollingVision Analysis Sample"
        jvmArgs = listOf(
            "-Djpackage.app-version=1.0.0"
        )
    }
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

    jpackage {
        imageName = "TollingVision Analysis Sample"
        installerName = "TollingVisionAnalysisSample"
        appVersion = "1.0.0"
        skipInstaller = false
        
        // Vendor/Publisher information (shown in Windows "Programs and Features")
        installerOptions.addAll(listOf(
            "--vendor", "Smart Cloud Solutions Inc.",
            "--copyright", "Copyright © 2025 Smart Cloud Solutions Inc. All rights reserved.",
            "--description", "Professional AI-powered vehicle image analysis application for batch processing using TollingVision service"
        ))
        
        // Ensure Java options are passed correctly
        jvmArgs = listOf(
            "-Djpackage.app-version=1.0.0"
        )

        // platform-specific installer format
        val os = OperatingSystem.current()
        installerType = when {
            os.isMacOsX   -> "dmg"
            os.isWindows  -> "msi"
            else          -> "deb"   // Linux
        }
        
        // Application icon (platform-specific)
        // The plugin automatically uses the correct icon based on the platform
        val iconFile = when {
            os.isMacOsX   -> file("app-icon.icns")
            os.isWindows  -> file("app-icon.ico")
            else          -> file("app-icon.png")  // Linux
        }
        if (iconFile.exists() && iconFile.length() > 0) {
            icon = iconFile.absolutePath
            logger.lifecycle("Using application icon: ${iconFile.absolutePath}")
        } else {
            logger.warn("Application icon not found: ${iconFile.name}")
        }
        
        // Windows-specific: Add resource-dir for WixUI splash screen
        // Must use installerOptions, not resourceDir property (not supported by plugin)
        if (os.isWindows) {
            val wixDialogBmp = file("WixUI_Bmp_Dialog.png")
            if (wixDialogBmp.exists() && wixDialogBmp.length() > 0) {
                installerOptions.addAll(listOf("--resource-dir", projectDir.absolutePath))
                logger.lifecycle("Windows installer splash screen: WixUI_Bmp_Dialog.png in ${projectDir.absolutePath}")
            } else {
                logger.lifecycle("No custom splash screen (WixUI_Bmp_Dialog.png not found)")
            }
        }
        
        // Common options for all platforms
        val licenseFile = file("LICENSE.txt")
        if (licenseFile.exists()) {
            installerOptions.add("--license-file")
            installerOptions.add(licenseFile.absolutePath)
        }
        
        // Windows-specific configuration
        if (os.isWindows) {
            val winOptions = mutableListOf(
                "--win-dir-chooser",
                "--win-menu",
                "--win-shortcut",
                "--win-menu-group", "TollingVision",
                // Install to C:\Program Files\TollingVision\AnalysisSample
                "--install-dir", "TollingVision\\AnalysisSample"
            )
            
            // Code signing (if certificate is available)
            // To enable: set environment variables or system properties:
            //   - WIN_SIGN_CERT_FILE: path to .pfx/.p12 certificate file
            //   - WIN_SIGN_CERT_PASSWORD: certificate password
            val certFile = System.getenv("WIN_SIGN_CERT_FILE") ?: System.getProperty("winSignCertFile")
            val certPass = System.getenv("WIN_SIGN_CERT_PASSWORD") ?: System.getProperty("winSignCertPassword")
            
            if (certFile != null && file(certFile).exists()) {
                winOptions.add("--win-per-user-install")  // Per-user install when signed
                winOptions.add("--win-upgrade-uuid")
                winOptions.add("b8c9d5e6-7f8a-4b1c-9d2e-3f4a5b6c7d8e")  // Unique GUID for upgrades
                
                // Note: jpackage doesn't directly support signing, use post-processing with signtool
                // For actual signing, run after jpackage:
                // signtool sign /f certificate.pfx /p password /t http://timestamp.digicert.com installer.msi
                logger.lifecycle("Code signing certificate detected: $certFile")
                logger.lifecycle("Note: Run signtool manually after jpackage for actual signing")
            } else {
                logger.lifecycle("Windows installer will be unsigned (no certificate found)")
                logger.lifecycle("To sign: set WIN_SIGN_CERT_FILE and WIN_SIGN_CERT_PASSWORD environment variables")
            }
            
            installerOptions.addAll(winOptions)
        }
        
        // macOS-specific configuration
        if (os.isMacOsX) {
            val macOptions = mutableListOf(
                "--mac-package-name", "TollingVisionAnalysisSample",
                // Install to /Applications/TollingVision/
                "--install-dir", "/Applications/TollingVision"
            )
            
            // Code signing for macOS (if certificate is available)
            // To enable: set environment variables:
            //   - MAC_SIGN_IDENTITY: Developer ID Application certificate name
            //   Example: "Developer ID Application: Your Name (TEAMID)"
            val signIdentity = System.getenv("MAC_SIGN_IDENTITY") ?: System.getProperty("macSignIdentity")
            
            if (signIdentity != null && signIdentity.isNotEmpty()) {
                macOptions.add("--mac-sign")
                macOptions.add("--mac-signing-key-user-name")
                macOptions.add(signIdentity)
                
                // Optional: add entitlements if needed
                val entitlements = file("macos-entitlements.plist")
                if (entitlements.exists()) {
                    macOptions.add("--mac-entitlements")
                    macOptions.add(entitlements.absolutePath)
                }
                
                logger.lifecycle("macOS code signing enabled with identity: $signIdentity")
            } else {
                logger.lifecycle("macOS installer will be unsigned (no signing identity found)")
                logger.lifecycle("To sign: set MAC_SIGN_IDENTITY environment variable")
            }
            
            installerOptions.addAll(macOptions)
        }
        
        // Linux-specific configuration
        if (os.isLinux) {
            val linuxOptions = mutableListOf(
                "--linux-shortcut",
                "--linux-menu-group", "TollingVision",
                // Install to /opt/tollingvision/analysissample
                "--install-dir", "/opt/tollingvision/analysissample",
                // Package maintainer information (shown in package managers)
                "--linux-package-name", "tollingvision-analysis-sample",
                "--linux-deb-maintainer", "Smart Cloud Solutions <info@smartcloudsolutions.com>",
                "--linux-rpm-license-type", "Proprietary"
            )
            
            installerOptions.addAll(linuxOptions)
        }
    }
}

// Code formatting and import ordering
spotless {
    java {
        // Use Google Java Format for consistent formatting
        googleJavaFormat("1.22.0").reflowLongStrings()
        // Organize imports (remove unused, sort)
        importOrder()
        removeUnusedImports()
        target("src/**/*.java")
    }
    format("misc") {
        target("**/*.gradle", "**/*.md", "**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
