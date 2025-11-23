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

    imageName.set("AnalysisSample")
    launcher {
        name = "AnalysisSample"
        jvmArgs = listOf(
            "-Djpackage.app-version=1.0.0"
        )
    }
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

    jpackage {
        imageName = "AnalysisSample"
        installerName = "AnalysisSample"
        appVersion = "1.0.0"
        skipInstaller = false
        
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
            
            // Add splash screen if it exists
            val splashFile = file("installer-splash.png")
            if (splashFile.exists()) {
                winOptions.add("--resource-dir")
                winOptions.add(projectDir.absolutePath)
            }
            
            installerOptions.addAll(winOptions)
        }
        
        // macOS-specific configuration
        if (os.isMacOsX) {
            val macOptions = mutableListOf(
                "--mac-package-name", "AnalysisSample",
                // Install to /Applications/TollingVision/
                "--install-dir", "/Applications/TollingVision"
            )
            
            installerOptions.addAll(macOptions)
        }
        
        // Linux-specific configuration
        if (os.isLinux) {
            val linuxOptions = mutableListOf(
                "--linux-shortcut",
                "--linux-menu-group", "TollingVision",
                // Install to /opt/tollingvision/analysissample
                "--install-dir", "/opt/tollingvision/analysissample"
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
