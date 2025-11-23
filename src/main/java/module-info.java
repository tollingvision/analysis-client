/**
 * TollingVision Analysis Sample Application Module
 *
 * <p>This module provides a JavaFX-based desktop application for batch processing vehicle images
 * through AI analysis using the TollingVision service.
 */
module analysis.sample {
  // JavaFX dependencies
  requires javafx.swing;
  requires transitive javafx.graphics;
  requires transitive javafx.controls;

  // gRPC and networking
  requires io.grpc;
  requires io.grpc.netty;
  requires io.netty.transport;
  requires io.netty.codec.http2;
  requires io.netty.handler;

  // TollingVision API (now with correct module name)
  requires transitive com.smartcloudsolutions.tollingvision;

  // Protocol Buffers
  requires com.google.protobuf;
  requires com.google.protobuf.util;

  // JSON processing (for protobuf-util serialization)
  requires com.google.gson;

  // Java desktop integration
  requires java.desktop;

  // Logging
  requires transitive java.logging;

  // Memory management
  requires java.management;

  // gRPC service providers (required for runtime service loading)
  uses io.grpc.NameResolverProvider;
  uses io.grpc.LoadBalancerProvider;

  // Exports for JavaFX application
  exports com.smartcloudsolutions.tollingvision.samples;
  exports com.smartcloudsolutions.tollingvision.samples.ui;
  exports com.smartcloudsolutions.tollingvision.samples.model;
  exports com.smartcloudsolutions.tollingvision.samples.util;
  exports com.smartcloudsolutions.tollingvision.samples.patternbuilder;
}
