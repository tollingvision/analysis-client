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
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.swing")
}

jlink {
    forceMerge(".*")
    mergedModule { additive = true }

    imageName.set("analysis-sample-runtime")
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

    jpackage {
        installerName = "AnalysisSample"

        // platform-specific installer format
        val os = OperatingSystem.current()
        installerType = when {
            os.isMacOsX   -> "dmg"
            os.isWindows  -> "exe"
            else          -> "deb"   // Linux
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
