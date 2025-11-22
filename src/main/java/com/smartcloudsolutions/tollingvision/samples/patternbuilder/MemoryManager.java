package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Memory management utility for monitoring and managing memory usage during large file processing
 * operations.
 */
public class MemoryManager {

  private static final long LOW_MEMORY_THRESHOLD_MB = 50; // 50 MB
  private static final long CRITICAL_MEMORY_THRESHOLD_MB = 20; // 20 MB

  private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
  private static final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "MemoryMonitor");
            t.setDaemon(true);
            return t;
          });

  private static volatile boolean monitoringEnabled = false;
  private static volatile MemoryWarningListener warningListener;

  /** Interface for receiving memory warning notifications. */
  public interface MemoryWarningListener {
    /**
     * Called when available memory is low.
     *
     * @param availableMB available memory in MB
     * @param usedMB used memory in MB
     * @param totalMB total memory in MB
     */
    void onLowMemory(long availableMB, long usedMB, long totalMB);

    /**
     * Called when available memory is critically low.
     *
     * @param availableMB available memory in MB
     * @param usedMB used memory in MB
     * @param totalMB total memory in MB
     */
    void onCriticalMemory(long availableMB, long usedMB, long totalMB);
  }

  /**
   * Starts memory monitoring with the specified listener.
   *
   * @param listener the listener to receive memory warnings
   */
  public static void startMonitoring(MemoryWarningListener listener) {
    warningListener = listener;

    if (!monitoringEnabled) {
      monitoringEnabled = true;
      scheduler.scheduleAtFixedRate(MemoryManager::checkMemoryUsage, 0, 2, TimeUnit.SECONDS);
    }
  }

  /** Stops memory monitoring. */
  public static void stopMonitoring() {
    monitoringEnabled = false;
    warningListener = null;
  }

  /** Checks current memory usage and triggers warnings if necessary. */
  private static void checkMemoryUsage() {
    if (!monitoringEnabled || warningListener == null) {
      return;
    }

    try {
      MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

      long totalMB = heapUsage.getMax() / (1024 * 1024);
      long usedMB = heapUsage.getUsed() / (1024 * 1024);
      long availableMB = totalMB - usedMB;

      if (availableMB <= CRITICAL_MEMORY_THRESHOLD_MB) {
        warningListener.onCriticalMemory(availableMB, usedMB, totalMB);
      } else if (availableMB <= LOW_MEMORY_THRESHOLD_MB) {
        warningListener.onLowMemory(availableMB, usedMB, totalMB);
      }

    } catch (Exception e) {
      // Ignore monitoring errors to avoid disrupting the application
    }
  }

  /**
   * Gets current memory usage information.
   *
   * @return memory usage information
   */
  public static MemoryInfo getCurrentMemoryInfo() {
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

    long totalMB = heapUsage.getMax() / (1024 * 1024);
    long usedMB = heapUsage.getUsed() / (1024 * 1024);
    long availableMB = totalMB - usedMB;

    return new MemoryInfo(totalMB, usedMB, availableMB);
  }

  /**
   * Suggests garbage collection to free memory. This is a hint to the JVM and may not immediately
   * free memory.
   */
  public static void suggestGarbageCollection() {
    System.gc();
  }

  /**
   * Checks if memory usage is currently low.
   *
   * @return true if available memory is below the low threshold
   */
  public static boolean isMemoryLow() {
    MemoryInfo info = getCurrentMemoryInfo();
    return info.getAvailableMB() <= LOW_MEMORY_THRESHOLD_MB;
  }

  /**
   * Checks if memory usage is critically low.
   *
   * @return true if available memory is below the critical threshold
   */
  public static boolean isMemoryCritical() {
    MemoryInfo info = getCurrentMemoryInfo();
    return info.getAvailableMB() <= CRITICAL_MEMORY_THRESHOLD_MB;
  }

  /**
   * Gets a formatted string describing current memory usage.
   *
   * @return memory usage description
   */
  public static String getMemoryUsageString() {
    MemoryInfo info = getCurrentMemoryInfo();
    double usagePercent = (double) info.getUsedMB() / info.getTotalMB() * 100;

    return String.format(
        "Memory: %d/%d MB (%.1f%% used, %d MB available)",
        info.getUsedMB(), info.getTotalMB(), usagePercent, info.getAvailableMB());
  }

  /**
   * Calculates the recommended maximum number of items to process based on current memory
   * availability.
   *
   * @param estimatedMemoryPerItem estimated memory usage per item in bytes
   * @return recommended maximum items to process
   */
  public static int getRecommendedMaxItems(long estimatedMemoryPerItem) {
    MemoryInfo info = getCurrentMemoryInfo();

    // Use 70% of available memory to leave buffer
    long availableBytes = (long) (info.getAvailableMB() * 1024 * 1024 * 0.7);

    if (estimatedMemoryPerItem <= 0) {
      return Integer.MAX_VALUE;
    }

    long maxItems = availableBytes / estimatedMemoryPerItem;
    return (int) Math.min(maxItems, Integer.MAX_VALUE);
  }

  /** Shuts down the memory manager and stops monitoring. */
  public static void shutdown() {
    stopMonitoring();
    scheduler.shutdown();
  }

  /** Container for memory usage information. */
  public static class MemoryInfo {
    private final long totalMB;
    private final long usedMB;
    private final long availableMB;

    public MemoryInfo(long totalMB, long usedMB, long availableMB) {
      this.totalMB = totalMB;
      this.usedMB = usedMB;
      this.availableMB = availableMB;
    }

    public long getTotalMB() {
      return totalMB;
    }

    public long getUsedMB() {
      return usedMB;
    }

    public long getAvailableMB() {
      return availableMB;
    }

    public double getUsagePercent() {
      return (double) usedMB / totalMB * 100;
    }

    @Override
    public String toString() {
      return String.format("Memory: %d/%d MB (%.1f%% used)", usedMB, totalMB, getUsagePercent());
    }
  }
}
