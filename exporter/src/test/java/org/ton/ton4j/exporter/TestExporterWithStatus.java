package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ton.ton4j.exporter.types.ExportStatus;

/** Test class for the enhanced Exporter with state persistence functionality */
public class TestExporterWithStatus {

  private Path tempDir;
  private StatusManager statusManager;

  @Before
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("exporter-test");
    statusManager = new StatusManager(tempDir.toString());
  }

  @After
  public void tearDown() {
    // Clean up any status files
    if (statusManager.statusExists()) {
      statusManager.deleteStatus();
    }
  }

  @Test
  public void testStatusManagerCreateAndLoad() {
    // Test creating a new status
    ExportStatus status = statusManager.createNewStatus(100, "file", "/tmp/test.txt", false, 4);
    assertThat(status).isNotNull();
    assertThat(status.getTotalPackages()).isEqualTo(100);
    assertThat(status.getExportType()).isEqualTo("file");
    assertThat(status.getOutputFile()).isEqualTo("/tmp/test.txt");
    assertThat(status.isDeserialized()).isFalse();
    assertThat(status.getParallelThreads()).isEqualTo(4);
    assertThat(status.isCompleted()).isFalse();

    // Test saving status
    statusManager.saveStatus(status);
    assertThat(statusManager.statusExists()).isTrue();

    // Test loading status
    ExportStatus loadedStatus = statusManager.loadStatus();
    assertThat(loadedStatus).isNotNull();
    assertThat(loadedStatus.getExportId()).isEqualTo(status.getExportId());
    assertThat(loadedStatus.getTotalPackages()).isEqualTo(status.getTotalPackages());
    assertThat(loadedStatus.getExportType()).isEqualTo(status.getExportType());
    assertThat(loadedStatus.getOutputFile()).isEqualTo(status.getOutputFile());
  }

  @Test
  public void testStatusPackageTracking() {
    ExportStatus status = statusManager.createNewStatus(10, "stdout", null, true, 2);

    // Initially no packages processed
    assertThat(status.getProcessedCount()).isEqualTo(0);
    assertThat(status.getProgressPercentage()).isCloseTo(0.0, within(0.01));
    assertThat(status.isPackageProcessed("package1")).isFalse();

    // Mark a package as processed
    status.markPackageProcessed("package1", 5, 2);
    assertThat(status.getProcessedCount()).isEqualTo(1);
    assertThat(status.getParsedBlocksCount()).isEqualTo(5);
    assertThat(status.getNonBlocksCount()).isEqualTo(2);
    assertThat(status.getProgressPercentage()).isCloseTo(10.0, within(0.01));
    assertThat(status.isPackageProcessed("package1")).isTrue();

    // Mark another package as processed
    status.markPackageProcessed("package2", 3, 1);
    assertThat(status.getProcessedCount()).isEqualTo(2);
    assertThat(status.getParsedBlocksCount()).isEqualTo(8);
    assertThat(status.getNonBlocksCount()).isEqualTo(3);
    assertThat(status.getProgressPercentage()).isCloseTo(20.0, within(0.01));
    assertThat(status.isPackageProcessed("package2")).isTrue();

    // Try to mark the same package again (should not change counts)
    status.markPackageProcessed("package1", 10, 5);
    assertThat(status.getProcessedCount()).isEqualTo(2);
    assertThat(status.getParsedBlocksCount()).isEqualTo(8);
    assertThat(status.getNonBlocksCount()).isEqualTo(3);
  }

  @Test
  public void testStatusCompletion() {
    ExportStatus status = statusManager.createNewStatus(5, "file", "/tmp/output.txt", false, 1);
    assertThat(status.isCompleted()).isFalse();

    status.markCompleted();
    assertThat(status.isCompleted()).isTrue();
    assertThat(status.getLastUpdate()).isNotNull();
  }

  @Test
  public void testStatusFilePersistence() {
    ExportStatus originalStatus =
        statusManager.createNewStatus(50, "file", "/tmp/blocks.txt", true, 8);
    originalStatus.markPackageProcessed("archive1", 10, 2);
    originalStatus.markPackageProcessed("archive2", 15, 3);

    // Save status
    statusManager.saveStatus(originalStatus);

    // Create new status manager instance to simulate restart
    StatusManager newStatusManager = new StatusManager(tempDir.toString());
    ExportStatus loadedStatus = newStatusManager.loadStatus();

    assertThat(loadedStatus).isNotNull();
    assertThat(loadedStatus.getExportId()).isEqualTo(originalStatus.getExportId());
    assertThat(loadedStatus.getTotalPackages()).isEqualTo(originalStatus.getTotalPackages());
    assertThat(loadedStatus.getProcessedCount()).isEqualTo(originalStatus.getProcessedCount());
    assertThat(loadedStatus.getParsedBlocksCount())
        .isEqualTo(originalStatus.getParsedBlocksCount());
    assertThat(loadedStatus.getNonBlocksCount()).isEqualTo(originalStatus.getNonBlocksCount());
    assertThat(loadedStatus.getExportType()).isEqualTo(originalStatus.getExportType());
    assertThat(loadedStatus.getOutputFile()).isEqualTo(originalStatus.getOutputFile());
    assertThat(loadedStatus.isDeserialized()).isEqualTo(originalStatus.isDeserialized());
    assertThat(loadedStatus.getParallelThreads()).isEqualTo(originalStatus.getParallelThreads());

    // Verify processed packages are preserved
    assertThat(loadedStatus.isPackageProcessed("archive1")).isTrue();
    assertThat(loadedStatus.isPackageProcessed("archive2")).isTrue();
    assertThat(loadedStatus.isPackageProcessed("archive3")).isFalse();

    // Clean up
    newStatusManager.deleteStatus();
  }

  @Test
  public void testStatusFileNotExists() {
    // Test loading when no status file exists
    ExportStatus status = statusManager.loadStatus();
    assertThat(status).isNull();
    assertThat(statusManager.statusExists()).isFalse();
  }

  @Test
  public void testStatusFileDeletion() {
    ExportStatus status = statusManager.createNewStatus(10, "stdout", null, false, 2);
    statusManager.saveStatus(status);
    assertThat(statusManager.statusExists()).isTrue();

    statusManager.deleteStatus();
    assertThat(statusManager.statusExists()).isFalse();

    ExportStatus loadedStatus = statusManager.loadStatus();
    assertThat(loadedStatus).isNull();
  }
}
