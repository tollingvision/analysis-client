package com.smartcloudsolutions.tollingvision.samples.util;

import com.smartcloudsolutions.tollingvision.Quadrilateral;
import com.smartcloudsolutions.tollingvision.SearchResponse;
import com.smartcloudsolutions.tollingvision.Vehicle;
import java.nio.file.Path;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Utility class for drawing overlays and bounding boxes without OpenCV dependencies. Provides
 * lightweight overlay functionality for the gallery window.
 */
public class OverlayUtils {

  /**
   * Draws vehicle and plate bounding boxes on the overlay canvas using SearchResponse data. Uses
   * provided image dimensions for accurate coordinate scaling with letterbox offset calculation.
   *
   * @param gc Graphics context for drawing
   * @param imagePath Path to the current image
   * @param searchResponse SearchResponse data for this specific image
   * @param canvasWidth Width of the overlay canvas
   * @param canvasHeight Height of the overlay canvas
   * @param actualImageWidth Actual width of the loaded image
   * @param actualImageHeight Actual height of the loaded image
   */
  public static void drawBoundingBoxes(
      GraphicsContext gc,
      Path imagePath,
      SearchResponse searchResponse,
      double canvasWidth,
      double canvasHeight,
      double actualImageWidth,
      double actualImageHeight) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      gc.clearRect(0, 0, canvasWidth, canvasHeight);
      return;
    }

    // Clear previous overlays
    gc.clearRect(0, 0, canvasWidth, canvasHeight);

    // Calculate proper scaling and letterbox offsets for fitted image
    double imageAspectRatio = actualImageWidth / actualImageHeight;
    double canvasAspectRatio = canvasWidth / canvasHeight;

    double scaleX, scaleY, offsetX = 0, offsetY = 0;

    if (imageAspectRatio > canvasAspectRatio) {
      // Image is wider - fit to width, letterbox top/bottom
      scaleX = canvasWidth / actualImageWidth;
      scaleY = scaleX; // Maintain aspect ratio
      double scaledHeight = actualImageHeight * scaleY;
      offsetY = (canvasHeight - scaledHeight) / 2;
    } else {
      // Image is taller - fit to height, letterbox left/right
      scaleY = canvasHeight / actualImageHeight;
      scaleX = scaleY; // Maintain aspect ratio
      double scaledWidth = actualImageWidth * scaleX;
      offsetX = (canvasWidth - scaledWidth) / 2;
    }

    for (Vehicle vehicle : searchResponse.getResultList()) {
      // Draw vehicle bounding box
      if (vehicle.hasFrame()) {
        drawQuadrilateralWithOffset(
            gc, vehicle.getFrame(), Color.CYAN, "VEHICLE", scaleX, scaleY, offsetX, offsetY);
      }

      // Draw plate bounding box
      if (vehicle.hasPlate() && vehicle.getPlate().hasPosition()) {
        drawQuadrilateralWithOffset(
            gc,
            vehicle.getPlate().getPosition(),
            Color.LIME,
            "PLATE",
            scaleX,
            scaleY,
            offsetX,
            offsetY);
      }
    }
  }

  /**
   * Draws vehicle and plate bounding boxes on the overlay canvas using SearchResponse data. Uses
   * default scaling when image dimensions are not available.
   *
   * @param gc Graphics context for drawing
   * @param imagePath Path to the current image
   * @param searchResponse SearchResponse data for this specific image
   * @param canvasWidth Width of the overlay canvas
   * @param canvasHeight Height of the overlay canvas
   */
  public static void drawBoundingBoxes(
      GraphicsContext gc,
      Path imagePath,
      SearchResponse searchResponse,
      double canvasWidth,
      double canvasHeight) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      gc.clearRect(0, 0, canvasWidth, canvasHeight);
      return;
    }

    // Clear previous overlays
    gc.clearRect(0, 0, canvasWidth, canvasHeight);

    // Use default scaling factors
    double scaleX = canvasWidth / 1000.0;
    double scaleY = canvasHeight / 1000.0;

    for (Vehicle vehicle : searchResponse.getResultList()) {
      // Draw vehicle bounding box
      if (vehicle.hasFrame()) {
        drawQuadrilateral(gc, vehicle.getFrame(), Color.CYAN, "VEHICLE", scaleX, scaleY);
      }

      // Draw plate bounding box
      if (vehicle.hasPlate() && vehicle.getPlate().hasPosition()) {
        drawQuadrilateral(
            gc, vehicle.getPlate().getPosition(), Color.LIME, "PLATE", scaleX, scaleY);
      }
    }
  }

  /**
   * Draws a quadrilateral on the graphics context with proper scaling and letterbox offsets.
   *
   * @param gc Graphics context
   * @param quad Quadrilateral to draw
   * @param color Color for the quadrilateral
   * @param label Label to display
   * @param scaleX X-axis scaling factor
   * @param scaleY Y-axis scaling factor
   * @param offsetX X-axis letterbox offset
   * @param offsetY Y-axis letterbox offset
   */
  private static void drawQuadrilateralWithOffset(
      GraphicsContext gc,
      Quadrilateral quad,
      Color color,
      String label,
      double scaleX,
      double scaleY,
      double offsetX,
      double offsetY) {
    gc.setStroke(color);
    gc.setLineWidth(3);
    gc.setFill(color);

    // Convert quadrilateral points to canvas coordinates with scaling and letterbox
    // offsets
    double[] xPoints = {
      quad.getTopLeft().getX() * scaleX + offsetX,
      quad.getTopRight().getX() * scaleX + offsetX,
      quad.getBottomRight().getX() * scaleX + offsetX,
      quad.getBottomLeft().getX() * scaleX + offsetX
    };

    double[] yPoints = {
      quad.getTopLeft().getY() * scaleY + offsetY,
      quad.getTopRight().getY() * scaleY + offsetY,
      quad.getBottomRight().getY() * scaleY + offsetY,
      quad.getBottomLeft().getY() * scaleY + offsetY
    };

    // Draw the quadrilateral
    gc.strokePolygon(xPoints, yPoints, 4);

    // Draw label with background for better visibility
    gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
    String labelText = label;
    double textWidth = labelText.length() * 7; // Approximate text width
    double textHeight = 12;

    // Draw label background
    gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
    gc.fillRect(xPoints[0] + 2, yPoints[0] - textHeight - 2, textWidth + 4, textHeight + 4);

    // Draw label text
    gc.setFill(color);
    gc.fillText(labelText, xPoints[0] + 4, yPoints[0] - 4);
  }

  /**
   * Draws a quadrilateral on the graphics context with proper scaling.
   *
   * @param gc Graphics context
   * @param quad Quadrilateral to draw
   * @param color Color for the quadrilateral
   * @param label Label to display
   * @param scaleX X-axis scaling factor
   * @param scaleY Y-axis scaling factor
   */
  private static void drawQuadrilateral(
      GraphicsContext gc,
      Quadrilateral quad,
      Color color,
      String label,
      double scaleX,
      double scaleY) {
    gc.setStroke(color);
    gc.setLineWidth(3);
    gc.setFill(color);

    // Convert quadrilateral points to canvas coordinates using scaling
    double[] xPoints = {
      quad.getTopLeft().getX() * scaleX,
      quad.getTopRight().getX() * scaleX,
      quad.getBottomRight().getX() * scaleX,
      quad.getBottomLeft().getX() * scaleX
    };

    double[] yPoints = {
      quad.getTopLeft().getY() * scaleY,
      quad.getTopRight().getY() * scaleY,
      quad.getBottomRight().getY() * scaleY,
      quad.getBottomLeft().getY() * scaleY
    };

    // Draw the quadrilateral
    gc.strokePolygon(xPoints, yPoints, 4);

    // Draw label with background for better visibility
    gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
    String labelText = label;
    double textWidth = labelText.length() * 7; // Approximate text width
    double textHeight = 12;

    // Draw label background
    gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
    gc.fillRect(xPoints[0] + 2, yPoints[0] - textHeight - 2, textWidth + 4, textHeight + 4);

    // Draw label text
    gc.setFill(color);
    gc.fillText(labelText, xPoints[0] + 4, yPoints[0] - 4);
  }

  /**
   * Extracts plate text from SearchResponse data.
   *
   * @param searchResponse SearchResponse containing plate data
   * @return Plate text or empty string if no plate found
   */
  public static String extractPlateText(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return "";
    }

    for (Vehicle vehicle : searchResponse.getResultList()) {
      if (vehicle.hasPlate() && !vehicle.getPlate().getText().isEmpty()) {
        return vehicle.getPlate().getText();
      }
    }

    return "";
  }

  /**
   * Checks if SearchResponse contains plate data.
   *
   * @param searchResponse SearchResponse to check
   * @return true if plate data exists
   */
  public static boolean hasPlateData(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return false;
    }

    return searchResponse.getResultList().stream()
        .anyMatch(vehicle -> vehicle.hasPlate() && !vehicle.getPlate().getText().isEmpty());
  }

  /**
   * Checks if SearchResponse contains vehicle data.
   *
   * @param searchResponse SearchResponse to check
   * @return true if vehicle data exists
   */
  public static boolean hasVehicleData(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return false;
    }

    return searchResponse.getResultList().stream().anyMatch(Vehicle::hasFrame);
  }

  /**
   * Gets the primary plate from SearchResponse.
   *
   * @param searchResponse SearchResponse containing plate data
   * @return Primary plate or null if not found
   */
  public static com.smartcloudsolutions.tollingvision.Plate getPrimaryPlate(
      SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return null;
    }

    for (Vehicle vehicle : searchResponse.getResultList()) {
      if (vehicle.hasPlate() && !vehicle.getPlate().getText().isEmpty()) {
        return vehicle.getPlate();
      }
    }

    return null;
  }

  /**
   * Gets the primary vehicle from SearchResponse.
   *
   * @param searchResponse SearchResponse containing vehicle data
   * @return Primary vehicle or null if not found
   */
  public static Vehicle getPrimaryVehicle(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return null;
    }

    return searchResponse.getResultList().get(0); // Return first vehicle
  }

  /**
   * Extracts MMR (Make, Model, Recognition) data from SearchResponse.
   *
   * @param searchResponse SearchResponse containing MMR data
   * @return MMR information string or empty string if not found
   */
  public static String extractMmrInfo(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return "";
    }

    for (Vehicle vehicle : searchResponse.getResultList()) {
      if (vehicle.hasMmr()) {
        com.smartcloudsolutions.tollingvision.Mmr mmr = vehicle.getMmr();
        return String.format("%s %s (%s)", mmr.getMake(), mmr.getModel(), mmr.getCategory());
      }
    }

    return "";
  }

  /**
   * Checks if SearchResponse contains MMR data.
   *
   * @param searchResponse SearchResponse to check
   * @return true if MMR data exists
   */
  public static boolean hasMmrData(SearchResponse searchResponse) {
    if (searchResponse == null || searchResponse.getResultList().isEmpty()) {
      return false;
    }

    return searchResponse.getResultList().stream().anyMatch(Vehicle::hasMmr);
  }
}
